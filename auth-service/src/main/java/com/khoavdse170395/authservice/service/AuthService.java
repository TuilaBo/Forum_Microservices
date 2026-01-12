package com.khoavdse170395.authservice.service;

import com.khoavdse170395.authservice.model.AuthResponse;
import com.khoavdse170395.authservice.model.LoginRequest;
import com.khoavdse170395.authservice.model.RegisterRequest;
import com.khoavdse170395.authservice.model.TokenResponse;
import com.khoavdse170395.authservice.model.UserInfo;
import org.springframework.security.oauth2.jwt.Jwt;

public interface AuthService {

    UserInfo getCurrentUser(Jwt jwt);

    String getLoginUrl(String redirectUri);

    String getRegisterUrl(String redirectUri);

    TokenResponse exchangeCodeForToken(String code, String redirectUri);

    /**
     * Login và trả về AuthResponse cho Next.js.
     */
    AuthResponse login(LoginRequest request);

    /**
     * Register user mới với role ROLE_STUDENT mặc định và trả về AuthResponse cho Next.js.
     */
    AuthResponse register(RegisterRequest request);

    /**
     * Refresh access token bằng refresh token.
     */
    AuthResponse refreshToken(String refreshToken);
}


