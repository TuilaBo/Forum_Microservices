package com.khoavdse170395.authservice.controller;

import com.khoavdse170395.authservice.model.LoginRequest;
import com.khoavdse170395.authservice.model.LoginUrlResponse;
import com.khoavdse170395.authservice.model.RegisterRequest;
import com.khoavdse170395.authservice.model.TokenResponse;
import com.khoavdse170395.authservice.model.UserInfo;
import com.khoavdse170395.authservice.service.AuthService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/me")
    public UserInfo me(@AuthenticationPrincipal Jwt jwt) {
        return authService.getCurrentUser(jwt);
    }

    @PostMapping("/login")
    public TokenResponse login(@RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/register")
    public TokenResponse register(@RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @GetMapping("/login-url")
    public LoginUrlResponse getLoginUrl(@RequestParam(defaultValue = "http://localhost:8081/auth/callback") String redirectUri) {
        String loginUrl = authService.getLoginUrl(redirectUri);
        String registerUrl = authService.getRegisterUrl(redirectUri);
        return new LoginUrlResponse(
                loginUrl,
                registerUrl,
                "Sử dụng loginUrl để đăng nhập hoặc registerUrl để đăng ký. Sau đó redirect về redirectUri với code parameter."
        );
    }

    @GetMapping("/register-url")
    public LoginUrlResponse getRegisterUrl(@RequestParam(defaultValue = "http://localhost:8081/auth/callback") String redirectUri) {
        String registerUrl = authService.getRegisterUrl(redirectUri);
        String loginUrl = authService.getLoginUrl(redirectUri);
        return new LoginUrlResponse(
                loginUrl,
                registerUrl,
                "Sử dụng registerUrl để đăng ký tài khoản mới. Sau đó redirect về redirectUri với code parameter."
        );
    }

    @PostMapping("/token")
    public TokenResponse exchangeToken(
            @RequestParam String code,
            @RequestParam(defaultValue = "http://localhost:8081/auth/callback") String redirectUri
    ) {
        return authService.exchangeCodeForToken(code, redirectUri);
    }

    @GetMapping("/callback")
    public TokenResponse callback(
            @RequestParam String code,
            @RequestParam(required = false) String error,
            @RequestParam(defaultValue = "http://localhost:8081/auth/callback") String redirectUri
    ) {
        if (error != null) {
            throw new RuntimeException("Keycloak error: " + error);
        }
        return authService.exchangeCodeForToken(code, redirectUri);
    }
}


