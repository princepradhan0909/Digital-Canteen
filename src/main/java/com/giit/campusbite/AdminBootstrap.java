package com.giit.campusbite;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AdminBootstrap {

    @Bean
    CommandLineRunner provisionAdmin(
            StudentUserRepository users,
            PasswordEncoder passwordEncoder,
            @Value("${campusbite.admin-email:}") String email,
            @Value("${campusbite.admin-password:}") String password,
            @Value("${campusbite.admin-name:Canteen Admin}") String name,
            @Value("${campusbite.admin-phone:}") String phone) {
        return args -> {
            if (email.isBlank() && password.isBlank()) return;
            if (email.isBlank() || password.length() < 12
                    || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72
                    || phone.isBlank()) {
                throw new IllegalStateException(
                        "Configure ADMIN_EMAIL, ADMIN_PASSWORD (12-72 UTF-8 bytes), "
                                + "and ADMIN_PHONE together");
            }
            String normalizedEmail = email.trim().toLowerCase();
            users.findByEmailIgnoreCase(normalizedEmail).ifPresentOrElse(
                    user -> {
                        if (!"ADMIN".equals(user.getRole())) {
                            throw new IllegalStateException(
                                    "Configured admin email already belongs to a student account. "
                                            + "Choose a dedicated admin email.");
                        }
                    },
                    () -> users.save(new StudentUser(
                            name, normalizedEmail, phone.trim(),
                            passwordEncoder.encode(password), "ADMIN")));
        };
    }
}
