package com.khoavdse170395.authservice.model;

import java.time.Instant;
import java.util.Map;

public record UserInfo(
        String id,
        String username,
        String email,
        Map<String, Object> realmRoles,
        Map<String, Object> resourceAccess,
        Instant issuedAt,
        Instant expiresAt
) {
}


