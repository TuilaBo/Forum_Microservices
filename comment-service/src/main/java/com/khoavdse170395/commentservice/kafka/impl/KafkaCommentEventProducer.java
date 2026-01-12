package com.khoavdse170395.commentservice.kafka.impl;

import com.khoavdse170395.commentservice.kafka.CommentEventProducer;
import com.khoavdse170395.commentservice.model.event.CommentCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Implementation của CommentEventProducer sử dụng Kafka.
 * 
 * Kafka Concepts:
 * 1. Producer: Service này là Kafka Producer - gửi messages lên Kafka
 * 2. Topic: "comment-created" - khi có comment mới
 * 3. Message: Event object được serialize thành JSON và gửi lên topic
 * 4. Async: Sử dụng CompletableFuture để không block thread khi gửi message
 * 
 * Flow:
 * CommentServiceImpl → CommentEventProducer → KafkaTemplate → Kafka Broker → Consumers
 */
@Service
public class KafkaCommentEventProducer implements CommentEventProducer {

    private static final Logger logger = LoggerFactory.getLogger(KafkaCommentEventProducer.class);

    // Kafka topic - chỉ gửi thông báo khi comment mới được tạo
    private static final String TOPIC_COMMENT_CREATED = "comment-created";

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public void publishCommentCreated(CommentCreatedEvent event) {
        try {
            logger.info("Publishing CommentCreatedEvent: {}", event);
            
            // Gửi message lên Kafka topic "comment-created"
            // Key: postId (để đảm bảo messages của cùng một post đi vào cùng partition)
            // Value: event object (sẽ được serialize thành JSON tự động)
            CompletableFuture<SendResult<String, Object>> future = 
                kafkaTemplate.send(TOPIC_COMMENT_CREATED, event.getPostId().toString(), event);
            
            // Callback để log kết quả
            future.whenComplete((result, exception) -> {
                if (exception == null) {
                    logger.info("Successfully published CommentCreatedEvent to topic: {}, offset: {}", 
                        TOPIC_COMMENT_CREATED, result.getRecordMetadata().offset());
                } else {
                    logger.error("Failed to publish CommentCreatedEvent to topic: {}", 
                        TOPIC_COMMENT_CREATED, exception);
                }
            });
        } catch (Exception e) {
            logger.error("Error publishing CommentCreatedEvent", e);
            // Không throw exception để không ảnh hưởng đến business logic
            // Có thể implement retry mechanism hoặc dead letter queue sau
        }
    }
}
