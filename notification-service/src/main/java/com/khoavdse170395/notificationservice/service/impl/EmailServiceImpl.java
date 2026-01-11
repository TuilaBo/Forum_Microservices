package com.khoavdse170395.notificationservice.service.impl;

import com.khoavdse170395.notificationservice.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Implementation của EmailService sử dụng Gmail SMTP.
 */
@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailServiceImpl.class);

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${notification.moderator.email:moderator@school.edu}")
    private String moderatorEmail;

    @Value("${notification.admin.email:admin@school.edu}")
    private String adminEmail;

    @Override
    public void sendEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            
            mailSender.send(message);
            logger.info("Email sent successfully to: {}", to);
        } catch (Exception e) {
            logger.error("Failed to send email to: {}", to, e);
            // Không throw exception để không ảnh hưởng đến Kafka consumer
        }
    }

    @Override
    public void sendEmailToModerator(Long postId, String title, String authorUsername) {
        String subject = "Có bài viết mới cần duyệt - Post ID: " + postId;
        String body = String.format(
            "Xin chào Moderator,\n\n" +
            "Có một bài viết mới cần được duyệt:\n\n" +
            "Post ID: %d\n" +
            "Tiêu đề: %s\n" +
            "Tác giả: %s\n\n" +
            "Vui lòng đăng nhập vào hệ thống để duyệt bài viết này.\n\n" +
            "Trân trọng,\n" +
            "School Forum System",
            postId, title, authorUsername
        );
        
        sendEmail(moderatorEmail, subject, body);
    }

    @Override
    public void sendEmailToAdmin(Long postId, String title, String authorUsername) {
        String subject = "Thông báo: Bài viết mới - Post ID: " + postId;
        String body = String.format(
            "Xin chào Admin,\n\n" +
            "Có một bài viết mới được tạo trong hệ thống:\n\n" +
            "Post ID: %d\n" +
            "Tiêu đề: %s\n" +
            "Tác giả: %s\n\n" +
            "Trân trọng,\n" +
            "School Forum System",
            postId, title, authorUsername
        );
        
        sendEmail(adminEmail, subject, body);
    }
}
