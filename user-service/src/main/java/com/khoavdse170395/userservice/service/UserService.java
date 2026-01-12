package com.khoavdse170395.userservice.service;

import com.khoavdse170395.userservice.model.dto.UpdateUserRequest;
import com.khoavdse170395.userservice.model.dto.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {

    /**
     * Lấy thông tin user hiện tại (từ JWT).
     * Tự động tạo user nếu chưa tồn tại trong database.
     */
    UserResponse getCurrentUser(String userId, String username, String email);

    /**
     * Lấy thông tin user theo ID.
     */
    UserResponse getUserById(String id);

    /**
     * Lấy thông tin user theo username.
     */
    UserResponse getUserByUsername(String username);

    /**
     * Tạo hoặc cập nhật user profile từ Keycloak info.
     * Được gọi khi user đăng nhập lần đầu hoặc thông tin Keycloak thay đổi.
     */
    UserResponse createOrUpdateUserFromKeycloak(String id, String username, String email);

    /**
     * Cập nhật thông tin user (firstName, lastName, bio).
     */
    UserResponse updateUser(String userId, UpdateUserRequest request);

    /**
     * Upload avatar cho user.
     */
    UserResponse uploadAvatar(String userId, MultipartFile file);

    /**
     * Xóa avatar của user.
     */
    UserResponse deleteAvatar(String userId);

    /**
     * Tìm kiếm users theo keyword (username, firstName, lastName).
     */
    Page<UserResponse> searchUsers(String keyword, Pageable pageable);
}
