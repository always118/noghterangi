package com.noghterangi.admin;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AdminSecurityConfigurationTest {
    @Test
    void missingPlaintextOrWeakCredentialsFailClosedWithoutEchoingValues() {
        var config = new AdminSecurityConfiguration();
        var encoder = new BCryptPasswordEncoder();
        for (String hash : new String[]{"", "not-an-encoded-password", new BCryptPasswordEncoder(4).encode("test-only")}) {
            assertThatThrownBy(() -> config.adminUsers("admin", hash, encoder))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Set ADMIN_USERNAME and a valid BCrypt ADMIN_PASSWORD_HASH (cost 10 or higher).");
        }
        assertThatThrownBy(() -> config.adminUsers(" ", encoder.encode("test-only"), encoder))
                .isInstanceOf(IllegalStateException.class);
    }
}
