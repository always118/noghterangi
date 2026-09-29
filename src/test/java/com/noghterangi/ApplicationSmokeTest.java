package com.noghterangi;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import javax.sql.DataSource;

import jakarta.persistence.EntityManagerFactory;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import com.noghterangi.admin.AdminTestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(AdminTestConfiguration.class)
class ApplicationSmokeTest {

    @LocalServerPort
    private int port;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private Flyway flyway;

    @Test
    void connectsToPostgreSqlAndQueriesThroughJpa() throws Exception {
        try (var connection = dataSource.getConnection()) {
            assertThat(connection.getMetaData().getDatabaseProductName()).isEqualTo("PostgreSQL");
            assertThat(connection.isValid(5)).isTrue();
        }
        try (var entityManager = entityManagerFactory.createEntityManager()) {
            Object schemaName = entityManager.createNativeQuery(
                    "select schema_name from information_schema.schemata where schema_name = 'noghterangi'")
                    .getSingleResult();
            assertThat(schemaName).isEqualTo("noghterangi");
        }
    }

    @Test
    void flywayAppliedTheFoundationMigrationAndDoesNotRepeatIt() {
        flyway.validate();
        assertThat(flyway.info().applied())
                .extracting(migration -> migration.getScript())
                .contains("V1__create_application_schema.sql", "V2__create_category.sql", "V3__add_category_parent.sql", "V4__create_product.sql");
        assertThat(flyway.info().pending()).isEmpty();
        assertThat(flyway.migrate().migrationsExecuted).isZero();
    }

    @Test
    void healthReturnsOkOverHttp() throws Exception {
        HttpResponse<String> response = get("/health");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("content-type")).hasValueSatisfying(
                value -> assertThat(value).startsWith("text/plain"));
        assertThat(response.body()).isEqualTo("OK");
    }

    @Test
    void homepageRendersPersianThymeleafTemplate() throws Exception {
        HttpResponse<String> response = get("/");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("content-type")).hasValueSatisfying(
                value -> assertThat(value).contains("text/html", "charset=UTF-8"));
        assertThat(response.body())
                .contains("lang=\"fa\"", "dir=\"rtl\"", "id=\"hero-title\"", "نقطه رنگی", "href=\"/\"")
                .contains("/vendor/bootstrap/bootstrap.rtl.min.css", "/css/main.css",
                        "id=\"main-navigation\"", "id=\"main-content\"", "<footer")
                .doesNotContain("th:href", "th:replace", "th:fragment");

        for (String asset : new String[]{"/css/main.css",
                "/vendor/bootstrap/bootstrap.rtl.min.css", "/vendor/bootstrap/bootstrap.min.js"}) {
            assertThat(get(asset).statusCode()).as(asset).isEqualTo(200);
        }
    }

    private HttpResponse<String> get(String path) throws Exception {
        try (HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build()) {
            HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();
            return client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        }
    }
}
