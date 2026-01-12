package com.khoavdse170395.notificationservice.service.impl;

import com.khoavdse170395.notificationservice.model.Notification;
import com.khoavdse170395.notificationservice.repository.NotificationRepository;
import com.khoavdse170395.notificationservice.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class NotificationServiceImpl implements NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Override
    public void createCommentNotification(String postAuthorId, String commentAuthorUsername, 
                                         Long postId, Long commentId, String commentContent) {
        // Tạo notification cho chủ bài viết
        Notification notification = new Notification();
        notification.setUserId(postAuthorId); // Người nhận notification
        notification.setType("COMMENT_ON_POST");
        notification.setTitle("Có comment mới trên bài viết của bạn");
        notification.setMessage(String.format("%s đã comment vào bài viết của bạn: \"%s\"", 
            commentAuthorUsername, 
            commentContent.length() > 50 ? commentContent.substring(0, 50) + "..." : commentContent));
        notification.setRelatedPostId(postId);
        notification.setRelatedCommentId(commentId);
        notification.setIsRead(false);
        
        notificationRepository.save(notification);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Notification> getUserNotifications(String userId, Pageable pageable) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    @Override
    public void markAsRead(Long notificationId, String userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found with id: " + notificationId));
        
        // Kiểm tra quyền: chỉ user sở hữu notification mới được đánh dấu đã đọc
        if (!notification.getUserId().equals(userId)) {
            throw new RuntimeException("You do not have permission to mark this notification as read.");
        }
        
        notification.setIsRead(true);
        notificationRepository.save(notification);
    }

    @Override
    @Transactional(readOnly = true)
    public Long countUnreadNotifications(String userId) {
        return notificationRepository.countByUserIdAndIsRead(userId, false);
    }
}
