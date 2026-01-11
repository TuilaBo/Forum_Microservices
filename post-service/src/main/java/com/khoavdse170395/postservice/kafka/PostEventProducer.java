package com.khoavdse170395.postservice.kafka;

import com.khoavdse170395.postservice.model.event.PostCreatedEvent;
import com.khoavdse170395.postservice.model.event.PostDeletedEvent;
import com.khoavdse170395.postservice.model.event.PostUpdatedEvent;

/**
 * Interface cho Kafka Event Producer.
 * 
 * Responsibility:
 * - Publish events lên Kafka topics
 * - Tách biệt business logic (PostService) với messaging infrastructure (Kafka)
 * 
 * Design Pattern: Strategy Pattern
 * - Có thể thay đổi implementation (Kafka, RabbitMQ, Redis Pub/Sub...) mà không ảnh hưởng business logic
 */
public interface PostEventProducer {

    /**
     * Publish PostCreatedEvent lên Kafka topic "post-created"
     * 
     * @param event PostCreatedEvent chứa thông tin bài viết vừa tạo
     */
    void publishPostCreated(PostCreatedEvent event);

    /**
     * Publish PostUpdatedEvent lên Kafka topic "post-updated"
     * 
     * @param event PostUpdatedEvent chứa thông tin bài viết vừa cập nhật
     */
    void publishPostUpdated(PostUpdatedEvent event);

    /**
     * Publish PostDeletedEvent lên Kafka topic "post-deleted"
     * 
     * @param event PostDeletedEvent chứa thông tin bài viết vừa xóa
     */
    void publishPostDeleted(PostDeletedEvent event);
}
