package com.khoavdse170395.postservice.model.event;

import java.time.LocalDateTime;

/**
 * Event được publish khi một bài viết bị xóa.
 * 
 * Event-driven Architecture:
 * - Khi user xóa post → PostServiceImpl.deletePost() → publish PostDeletedEvent
 * - Các service khác có thể subscribe để xóa cache, search index, comments liên quan, v.v.
 */
public class PostDeletedEvent {
    
    private Long postId;
    private String authorId;
    private String eventType = "PostDeletedEvent";
    private LocalDateTime eventTimestamp;

    public PostDeletedEvent() {
        this.eventTimestamp = LocalDateTime.now();
    }

    public PostDeletedEvent(Long postId, String authorId) {
        this.postId = postId;
        this.authorId = authorId;
        this.eventType = "PostDeletedEvent";
        this.eventTimestamp = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getPostId() {
        return postId;
    }

    public void setPostId(Long postId) {
        this.postId = postId;
    }

    public String getAuthorId() {
        return authorId;
    }

    public void setAuthorId(String authorId) {
        this.authorId = authorId;
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
        return "PostDeletedEvent{" +
                "postId=" + postId +
                ", authorId='" + authorId + '\'' +
                ", eventTimestamp=" + eventTimestamp +
                '}';
    }
}
