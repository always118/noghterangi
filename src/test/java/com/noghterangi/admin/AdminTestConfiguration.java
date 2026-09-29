package com.noghterangi.admin;

import java.util.UUID;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistrar;

@TestConfiguration(proxyBeanMethods = false)
public class AdminTestConfiguration {
    public static final String USERNAME = "integration-admin";
    public static final String PASSWORD = UUID.randomUUID().toString();
    private static final String PASSWORD_HASH = new BCryptPasswordEncoder().encode(PASSWORD);

    private static final String MEDIA_PATH = temporaryMediaDirectory();
    private static String temporaryMediaDirectory() {
        try { return java.nio.file.Files.createTempDirectory("noghterangi-test-media-").toString(); }
        catch (java.io.IOException exception) { throw new IllegalStateException(exception); }
    }

    @Bean
    DynamicPropertyRegistrar adminTestProperties() {
        return registry -> {
            registry.add("app.admin.username", () -> USERNAME);
            registry.add("app.media.storage-path", () -> MEDIA_PATH);
            registry.add("app.admin.password-hash", () -> PASSWORD_HASH);
            registry.add("server.servlet.session.cookie.secure", () -> false);
        };
    }
}
