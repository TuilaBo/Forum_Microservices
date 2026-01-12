package com.khoavdse170395.userservice.controller;

import com.khoavdse170395.userservice.model.dto.UpdateUserRequest;
import com.khoavdse170395.userservice.model.dto.UserResponse;
import com.khoavdse170395.userservice.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/users")
@Tag(name = "User Management", description = "APIs để quản lý thông tin user profile")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * Lấy thông tin user hiện tại (từ JWT).
     */
    @GetMapping("/me")
    @Operation(summary = "Get current user profile", description = "Lấy thông tin profile của user hiện tại")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('STUDENT') or hasRole('TEACHER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    public ResponseEntity<UserResponse> getCurrentUser(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        String username = jwt.getClaimAsString("preferred_username");
        String email = jwt.getClaimAsString("email");
        UserResponse user = userService.getCurrentUser(userId, username, email);
        return ResponseEntity.ok(user);
    }

    /**
     * Lấy thông tin user theo ID.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID", description = "Lấy thông tin user theo ID")
    public ResponseEntity<UserResponse> getUserById(@PathVariable String id) {
        UserResponse user = userService.getUserById(id);
        return ResponseEntity.ok(user);
    }

    /**
     * Lấy thông tin user theo username.
     */
    @GetMapping("/username/{username}")
    @Operation(summary = "Get user by username", description = "Lấy thông tin user theo username")
    public ResponseEntity<UserResponse> getUserByUsername(@PathVariable String username) {
        UserResponse user = userService.getUserByUsername(username);
        return ResponseEntity.ok(user);
    }

    /**
     * Cập nhật thông tin user (firstName, lastName, bio).
     */
    @PutMapping("/me")
    @Operation(summary = "Update current user profile", description = "Cập nhật thông tin profile của user hiện tại")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('STUDENT') or hasRole('TEACHER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    public ResponseEntity<UserResponse> updateCurrentUser(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateUserRequest request) {
        String userId = jwt.getSubject();
        UserResponse updatedUser = userService.updateUser(userId, request);
        return ResponseEntity.ok(updatedUser);
    }

    /**
     * Upload avatar cho user hiện tại.
     * Chấp nhận file ảnh: JPEG, PNG, GIF, WEBP
     * Kích thước tối đa: 5MB
     */
    @PostMapping("/me/avatar")
    @Operation(
        summary = "Upload avatar", 
        description = "Upload avatar cho user hiện tại. Chấp nhận file ảnh (JPEG, PNG, GIF, WEBP) với kích thước tối đa 5MB"
    )
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('STUDENT') or hasRole('TEACHER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    public ResponseEntity<UserResponse> uploadAvatar(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam("file") MultipartFile file) {
        // Validate file
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File không được để trống");
        }
        
        // Validate file type
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("File phải là ảnh (JPEG, PNG, GIF, WEBP)");
        }
        
        // Validate file size (5MB = 5 * 1024 * 1024 bytes)
        long maxSize = 5 * 1024 * 1024; // 5MB
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException("Kích thước file không được vượt quá 5MB");
        }
        
        String userId = jwt.getSubject();
        UserResponse updatedUser = userService.uploadAvatar(userId, file);
        return ResponseEntity.ok(updatedUser);
    }

    /**
     * Xóa avatar của user hiện tại.
     */
    @DeleteMapping("/me/avatar")
    @Operation(summary = "Delete avatar", description = "Xóa avatar của user hiện tại")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('STUDENT') or hasRole('TEACHER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    public ResponseEntity<UserResponse> deleteAvatar(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        UserResponse updatedUser = userService.deleteAvatar(userId);
        return ResponseEntity.ok(updatedUser);
    }

    /**
     * Tìm kiếm users.
     */
    @GetMapping("/search")
    @Operation(summary = "Search users", description = "Tìm kiếm users theo keyword (username, firstName, lastName)")
    public ResponseEntity<Page<UserResponse>> searchUsers(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("username").ascending());
        Page<UserResponse> users = userService.searchUsers(keyword, pageable);
        return ResponseEntity.ok(users);
    }
}
