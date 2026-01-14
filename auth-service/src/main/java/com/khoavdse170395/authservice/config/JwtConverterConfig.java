package com.khoavdse170395.authservice.config;

import com.khoavdse170395.authservice.security.JwtAuthConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JwtConverterConfig {

    @Bean
    public JwtAuthConverter jwtAuthConverter() {
        return new JwtAuthConverter();
    }

    // JwtDecoder sẽ được Spring Boot tự động tạo từ spring.security.oauth2.resourceserver.jwt.issuer-uri
    // Không cần tạo bean thủ công để tránh lỗi khi Keycloak chưa sẵn sàng
}
