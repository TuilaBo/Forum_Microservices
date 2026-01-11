package com.khoavdse170395.postservice.model.event;

import java.time.LocalDateTime;

/**
 * Event được publish khi một bài viết được cập nhật.
 * 
 * Event-driven Architecture:
 * - Khi user cập nhật post → PostServiceImpl.updatePost() → publish PostUpdatedEvent
 * - Các service khác có thể subscribe để cập nhật cache, search index, v.v.
 */
public class PostUpdatedEvent {
    
    private Long postId;
    private String title;
    private String content;
    private String authorId;
    private LocalDateTime updatedAt;
    private String eventType = "PostUpdatedEvent";
    private LocalDateTime eventTimestamp;

    public PostUpdatedEvent() {
        this.eventTimestamp = LocalDateTime.now();
    }

    public PostUpdatedEvent(Long postId, String title, String content, String authorId, LocalDateTime updatedAt) {
        this.postId = postId;
        this.title = title;
        this.content = content;
        this.authorId = authorId;
        this.updatedAt = updatedAt;
        this.eventType = "PostUpdatedEvent";
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

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
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
        return "PostUpdatedEvent{" +
                "postId=" + postId +
                ", title='" + title + '\'' +
                ", authorId='" + authorId + '\'' +
                ", eventTimestamp=" + eventTimestamp +
                '}';
    }
}
