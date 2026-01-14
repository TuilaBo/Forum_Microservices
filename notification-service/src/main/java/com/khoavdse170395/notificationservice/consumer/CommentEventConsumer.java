package com.khoavdse170395.notificationservice.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.khoavdse170395.notificationservice.model.event.CommentCreatedEvent;
import com.khoavdse170395.notificationservice.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;

/**
 * Kafka Consumer để nhận và xử lý Comment Events.
 * 
 * Flow:
 * Kafka → CommentEventConsumer → NotificationService → Database (lưu notification)
 * 
 * KHÔNG gửi email, chỉ lưu notification vào database cho chủ bài viết.
 */
@Component
public class CommentEventConsumer {

    private static final Logger logger = LoggerFactory.getLogger(CommentEventConsumer.class);

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Nhận CommentCreatedEvent từ Kafka topic "comment-created".
     * 
     * Khi user B comment vào bài viết của user A:
     * - Lưu notification vào database cho user A (post author)
     * - KHÔNG gửi email
     * 
     * @param payload CommentCreatedEvent từ Kafka
     */
    @KafkaListener(
        topics = "comment-created", 
        groupId = "notification-service-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeCommentCreated(@Payload LinkedHashMap<String, Object> payload) {
        CommentCreatedEvent event = null;
        try {
            // Convert LinkedHashMap sang CommentCreatedEvent với ObjectMapper
            event = objectMapper.convertValue(payload, CommentCreatedEvent.class);
            logger.info("Received CommentCreatedEvent: {}", event);
            
            // ⭐ KIỂM TRA: Không gửi notification nếu người comment chính là chủ bài viết
            if (event.getPostAuthorId() != null && !event.getPostAuthorId().isEmpty()) {
                // So sánh authorId (người comment) với postAuthorId (chủ bài viết)
                if (event.getAuthorId() != null && event.getAuthorId().equals(event.getPostAuthorId())) {
                    logger.info("Skipping notification: User {} commented on their own post (postId: {}, commentId: {})", 
                        event.getAuthorId(), event.getPostId(), event.getCommentId());
                    return; // Không tạo notification
                }
                
                // ⭐ LƯU NOTIFICATION VÀO DATABASE CHO CHỦ BÀI VIẾT (KHÔNG GỬI EMAIL)
                notificationService.createCommentNotification(
                    event.getPostAuthorId(),        // User A (chủ bài viết)
                    event.getAuthorUsername(),      // User B (người comment)
                    event.getPostId(),              // Post ID
                    event.getCommentId(),           // Comment ID
                    event.getContent()              // Nội dung comment
                );
                
                logger.info("Notification created for post author: {} (postId: {}, commentId: {})", 
                    event.getPostAuthorId(), event.getPostId(), event.getCommentId());
            } else {
                logger.warn("PostAuthorId is null or empty, cannot create notification. Event: {}", event);
            }
            
            logger.info("Successfully processed CommentCreatedEvent for commentId: {}", event.getCommentId());
        } catch (Exception e) {
            logger.error("Error processing CommentCreatedEvent. Payload: {}, Error: {}", payload, e.getMessage(), e);
            // Không throw exception để không block Kafka consumer
        }
    }
}
