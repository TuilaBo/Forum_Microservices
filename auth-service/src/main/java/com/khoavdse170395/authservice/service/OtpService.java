package com.khoavdse170395.authservice.service;

public interface OtpService {

    String generateRegisterOtp(String email);

    boolean verifyRegisterOtp(String email, String otp);

    String generateForgotPasswordOtp(String email);

    boolean verifyForgotPasswordOtp(String email, String otp);
}

