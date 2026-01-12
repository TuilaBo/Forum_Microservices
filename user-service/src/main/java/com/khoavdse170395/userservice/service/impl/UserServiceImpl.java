package com.khoavdse170395.userservice.service.impl;

import com.khoavdse170395.userservice.model.User;
import com.khoavdse170395.userservice.model.dto.UpdateUserRequest;
import com.khoavdse170395.userservice.model.dto.UserResponse;
import com.khoavdse170395.userservice.repository.UserRepository;
import com.khoavdse170395.userservice.service.CloudinaryService;
import com.khoavdse170395.userservice.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CloudinaryService cloudinaryService;

    @Override
    public UserResponse getCurrentUser(String userId, String username, String email) {
        User user = userRepository.findById(userId).orElse(null);
        
        // Nếu user chưa tồn tại, tự động tạo từ thông tin Keycloak
        if (user == null) {
            user = new User();
            user.setId(userId);
            user.setUsername(username);
            user.setEmail(email);
            user = userRepository.save(user);
        }
        
        return mapToResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
        return mapToResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found with username: " + username));
        return mapToResponse(user);
    }

    @Override
    public UserResponse createOrUpdateUserFromKeycloak(String id, String username, String email) {
        User user = userRepository.findById(id).orElse(new User());
        
        // Update thông tin từ Keycloak
        user.setId(id);
        user.setUsername(username);
        user.setEmail(email);
        
        // Nếu là user mới, giữ các field khác null
        // Nếu là user cũ, giữ nguyên firstName, lastName, bio, avatarUrl
        
        User savedUser = userRepository.save(user);
        return mapToResponse(savedUser);
    }

    @Override
    public UserResponse updateUser(String userId, UpdateUserRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        // Chỉ update các field được phép
        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }
        if (request.getBio() != null) {
            user.setBio(request.getBio());
        }

        User updatedUser = userRepository.save(user);
        return mapToResponse(updatedUser);
    }

    @Override
    public UserResponse uploadAvatar(String userId, MultipartFile file) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        // Xóa avatar cũ nếu có
        if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
            try {
                cloudinaryService.deleteImage(user.getAvatarUrl());
            } catch (Exception e) {
                // Log error nhưng không throw để không block upload avatar mới
                System.err.println("Error deleting old avatar: " + e.getMessage());
            }
        }

        // Upload avatar mới
        String avatarUrl = cloudinaryService.uploadImage(file);
        user.setAvatarUrl(avatarUrl);

        User updatedUser = userRepository.save(user);
        return mapToResponse(updatedUser);
    }

    @Override
    public UserResponse deleteAvatar(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
            try {
                cloudinaryService.deleteImage(user.getAvatarUrl());
            } catch (Exception e) {
                System.err.println("Error deleting avatar: " + e.getMessage());
            }
            user.setAvatarUrl(null);
        }

        User updatedUser = userRepository.save(user);
        return mapToResponse(updatedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> searchUsers(String keyword, Pageable pageable) {
        return userRepository.searchUsers(keyword, pageable)
                .map(this::mapToResponse);
    }

    private UserResponse mapToResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setFirstName(user.getFirstName());
        response.setLastName(user.getLastName());
        response.setBio(user.getBio());
        response.setAvatarUrl(user.getAvatarUrl());
        response.setCreatedAt(user.getCreatedAt());
        response.setUpdatedAt(user.getUpdatedAt());
        return response;
    }
}
