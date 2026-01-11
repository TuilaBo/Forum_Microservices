package com.khoavdse170395.authservice.repository;

import com.khoavdse170395.authservice.model.UserInfo;

import java.util.Optional;

/**
 * Repository tối giản cho user.
 * Hiện tại chỉ là interface để tách layer, bạn có thể
 * thay thế bằng JPA hoặc gọi sang user-service sau này.
 */
public interface UserRepository {

    Optional<UserInfo> findById(String id);

    UserInfo save(UserInfo userInfo);
}


