package com.khoavdse170395.postservice.kafka.impl;

import com.khoavdse170395.postservice.kafka.PostEventProducer;
import com.khoavdse170395.postservice.model.event.PostCreatedEvent;
import com.khoavdse170395.postservice.model.event.PostDeletedEvent;
import com.khoavdse170395.postservice.model.event.PostUpdatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Implementation của PostEventProducer sử dụng Kafka.
 * 
 * Kafka Concepts:
 * 1. Producer: Service này là Kafka Producer - gửi messages lên Kafka
 * 2. Topic: Mỗi event type có một topic riêng (post-created, post-updated, post-deleted)
 * 3. Message: Event object được serialize thành JSON và gửi lên topic
 * 4. Async: Sử dụng CompletableFuture để không block thread khi gửi message
 * 
 * Flow:
 * PostServiceImpl → PostEventProducer → KafkaTemplate → Kafka Broker → Consumers
 */
@Service
public class KafkaPostEventProducer implements PostEventProducer {

    private static final Logger logger = LoggerFactory.getLogger(KafkaPostEventProducer.class);

    // Kafka topics - mỗi event type có một topic riêng
    private static final String TOPIC_POST_CREATED = "post-created";
    private static final String TOPIC_POST_UPDATED = "post-updated";
    private static final String TOPIC_POST_DELETED = "post-deleted";

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public void publishPostCreated(PostCreatedEvent event) {
        try {
            logger.info("Publishing PostCreatedEvent: {}", event);
            
            // Gửi message lên Kafka topic "post-created"
            // Key: postId (để đảm bảo messages của cùng một post đi vào cùng partition)
            // Value: event object (sẽ được serialize thành JSON tự động)
            CompletableFuture<SendResult<String, Object>> future = 
                kafkaTemplate.send(TOPIC_POST_CREATED, event.getPostId().toString(), event);
            
            // Callback để log kết quả
            future.whenComplete((result, exception) -> {
                if (exception == null) {
                    logger.info("Successfully published PostCreatedEvent to topic: {}, offset: {}", 
                        TOPIC_POST_CREATED, result.getRecordMetadata().offset());
                } else {
                    logger.error("Failed to publish PostCreatedEvent to topic: {}", 
                        TOPIC_POST_CREATED, exception);
                }
            });
        } catch (Exception e) {
            logger.error("Error publishing PostCreatedEvent", e);
            // Không throw exception để không ảnh hưởng đến business logic
            // Có thể implement retry mechanism hoặc dead letter queue sau
        }
    }

    @Override
    public void publishPostUpdated(PostUpdatedEvent event) {
        try {
            logger.info("Publishing PostUpdatedEvent: {}", event);
            
            CompletableFuture<SendResult<String, Object>> future = 
                kafkaTemplate.send(TOPIC_POST_UPDATED, event.getPostId().toString(), event);
            
            future.whenComplete((result, exception) -> {
                if (exception == null) {
                    logger.info("Successfully published PostUpdatedEvent to topic: {}, offset: {}", 
                        TOPIC_POST_UPDATED, result.getRecordMetadata().offset());
                } else {
                    logger.error("Failed to publish PostUpdatedEvent to topic: {}", 
                        TOPIC_POST_UPDATED, exception);
                }
            });
        } catch (Exception e) {
            logger.error("Error publishing PostUpdatedEvent", e);
        }
    }

    @Override
    public void publishPostDeleted(PostDeletedEvent event) {
        try {
            logger.info("Publishing PostDeletedEvent: {}", event);
            
            CompletableFuture<SendResult<String, Object>> future = 
                kafkaTemplate.send(TOPIC_POST_DELETED, event.getPostId().toString(), event);
            
            future.whenComplete((result, exception) -> {
                if (exception == null) {
                    logger.info("Successfully published PostDeletedEvent to topic: {}, offset: {}", 
                        TOPIC_POST_DELETED, result.getRecordMetadata().offset());
                } else {
                    logger.error("Failed to publish PostDeletedEvent to topic: {}", 
                        TOPIC_POST_DELETED, exception);
                }
            });
        } catch (Exception e) {
            logger.error("Error publishing PostDeletedEvent", e);
        }
    }
}
