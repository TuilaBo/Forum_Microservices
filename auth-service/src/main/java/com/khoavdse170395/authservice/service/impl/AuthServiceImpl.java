package com.khoavdse170395.authservice.service.impl;

import com.khoavdse170395.authservice.model.LoginRequest;
import com.khoavdse170395.authservice.model.RegisterRequest;
import com.khoavdse170395.authservice.model.TokenResponse;
import com.khoavdse170395.authservice.model.UserInfo;
import com.khoavdse170395.authservice.repository.UserRepository;
import com.khoavdse170395.authservice.service.AuthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final String keycloakUrl;
    private final String realm;
    private final String clientId;

    public AuthServiceImpl(
            UserRepository userRepository,
            @Value("${keycloak.url:http://localhost:8080}") String keycloakUrl,
            @Value("${keycloak.realm:school-forum}") String realm,
            @Value("${keycloak.client-id:forum-frontend}") String clientId
    ) {
        this.userRepository = userRepository;
        this.keycloakUrl = keycloakUrl;
        this.realm = realm;
        this.clientId = clientId;
    }

    @Override
    public UserInfo getCurrentUser(Jwt jwt) {
        UserInfo info = new UserInfo(
                jwt.getSubject(),
                jwt.getClaimAsString("preferred_username"),
                jwt.getClaimAsString("email"),
                jwt.getClaim("realm_access"),
                jwt.getClaim("resource_access"),
                jwt.getIssuedAt(),
                jwt.getExpiresAt()
        );

        userRepository.save(info);
        return info;
    }

    @Override
    public String getLoginUrl(String redirectUri) {
        String baseUrl = keycloakUrl + "/realms/" + realm + "/protocol/openid-connect/auth";
        return baseUrl + "?client_id=" + clientId +
                "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8) +
                "&response_type=code" +
                "&scope=openid profile email";
    }

    @Override
    public String getRegisterUrl(String redirectUri) {
        String baseUrl = keycloakUrl + "/realms/" + realm + "/protocol/openid-connect/registrations";
        return baseUrl + "?client_id=" + clientId +
                "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8) +
                "&response_type=code" +
                "&scope=openid profile email";
    }

    @Override
    public TokenResponse exchangeCodeForToken(String code, String redirectUri) {
        String tokenUrl = keycloakUrl + "/realms/" + realm + "/protocol/openid-connect/token";

        String requestBody = "grant_type=authorization_code" +
                "&code=" + URLEncoder.encode(code, StandardCharsets.UTF_8) +
                "&client_id=" + clientId +
                "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8);

        return callKeycloakTokenEndpoint(tokenUrl, requestBody);
    }

    @Override
    public TokenResponse login(LoginRequest request) {
        String tokenUrl = keycloakUrl + "/realms/" + realm + "/protocol/openid-connect/token";

        String requestBody = "grant_type=password" +
                "&client_id=" + clientId +
                "&username=" + URLEncoder.encode(request.username(), StandardCharsets.UTF_8) +
                "&password=" + URLEncoder.encode(request.password(), StandardCharsets.UTF_8);

        return callKeycloakTokenEndpoint(tokenUrl, requestBody);
    }

    @Override
    public TokenResponse register(RegisterRequest request) {
        // Đăng ký user trong Keycloak (cần admin client hoặc bật user registration)
        // Tạm thời chỉ trả về login URL, user phải đăng ký qua Keycloak UI
        // Hoặc bạn có thể implement gọi Keycloak Admin API để tạo user
        throw new UnsupportedOperationException("Direct registration not implemented. Use /auth/register-url to get registration URL.");
    }

    private TokenResponse callKeycloakTokenEndpoint(String tokenUrl, String requestBody) {
        RestClient restClient = RestClient.create();
        var response = restClient.post()
                .uri(tokenUrl)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .body(requestBody)
                .retrieve()
                .body(TokenResponseDto.class);

        if (response == null) {
            throw new RuntimeException("Failed to get token from Keycloak");
        }

        return new TokenResponse(
                response.access_token,
                response.refresh_token,
                response.token_type,
                response.expires_in,
                response.scope
        );
    }

    private static class TokenResponseDto {
        public String access_token;
        public String refresh_token;
        public String token_type;
        public Long expires_in;
        public String scope;
    }
}


