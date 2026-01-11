package com.khoavdse170395.authservice.model;

public record TokenResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        Long expiresIn,
        String scope
) {
}

