package com.khoavdse170395.authservice.service;

import com.khoavdse170395.authservice.model.RegisterRequest;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Map;

/**
 * Service để gọi Keycloak Admin API.
 */
public interface KeycloakAdminService {

    /**
     * Tạo user mới trong Keycloak với role ROLE_STUDENT mặc định.
     */
    String createUser(RegisterRequest request);

    /**
     * Gán role cho user.
     */
    void assignRoleToUser(String userId, String roleName);

    /**
     * Lấy admin access token từ Keycloak.
     */
    String getAdminAccessToken();

    /**
     * Lấy user info từ JWT token.
     */
    Map<String, Object> getUserInfoFromToken(Jwt jwt);

    /**
     * Tìm userId theo email.
     */
    String findUserIdByEmail(String email);

    /**
     * Đổi mật khẩu user theo userId.
     */
    void updateUserPassword(String userId, String newPassword);
}
