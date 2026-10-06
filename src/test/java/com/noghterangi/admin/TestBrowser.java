package com.noghterangi.admin;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/** Small real-HTTP client that submits the same session cookies and CSRF fields as a browser. */
public class TestBrowser implements AutoCloseable {
    private static final Pattern CSRF = Pattern.compile("<input(?=[^>]*name=\"_csrf\")(?=[^>]*value=\"([^\"]+)\")[^>]*>");
    private final CookieManager cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
    private final HttpClient client = HttpClient.newBuilder().cookieHandler(cookies)
            .connectTimeout(Duration.ofSeconds(10)).build();
    private final String baseUrl;

    public TestBrowser(int port) { baseUrl = "http://localhost:" + port; }

    public HttpResponse<String> get(String path) throws Exception { return request(path, null); }

    public HttpResponse<String> getHtml(String path) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create(baseUrl + path)).header("Accept", "text/html")
                .timeout(Duration.ofSeconds(15)).GET().build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    public HttpResponse<String> post(String path, Map<String, String> fields) throws Exception {
        String body = fields.entrySet().stream()
                .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "="
                        + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
        return request(path, body);
    }

    public HttpResponse<String> postWithCsrf(String formPage, String path, Map<String, String> fields) throws Exception {
        var values = new HashMap<>(fields);
        values.put("_csrf", csrf(get(formPage).body()));
        return post(path, values);
    }

    public HttpResponse<String> upload(String path, String csrf, java.util.List<org.springframework.mock.web.MockMultipartFile> files) throws Exception {
        return upload(path, csrf, "files", files);
    }

    public HttpResponse<String> upload(String path, String csrf, String fieldName,
                                       java.util.List<org.springframework.mock.web.MockMultipartFile> files) throws Exception {
        String boundary = "image-test-" + java.util.UUID.randomUUID();
        var body = new java.io.ByteArrayOutputStream();
        for (var file : files) {
            body.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"" + fieldName + "\"; filename=\""
                    + file.getOriginalFilename() + "\"\r\nContent-Type: " + file.getContentType() + "\r\n\r\n")
                    .getBytes(StandardCharsets.UTF_8));
            body.write(file.getBytes()); body.write("\r\n".getBytes(StandardCharsets.UTF_8));
        }
        body.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        String target = path + (csrf == null ? "" : "?_csrf=" + URLEncoder.encode(csrf, StandardCharsets.UTF_8));
        var request = HttpRequest.newBuilder(URI.create(baseUrl + target)).timeout(Duration.ofSeconds(30))
                .header("Accept", "text/html").header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray())).build();
        return client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    public void login() throws Exception {
        var response = postWithCsrf("/admin/login", "/admin/login",
                Map.of("username", AdminTestConfiguration.USERNAME, "password", AdminTestConfiguration.PASSWORD));
        assertThat(response.statusCode()).isEqualTo(302);
        assertThat(response.headers().firstValue("location")).hasValueSatisfying(
                value -> assertThat(URI.create(value).getPath()).isEqualTo("/admin"));
    }

    public String sessionId() {
        return cookies.getCookieStore().getCookies().stream()
                .filter(cookie -> cookie.getName().equals("JSESSIONID")).findFirst().orElseThrow().getValue();
    }

    public static String csrf(String html) {
        var matcher = CSRF.matcher(html);
        if (!matcher.find()) { throw new AssertionError("Rendered form has no CSRF token"); }
        return matcher.group(1);
    }

    private HttpResponse<String> request(String path, String body) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create(baseUrl + path)).timeout(Duration.ofSeconds(15));
        if (body == null) { builder.GET(); }
        else { builder.header("Content-Type", "application/x-www-form-urlencoded").POST(HttpRequest.BodyPublishers.ofString(body)); }
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    @Override
    public void close() { client.close(); }
}
