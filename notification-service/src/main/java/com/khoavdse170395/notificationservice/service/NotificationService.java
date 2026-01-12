package com.khoavdse170395.notificationservice.service;

import com.khoavdse170395.notificationservice.model.Notification;

/**
 * Service để quản lý notifications (lưu vào database, không gửi email).
 */
public interface NotificationService {

    /**
     * Tạo notification khi có comment mới vào bài viết.
     * 
     * @param postAuthorId ID của người sở hữu post
     * @param commentAuthorUsername Tên người comment
     * @param postId ID của post
     * @param commentId ID của comment
     * @param commentContent Nội dung comment
     */
    void createCommentNotification(String postAuthorId, String commentAuthorUsername, 
                                   Long postId, Long commentId, String commentContent);

    /**
     * Lấy danh sách notifications của user.
     */
    org.springframework.data.domain.Page<Notification> getUserNotifications(String userId, 
                                                                              org.springframework.data.domain.Pageable pageable);

    /**
     * Đánh dấu notification là đã đọc.
     */
    void markAsRead(Long notificationId, String userId);

    /**
     * Đếm số notifications chưa đọc của user.
     */
    Long countUnreadNotifications(String userId);
}
