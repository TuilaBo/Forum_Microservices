package com.khoavdse170395.notificationservice.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Notification entity để lưu thông báo cho users.
 * 
 * Khi có comment mới vào bài viết, lưu notification cho chủ bài viết.
 */
@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private String userId; // ID của user nhận notification (post author)

    @Column(name = "type", nullable = false)
    private String type; // "COMMENT_ON_POST", "POST_CREATED", etc.

    @Column(name = "title", nullable = false)
    private String title; // Tiêu đề notification

    @Column(name = "message", columnDefinition = "TEXT")
    private String message; // Nội dung notification

    @Column(name = "related_post_id")
    private Long relatedPostId; // ID của post liên quan

    @Column(name = "related_comment_id")
    private Long relatedCommentId; // ID của comment liên quan (nếu có)

    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false; // Đã đọc chưa

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (isRead == null) {
            isRead = false;
        }
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getRelatedPostId() {
        return relatedPostId;
    }

    public void setRelatedPostId(Long relatedPostId) {
        this.relatedPostId = relatedPostId;
    }

    public Long getRelatedCommentId() {
        return relatedCommentId;
    }

    public void setRelatedCommentId(Long relatedCommentId) {
        this.relatedCommentId = relatedCommentId;
    }

    public Boolean getIsRead() {
        return isRead;
    }

    public void setIsRead(Boolean isRead) {
        this.isRead = isRead;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
