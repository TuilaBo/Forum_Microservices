package com.khoavdse170395.notificationservice.controller;

import com.khoavdse170395.notificationservice.model.Notification;
import com.khoavdse170395.notificationservice.service.EmailService;
import com.khoavdse170395.notificationservice.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

/**
 * Controller để quản lý notifications và test email service.
 */
@RestController
@RequestMapping("/notifications")
@Tag(name = "Notification Controller", description = "API quản lý notifications và email service")
public class NotificationController {

    @Autowired
    private EmailService emailService;

    @Autowired
    private NotificationService notificationService;

    /**
     * Test endpoint để gửi email.
     */
    @PostMapping("/test-email")
    @Operation(summary = "Test gửi email", description = "Endpoint để test gửi email (không cần authentication)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Email gửi thành công"),
            @ApiResponse(responseCode = "500", description = "Lỗi khi gửi email")
    })
    public ResponseEntity<String> testEmail(
            @Parameter(description = "Email người nhận", required = true) @RequestParam String to,
            @Parameter(description = "Tiêu đề email", required = true) @RequestParam String subject,
            @Parameter(description = "Nội dung email", required = true) @RequestParam String body) {
        try {
            emailService.sendEmail(to, subject, body);
            return ResponseEntity.ok("Email sent successfully to: " + to);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed to send email: " + e.getMessage());
        }
    }

    /**
     * Lấy danh sách notifications của user hiện tại.
     */
    @GetMapping
    @Operation(summary = "Lấy danh sách notifications", description = "Lấy danh sách notifications của user hiện tại với phân trang")
    @SecurityRequirement(name = "bearer-keycloak")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lấy danh sách notifications thành công"),
            @ApiResponse(responseCode = "401", description = "Chưa đăng nhập")
    })
    public ResponseEntity<Page<Notification>> getMyNotifications(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Số trang (bắt đầu từ 0)", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Số lượng notifications mỗi trang", example = "10") @RequestParam(defaultValue = "10") int size) {
        String userId = jwt.getSubject();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Notification> notifications = notificationService.getUserNotifications(userId, pageable);
        return ResponseEntity.ok(notifications);
    }

    /**
     * Đánh dấu notification là đã đọc.
     */
    @PutMapping("/{id}/read")
    @Operation(summary = "Đánh dấu notification đã đọc", description = "Đánh dấu một notification là đã đọc")
    @SecurityRequirement(name = "bearer-keycloak")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Đánh dấu thành công"),
            @ApiResponse(responseCode = "401", description = "Chưa đăng nhập"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy notification")
    })
    public ResponseEntity<Void> markAsRead(
            @Parameter(description = "ID của notification", required = true) @PathVariable Long id,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        notificationService.markAsRead(id, userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Đếm số notifications chưa đọc.
     */
    @GetMapping("/unread-count")
    @Operation(summary = "Đếm notifications chưa đọc", description = "Lấy số lượng notifications chưa đọc của user hiện tại")
    @SecurityRequirement(name = "bearer-keycloak")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lấy số lượng thành công"),
            @ApiResponse(responseCode = "401", description = "Chưa đăng nhập")
    })
    public ResponseEntity<Long> getUnreadCount(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        Long count = notificationService.countUnreadNotifications(userId);
        return ResponseEntity.ok(count);
    }

    /**
     * Health check endpoint.
     */
    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Kiểm tra trạng thái của notification service")
    @ApiResponse(responseCode = "200", description = "Service đang chạy")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Notification Service is running");
    }
}
