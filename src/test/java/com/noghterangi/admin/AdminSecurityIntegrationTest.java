package com.noghterangi.admin;

import com.noghterangi.category.CategoryRepository;
import java.net.URI;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(AdminTestConfiguration.class)
class AdminSecurityIntegrationTest {
    @LocalServerPort int port;
    @Autowired CategoryRepository categories;
    private TestBrowser browser;
    private final String slug = "security-test-" + UUID.randomUUID();

    @BeforeEach
    void openBrowser() { browser = new TestBrowser(port); }

    @AfterEach
    void cleanUp() {
        browser.close();
        categories.deleteAll(categories.findAll().stream().filter(c -> c.getSlug().equals(slug)).toList());
    }

    @Test
    void publicPagesAssetsAndPersianLoginRemainAnonymous() throws Exception {
        for (String path : new String[]{"/", "/health", "/css/main.css", "/vendor/bootstrap/bootstrap.rtl.min.css", "/vendor/bootstrap/bootstrap.min.js"}) {
            assertThat(browser.get(path).statusCode()).as(path).isEqualTo(200);
        }
        var login = browser.get("/admin/login");
        assertThat(login.statusCode()).isEqualTo(200);
        assertThat(login.body()).contains("lang=\"fa\"", "dir=\"rtl\"", "name=\"username\"", "type=\"password\"", "name=\"_csrf\"");
        assertThat(login.headers().allValues("set-cookie")).anySatisfy(cookie ->
                assertThat(cookie).contains("HttpOnly", "SameSite=Lax"));
    }

    @Test
    void allAdminPagesIncludingFutureRoutesRequireAuthentication() throws Exception {
        for (String path : new String[]{"/admin", "/admin/", "/admin/categories", "/admin/categories/new", "/admin/categories/1/edit", "/admin/future-feature"}) {
            var response = browser.get(path);
            assertThat(response.statusCode()).as(path).isEqualTo(302);
            assertThat(response.headers().firstValue("location")).hasValueSatisfying(location ->
                    assertThat(URI.create(location).getPath()).isEqualTo("/admin/login"));
        }
        // A valid anonymous CSRF token still cannot bypass authentication.
        var response = browser.postWithCsrf("/admin/login", "/admin/categories", Map.of("name", "نباید ایجاد شود", "slug", slug));
        assertThat(response.statusCode()).isEqualTo(302);
        assertThat(categories.existsBySlug(slug)).isFalse();
    }

    @Test
    void validLoginRotatesSessionAndAlwaysLandsOnAdmin() throws Exception {
        browser.get("/admin/categories");
        browser.get("/admin/login");
        String anonymousSession = browser.sessionId();
        browser.login();
        assertThat(browser.sessionId()).isNotEqualTo(anonymousSession);
        assertThat(browser.get("/admin").statusCode()).isEqualTo(200);
        assertThat(browser.get("/admin").body()).contains("مدیریت نقطه رنگی", "action=\"/admin/logout\"");
        assertThat(browser.get("/admin/categories").statusCode()).isEqualTo(200);
    }

    @Test
    void invalidCredentialsFailWithGenericPersianMessage() throws Exception {
        for (String username : new String[]{AdminTestConfiguration.USERNAME, "nonexistent-admin"}) {
            var response = browser.postWithCsrf("/admin/login", "/admin/login", Map.of("username", username, "password", "invalid"));
            assertThat(response.statusCode()).isEqualTo(302);
            assertThat(response.headers().firstValue("location")).hasValueSatisfying(value ->
                    assertThat(value).endsWith("/admin/login?error"));
            assertThat(browser.get("/admin/login?error").body()).contains("نام کاربری یا رمز عبور نادرست است.").doesNotContain("invalid", "nonexistent-admin");
            assertThat(browser.get("/admin").statusCode()).isEqualTo(302);
        }
    }

    @Test
    void loginAndCategoryMutationsRejectMissingOrInvalidCsrf() throws Exception {
        var credentials = Map.of("username", AdminTestConfiguration.USERNAME, "password", AdminTestConfiguration.PASSWORD);
        assertThat(browser.post("/admin/login", credentials).statusCode()).isEqualTo(403);
        browser.login();
        var fields = Map.of("name", "آزمون امنیت", "slug", slug, "active", "true");
        assertThat(browser.post("/admin/categories", fields).statusCode()).isEqualTo(403);
        assertThat(categories.existsBySlug(slug)).isFalse();
        assertThat(browser.postWithCsrf("/admin/categories/new", "/admin/categories", fields).statusCode()).isEqualTo(302);
        var category = categories.findAll().stream().filter(c -> c.getSlug().equals(slug)).findFirst().orElseThrow();
        String path = "/admin/categories/" + category.getId();
        assertThat(browser.post(path, Map.of("name", "نباید تغییر کند", "slug", slug)).statusCode()).isEqualTo(403);
        assertThat(browser.post(path + "/active", Map.of("active", "false")).statusCode()).isEqualTo(403);
        assertThat(browser.post(path + "/active", Map.of("active", "false", "_csrf", "invalid-token")).statusCode()).isEqualTo(403);
        var unchanged = categories.findById(category.getId()).orElseThrow();
        assertThat(unchanged.getName()).isEqualTo("آزمون امنیت");
        assertThat(unchanged.isActive()).isTrue();
    }

    @Test
    void logoutRequiresCsrfAndRemovesAdminAccess() throws Exception {
        browser.login();
        browser.get("/admin/logout");
        assertThat(browser.get("/admin").statusCode()).isEqualTo(200);
        assertThat(browser.post("/admin/logout", Map.of()).statusCode()).isEqualTo(403);
        assertThat(browser.get("/admin").statusCode()).isEqualTo(200);
        var response = browser.postWithCsrf("/admin", "/admin/logout", Map.of());
        assertThat(response.statusCode()).isEqualTo(302);
        assertThat(response.headers().firstValue("location")).hasValueSatisfying(value ->
                assertThat(value).endsWith("/admin/login?logout"));
        assertThat(browser.get("/admin/login?logout").body()).contains("با موفقیت خارج شدید.");
        assertThat(browser.get("/admin").statusCode()).isEqualTo(302);
        assertThat(browser.get("/admin/categories").statusCode()).isEqualTo(302);
    }
}
