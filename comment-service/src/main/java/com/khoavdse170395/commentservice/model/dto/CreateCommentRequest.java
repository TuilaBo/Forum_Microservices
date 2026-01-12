package com.khoavdse170395.commentservice.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CreateCommentRequest {

    @NotNull(message = "Post ID is required")
    private Long postId;

    @NotBlank(message = "Content is required")
    private String content;

    // Getters and Setters
    public Long getPostId() {
        return postId;
    }

    public void setPostId(Long postId) {
        this.postId = postId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
