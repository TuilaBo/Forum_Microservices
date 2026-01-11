package com.khoavdse170395.authservice.model;

public record RegisterRequest(
        String username,
        String email,
        String password,
        String firstName,
        String lastName
) {
}
