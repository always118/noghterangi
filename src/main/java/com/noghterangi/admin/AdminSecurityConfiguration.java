package com.noghterangi.admin;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration(proxyBeanMethods = false)
public class AdminSecurityConfiguration {
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(authorize -> authorize
                    .requestMatchers("/admin/login").permitAll()
                    .requestMatchers("/admin", "/admin/**").hasRole("ADMIN")
                    .anyRequest().permitAll())
                .formLogin(login -> login
                    .loginPage("/admin/login")
                    .loginProcessingUrl("/admin/login")
                    .defaultSuccessUrl("/admin", true)
                    .failureUrl("/admin/login?error")
                    .permitAll())
                .logout(logout -> logout
                    .logoutUrl("/admin/logout")
                    .logoutSuccessUrl("/admin/login?logout")
                    .invalidateHttpSession(true)
                    .clearAuthentication(true)
                    .deleteCookies("JSESSIONID"))
                .csrf(Customizer.withDefaults());
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    UserDetailsService adminUsers(@Value("${app.admin.username}") String username,
                                 @Value("${app.admin.password-hash}") String passwordHash,
                                 PasswordEncoder encoder) {
        if (username.isBlank() || !passwordHash.matches("\\$2[aby]\\$(0[4-9]|[12][0-9]|3[01])\\$[./A-Za-z0-9]{53}")
                || encoder.upgradeEncoding(passwordHash)) {
            // Do not include credential values in startup errors or validation reports.
            throw new IllegalStateException("Set ADMIN_USERNAME and a valid BCrypt ADMIN_PASSWORD_HASH (cost 10 or higher).");
        }
        return new InMemoryUserDetailsManager(User.withUsername(username)
                .password(passwordHash).roles("ADMIN").build());
    }
}
