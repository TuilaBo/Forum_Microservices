package com.khoavdse170395.authservice.service;

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

    TokenResponse login(LoginRequest request);

    TokenResponse register(RegisterRequest request);
}


