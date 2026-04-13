package com.flip7.flip7.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Configuration de sécurité : fournit le bean PasswordEncoder (BCrypt).
 * BCrypt intègre automatiquement le sel (salt) dans le hash et résiste aux
 * attaques par table arc-en-ciel et force brute.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        // Strength 12 : bon compromis sécurité / performance
        return new BCryptPasswordEncoder(12);
    }
}
