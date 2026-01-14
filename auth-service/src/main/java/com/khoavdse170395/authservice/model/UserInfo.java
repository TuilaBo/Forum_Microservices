package com.khoavdse170395.authservice.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Map;

@Schema(description = "Thông tin user từ JWT token")
@JsonIgnoreProperties(ignoreUnknown = true)
public record UserInfo(
        @Schema(description = "User ID", example = "c4144f5a-0226-4fd4-a596-e9d0da3959b7")
        String id,
        
        @Schema(description = "Username", example = "student1")
        String username,
        
        @Schema(description = "Email", example = "student1@school.edu")
        String email,
        
        @Schema(
            description = "Realm roles từ Keycloak", 
            type = "object",
            implementation = Object.class,
            example = "{\"roles\": [\"STUDENT\"]}"
        )
        Map<String, Object> realmRoles,
        
        @Schema(
            description = "Resource access từ Keycloak", 
            type = "object",
            implementation = Object.class
        )
        Map<String, Object> resourceAccess,
        
        @Schema(description = "Thời điểm token được phát hành", example = "2026-01-12T10:00:00Z")
        Instant issuedAt,
        
        @Schema(description = "Thời điểm token hết hạn", example = "2026-01-12T10:05:00Z")
        Instant expiresAt
) {
}


