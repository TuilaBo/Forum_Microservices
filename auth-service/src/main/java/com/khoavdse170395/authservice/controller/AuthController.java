package com.khoavdse170395.authservice.controller;

import com.khoavdse170395.authservice.model.AuthResponse;
import com.khoavdse170395.authservice.model.LoginRequest;
import com.khoavdse170395.authservice.model.LoginUrlResponse;
import com.khoavdse170395.authservice.model.RefreshTokenRequest;
import com.khoavdse170395.authservice.model.RegisterRequest;
import com.khoavdse170395.authservice.model.TokenResponse;
import com.khoavdse170395.authservice.model.UserInfo;
import com.khoavdse170395.authservice.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "APIs để đăng nhập và đăng ký")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user", description = "Lấy thông tin user hiện tại từ JWT token")
    public UserInfo me(@AuthenticationPrincipal Jwt jwt) {
        return authService.getCurrentUser(jwt);
    }

    @PostMapping("/login")
    @Operation(summary = "Login", description = "Đăng nhập và nhận access token + user info")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    @Operation(summary = "Register", description = "Đăng ký tài khoản mới với role ROLE_STUDENT mặc định")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.ok(response);
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

    @PostMapping("/refresh")
    @Operation(summary = "Refresh token", description = "Làm mới access token bằng refresh token")
    public ResponseEntity<AuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refreshToken(request.refreshToken());
        return ResponseEntity.ok(response);
    }
}


