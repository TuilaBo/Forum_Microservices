package com.khoavdse170395.authservice.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class OtpDtos {

    public record RegisterSendOtpRequest(
            @NotBlank @Email String email,
            @NotBlank @Size(min = 3, max = 50) String username,
            @NotBlank @Size(min = 6) String password,
            String firstName,
            String lastName
    ) {}

    public record RegisterConfirmOtpRequest(
            @NotBlank @Email String email,
            @NotBlank String otp,
            @NotBlank @Size(min = 3, max = 50) String username,
            @NotBlank @Size(min = 6) String password,
            String firstName,
            String lastName
    ) {}

    public record ForgotPasswordRequest(
            @NotBlank @Email String email
    ) {}

    public record ResetPasswordWithOtpRequest(
            @NotBlank @Email String email,
            @NotBlank String otp,
            @NotBlank @Size(min = 6) String newPassword
    ) {}
}

