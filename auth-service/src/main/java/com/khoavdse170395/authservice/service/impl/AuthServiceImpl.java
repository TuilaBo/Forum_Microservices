package com.khoavdse170395.authservice.service.impl;

import com.khoavdse170395.authservice.model.AuthResponse;
import com.khoavdse170395.authservice.model.LoginRequest;
import com.khoavdse170395.authservice.model.RegisterRequest;
import com.khoavdse170395.authservice.model.TokenResponse;
import com.khoavdse170395.authservice.model.UserInfo;
import com.khoavdse170395.authservice.repository.UserRepository;
import com.khoavdse170395.authservice.service.AuthService;
import com.khoavdse170395.authservice.service.KeycloakAdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final KeycloakAdminService keycloakAdminService;
    private final JwtDecoder jwtDecoder;
    private final String keycloakUrl;
    private final String realm;
    private final String clientId;

    @Autowired
    public AuthServiceImpl(
            UserRepository userRepository,
            KeycloakAdminService keycloakAdminService,
            JwtDecoder jwtDecoder,
            @Value("${keycloak.url:http://localhost:8080}") String keycloakUrl,
            @Value("${keycloak.realm:school-forum}") String realm,
            @Value("${keycloak.client-id:forum-frontend}") String clientId
    ) {
        this.userRepository = userRepository;
        this.keycloakAdminService = keycloakAdminService;
        this.jwtDecoder = jwtDecoder;
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
    public AuthResponse login(LoginRequest request) {
        String tokenUrl = keycloakUrl + "/realms/" + realm + "/protocol/openid-connect/token";

        String requestBody = "grant_type=password" +
                "&client_id=" + clientId +
                "&username=" + URLEncoder.encode(request.username(), StandardCharsets.UTF_8) +
                "&password=" + URLEncoder.encode(request.password(), StandardCharsets.UTF_8);

        TokenResponse tokenResponse = callKeycloakTokenEndpoint(tokenUrl, requestBody);
        
        // Decode JWT để lấy user info
        Jwt jwt = jwtDecoder.decode(tokenResponse.accessToken());
        Map<String, Object> userInfo = keycloakAdminService.getUserInfoFromToken(jwt);
        
        // Build AuthResponse cho Next.js
        return buildAuthResponse(tokenResponse, userInfo);
    }

    @Override
    public AuthResponse register(RegisterRequest request) {
        // 1. Tạo user trong Keycloak
        String userId = keycloakAdminService.createUser(request);
        
        // 2. Gán role ROLE_STUDENT mặc định
        keycloakAdminService.assignRoleToUser(userId, "ROLE_STUDENT");
        
        // 3. Login để lấy token
        LoginRequest loginRequest = new LoginRequest(request.username(), request.password());
        return login(loginRequest);
    }

    @Override
    public AuthResponse refreshToken(String refreshToken) {
        String tokenUrl = keycloakUrl + "/realms/" + realm + "/protocol/openid-connect/token";

        String requestBody = "grant_type=refresh_token" +
                "&client_id=" + clientId +
                "&refresh_token=" + URLEncoder.encode(refreshToken, StandardCharsets.UTF_8);

        TokenResponse tokenResponse = callKeycloakTokenEndpoint(tokenUrl, requestBody);
        
        // Decode JWT để lấy user info
        Jwt jwt = jwtDecoder.decode(tokenResponse.accessToken());
        Map<String, Object> userInfo = keycloakAdminService.getUserInfoFromToken(jwt);
        
        // Build AuthResponse cho Next.js
        return buildAuthResponse(tokenResponse, userInfo);
    }

    /**
     * Build AuthResponse từ TokenResponse và user info.
     */
    private AuthResponse buildAuthResponse(TokenResponse tokenResponse, Map<String, Object> userInfo) {
        AuthResponse.UserData userData = new AuthResponse.UserData();
        userData.setId((String) userInfo.get("id"));
        userData.setUsername((String) userInfo.get("username"));
        userData.setEmail((String) userInfo.get("email"));
        // getUserInfoFromToken trả về "firstName" và "lastName" (đã map từ given_name/family_name)
        userData.setFirstName((String) userInfo.get("firstName"));
        userData.setLastName((String) userInfo.get("lastName"));
        
        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) userInfo.get("roles");
        userData.setRoles(roles != null ? roles : List.of());

        return new AuthResponse(
                tokenResponse.accessToken(),
                tokenResponse.refreshToken(),
                tokenResponse.tokenType(),
                tokenResponse.expiresIn(),
                userData
        );
    }

    private TokenResponse callKeycloakTokenEndpoint(String tokenUrl, String requestBody) {
        RestClient restClient = RestClient.create();
        try {
            var response = restClient.post()
                    .uri(tokenUrl)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .body(requestBody)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), (req, res) -> {
                        // Read error response body
                        String errorBody = new String(res.getBody().readAllBytes(), StandardCharsets.UTF_8);
                        throw new RuntimeException("Keycloak error: " + res.getStatusCode() + " - " + errorBody);
                    })
                    .body(TokenResponseDto.class);

            if (response == null || response.access_token == null) {
                throw new RuntimeException("Failed to get token from Keycloak: Empty response");
            }

            return new TokenResponse(
                    response.access_token,
                    response.refresh_token,
                    response.token_type,
                    response.expires_in,
                    response.scope
            );
        } catch (RuntimeException e) {
            // Re-throw với message rõ ràng hơn
            if (e.getMessage() != null && e.getMessage().contains("Keycloak error")) {
                throw e;
            }
            throw new RuntimeException("Failed to get token from Keycloak: " + e.getMessage(), e);
        }
    }

    private static class TokenResponseDto {
        public String access_token;
        public String refresh_token;
        public String token_type;
        public Long expires_in;
        public String scope;
    }
}


