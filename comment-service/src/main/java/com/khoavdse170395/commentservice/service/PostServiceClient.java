package com.khoavdse170395.commentservice.service;

/**
 * Client để gọi post-service API.
 * 
 * Mục đích: Lấy thông tin post (bao gồm postAuthorId) để gửi notification.
 */
public interface PostServiceClient {

    /**
     * Lấy authorId của post.
     * 
     * @param postId ID của post
     * @return authorId của post, hoặc null nếu post không tồn tại
     */
    String getPostAuthorId(Long postId);
}
