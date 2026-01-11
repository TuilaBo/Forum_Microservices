package com.khoavdse170395.postservice.model.event;

import java.time.LocalDateTime;

/**
 * Event được publish khi một bài viết mới được tạo.
 * 
 * Event-driven Architecture:
 * - Khi user tạo post → PostServiceImpl.createPost() → publish PostCreatedEvent
 * - Các service khác (notification-service, search-service...) có thể subscribe để xử lý
 */
public class PostCreatedEvent {
    
    private Long postId;
    private String title;
    private String content;
    private String authorId;
    private String authorUsername;
    private LocalDateTime createdAt;
    private String eventType = "PostCreatedEvent";
    private LocalDateTime eventTimestamp;

    public PostCreatedEvent() {
        this.eventTimestamp = LocalDateTime.now();
    }

    public PostCreatedEvent(Long postId, String title, String content, String authorId, String authorUsername, LocalDateTime createdAt) {
        this.postId = postId;
        this.title = title;
        this.content = content;
        this.authorId = authorId;
        this.authorUsername = authorUsername;
        this.createdAt = createdAt;
        this.eventType = "PostCreatedEvent";
        this.eventTimestamp = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getPostId() {
        return postId;
    }

    public void setPostId(Long postId) {
        this.postId = postId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getAuthorId() {
        return authorId;
    }

    public void setAuthorId(String authorId) {
        this.authorId = authorId;
    }

    public String getAuthorUsername() {
        return authorUsername;
    }

    public void setAuthorUsername(String authorUsername) {
        this.authorUsername = authorUsername;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public LocalDateTime getEventTimestamp() {
        return eventTimestamp;
    }

    public void setEventTimestamp(LocalDateTime eventTimestamp) {
        this.eventTimestamp = eventTimestamp;
    }

    @Override
    public String toString() {
        return "PostCreatedEvent{" +
                "postId=" + postId +
                ", title='" + title + '\'' +
                ", authorId='" + authorId + '\'' +
                ", authorUsername='" + authorUsername + '\'' +
                ", eventTimestamp=" + eventTimestamp +
                '}';
    }
}
