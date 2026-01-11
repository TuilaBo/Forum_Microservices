package com.khoavdse170395.notificationservice.service;

/**
 * Interface cho Email Service.
 */
public interface EmailService {
    
    /**
     * Gửi email đơn giản.
     * 
     * @param to Email người nhận
     * @param subject Tiêu đề email
     * @param body Nội dung email
     */
    void sendEmail(String to, String subject, String body);
    
    /**
     * Gửi email cho moderator khi có post mới cần duyệt.
     * 
     * @param postId ID của post
     * @param title Tiêu đề post
     * @param authorUsername Tên tác giả
     */
    void sendEmailToModerator(Long postId, String title, String authorUsername);
    
    /**
     * Gửi email cho admin khi có post mới.
     * 
     * @param postId ID của post
     * @param title Tiêu đề post
     * @param authorUsername Tên tác giả
     */
    void sendEmailToAdmin(Long postId, String title, String authorUsername);
}
