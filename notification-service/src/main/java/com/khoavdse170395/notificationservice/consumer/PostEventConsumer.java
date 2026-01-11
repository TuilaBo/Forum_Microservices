package com.khoavdse170395.notificationservice.consumer;

import com.khoavdse170395.notificationservice.model.event.PostCreatedEvent;
import com.khoavdse170395.notificationservice.model.event.PostDeletedEvent;
import com.khoavdse170395.notificationservice.model.event.PostUpdatedEvent;
import com.khoavdse170395.notificationservice.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka Consumer để nhận và xử lý Post Events.
 * 
 * Flow:
 * Kafka → PostEventConsumer → EmailService → Gmail SMTP
 */
@Component
public class PostEventConsumer {

    private static final Logger logger = LoggerFactory.getLogger(PostEventConsumer.class);

    @Autowired
    private EmailService emailService;

    /**
     * Nhận PostCreatedEvent từ Kafka topic "post-created".
     * 
     * @param event PostCreatedEvent
     */
    @KafkaListener(topics = "post-created", groupId = "notification-service-group")
    public void consumePostCreated(PostCreatedEvent event) {
        try {
            logger.info("Received PostCreatedEvent: {}", event);
            
            // Gửi email cho moderator
            emailService.sendEmailToModerator(
                event.getPostId(),
                event.getTitle(),
                event.getAuthorUsername()
            );
            
            // Gửi email cho admin
            emailService.sendEmailToAdmin(
                event.getPostId(),
                event.getTitle(),
                event.getAuthorUsername()
            );
            
            logger.info("Successfully processed PostCreatedEvent for postId: {}", event.getPostId());
        } catch (Exception e) {
            logger.error("Error processing PostCreatedEvent: {}", event, e);
            // Không throw exception để không block Kafka consumer
            // Có thể implement retry mechanism hoặc dead letter queue sau
        }
    }

    /**
     * Nhận PostUpdatedEvent từ Kafka topic "post-updated".
     * 
     * @param event PostUpdatedEvent
     */
    @KafkaListener(topics = "post-updated", groupId = "notification-service-group")
    public void consumePostUpdated(PostUpdatedEvent event) {
        try {
            logger.info("Received PostUpdatedEvent: {}", event);
            
            // Có thể gửi email thông báo post đã được cập nhật
            // Hiện tại chỉ log
            logger.info("Post updated - PostId: {}, Title: {}", event.getPostId(), event.getTitle());
        } catch (Exception e) {
            logger.error("Error processing PostUpdatedEvent: {}", event, e);
        }
    }

    /**
     * Nhận PostDeletedEvent từ Kafka topic "post-deleted".
     * 
     * @param event PostDeletedEvent
     */
    @KafkaListener(topics = "post-deleted", groupId = "notification-service-group")
    public void consumePostDeleted(PostDeletedEvent event) {
        try {
            logger.info("Received PostDeletedEvent: {}", event);
            
            // Có thể gửi email thông báo post đã bị xóa
            // Hiện tại chỉ log
            logger.info("Post deleted - PostId: {}", event.getPostId());
        } catch (Exception e) {
            logger.error("Error processing PostDeletedEvent: {}", event, e);
        }
    }
}
