package com.khoavdse170395.authservice.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.khoavdse170395.authservice.model.RegisterRequest;
import com.khoavdse170395.authservice.service.KeycloakAdminService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class KeycloakAdminServiceImpl implements KeycloakAdminService {

    private static final Logger logger = LoggerFactory.getLogger(KeycloakAdminServiceImpl.class);

    @Value("${keycloak.url:http://localhost:8080}")
    private String keycloakUrl;

    @Value("${keycloak.realm:school-forum}")
    private String realm;

    @Value("${keycloak.admin.username:admin}")
    private String adminUsername;

    @Value("${keycloak.admin.password:admin}")
    private String adminPassword;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestClient restClient = RestClient.create();

    @Override
    public String createUser(RegisterRequest request) {
        String adminToken = getAdminAccessToken();
        String createUserUrl = keycloakUrl + "/admin/realms/" + realm + "/users";

        try {
            // Tạo user object
            Map<String, Object> userRepresentation = new HashMap<>();
            userRepresentation.put("username", request.username());
            userRepresentation.put("email", request.email());
            userRepresentation.put("firstName", request.firstName());
            userRepresentation.put("lastName", request.lastName());
            userRepresentation.put("enabled", true);
            userRepresentation.put("emailVerified", true);
            userRepresentation.put("credentials", List.of(
                    Map.of(
                            "type", "password",
                            "value", request.password(),
                            "temporary", false
                    )
            ));

            String userJson = objectMapper.writeValueAsString(userRepresentation);

            // Tạo user
            var response = restClient.post()
                    .uri(createUserUrl)
                    .header("Authorization", "Bearer " + adminToken)
                    .header("Content-Type", "application/json")
                    .body(userJson)
                    .retrieve()
                    .toBodilessEntity();

            // Lấy user ID từ Location header
            String location = response.getHeaders().getFirst("Location");
            if (location == null) {
                throw new RuntimeException("Failed to create user: No Location header");
            }

            // Extract user ID from location: /admin/realms/school-forum/users/{userId}
            String userId = location.substring(location.lastIndexOf('/') + 1);
            logger.info("User created successfully with ID: {}", userId);

            return userId;
        } catch (Exception e) {
            logger.error("Error creating user in Keycloak", e);
            throw new RuntimeException("Failed to create user: " + e.getMessage());
        }
    }

    @Override
    public void assignRoleToUser(String userId, String roleName) {
        String adminToken = getAdminAccessToken();

        try {
            // Lấy role ID
            String getRoleUrl = keycloakUrl + "/admin/realms/" + realm + "/roles/" + roleName;
            var roleResponse = restClient.get()
                    .uri(getRoleUrl)
                    .header("Authorization", "Bearer " + adminToken)
                    .retrieve()
                    .body(Map.class);

            if (roleResponse == null) {
                throw new RuntimeException("Role not found: " + roleName);
            }

            // Gán role cho user
            String assignRoleUrl = keycloakUrl + "/admin/realms/" + realm + "/users/" + userId + "/role-mappings/realm";
            String roleJson = objectMapper.writeValueAsString(List.of(roleResponse));

            restClient.post()
                    .uri(assignRoleUrl)
                    .header("Authorization", "Bearer " + adminToken)
                    .header("Content-Type", "application/json")
                    .body(roleJson)
                    .retrieve()
                    .toBodilessEntity();

            logger.info("Role {} assigned to user {}", roleName, userId);
        } catch (Exception e) {
            logger.error("Error assigning role to user", e);
            throw new RuntimeException("Failed to assign role: " + e.getMessage());
        }
    }

    @Override
    public String getAdminAccessToken() {
        String tokenUrl = keycloakUrl + "/realms/master/protocol/openid-connect/token";

        String requestBody = "grant_type=password" +
                "&client_id=admin-cli" +
                "&username=" + URLEncoder.encode(adminUsername, StandardCharsets.UTF_8) +
                "&password=" + URLEncoder.encode(adminPassword, StandardCharsets.UTF_8);

        try {
            var response = restClient.post()
                    .uri(tokenUrl)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .body(requestBody)
                    .retrieve()
                    .body(Map.class);

            if (response == null || !response.containsKey("access_token")) {
                throw new RuntimeException("Failed to get admin token from Keycloak");
            }

            return (String) response.get("access_token");
        } catch (Exception e) {
            logger.error("Error getting admin token", e);
            throw new RuntimeException("Failed to get admin token: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> getUserInfoFromToken(Jwt jwt) {
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", jwt.getSubject());
        userInfo.put("username", jwt.getClaimAsString("preferred_username"));
        userInfo.put("email", jwt.getClaimAsString("email"));
        userInfo.put("firstName", jwt.getClaimAsString("given_name"));
        userInfo.put("lastName", jwt.getClaimAsString("family_name"));

        // Extract roles
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess != null) {
            @SuppressWarnings("unchecked")
            List<String> roles = (List<String>) realmAccess.get("roles");
            userInfo.put("roles", roles);
        }

        return userInfo;
    }

    @Override
    public String findUserIdByEmail(String email) {
        String adminToken = getAdminAccessToken();
        // Dùng search để linh hoạt hơn, lấy user đầu tiên khớp với email/username
        String url = keycloakUrl + "/admin/realms/" + realm + "/users?search="
                + URLEncoder.encode(email, StandardCharsets.UTF_8);
        try {
            var response = restClient.get()
                    .uri(url)
                    .header("Authorization", "Bearer " + adminToken)
                    .retrieve()
                    .body(List.class);
            if (response == null || response.isEmpty()) {
                return null;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> user = (Map<String, Object>) response.get(0);
            return (String) user.get("id");
        } catch (Exception e) {
            logger.error("Error finding user by email", e);
            throw new RuntimeException("Failed to find user by email: " + e.getMessage());
        }
    }

    @Override
    public void updateUserPassword(String userId, String newPassword) {
        String adminToken = getAdminAccessToken();
        String url = keycloakUrl + "/admin/realms/" + realm + "/users/" + userId + "/reset-password";
        try {
            Map<String, Object> credential = Map.of(
                    "type", "password",
                    "value", newPassword,
                    "temporary", false
            );
            String body = objectMapper.writeValueAsString(credential);
            restClient.put()
                    .uri(url)
                    .header("Authorization", "Bearer " + adminToken)
                    .header("Content-Type", "application/json")
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            logger.error("Error updating user password", e);
            throw new RuntimeException("Failed to update user password: " + e.getMessage());
        }
    }
}
