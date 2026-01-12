package com.khoavdse170395.commentservice.kafka;

import com.khoavdse170395.commentservice.model.event.CommentCreatedEvent;

/**
 * Interface cho Kafka Event Producer.
 * 
 * Responsibility:
 * - Publish events lên Kafka topics
 * - Tách biệt business logic (CommentService) với messaging infrastructure (Kafka)
 * 
 * Design Pattern: Strategy Pattern
 * - Có thể thay đổi implementation (Kafka, RabbitMQ, Redis Pub/Sub...) mà không ảnh hưởng business logic
 */
public interface CommentEventProducer {

    /**
     * Publish CommentCreatedEvent lên Kafka topic "comment-created"
     * 
     * @param event CommentCreatedEvent chứa thông tin comment vừa tạo
     */
    void publishCommentCreated(CommentCreatedEvent event);
}
