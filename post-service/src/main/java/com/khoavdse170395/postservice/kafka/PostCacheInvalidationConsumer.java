package com.khoavdse170395.postservice.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Kafka Consumer để nhận Debezium CDC events và invalidate Redis cache.
 * 
 * Flow:
 * DB Change → Debezium → Kafka Topic (dbserver1.postservice.posts) → This Consumer → Redis DEL
 * 
 * Debezium CDC Message Format:
 * {
 *   "before": { "id": 123, "title": "Old", ... },
 *   "after": { "id": 123, "title": "New", ... },
 *   "source": { "table": "posts", "db": "postservice", ... },
 *   "op": "u" (u=update, c=create, d=delete),
 *   "ts_ms": 1234567890
 * }
 */
@Component
public class PostCacheInvalidationConsumer {

    private static final Logger logger = LoggerFactory.getLogger(PostCacheInvalidationConsumer.class);

    private static final String CACHE_KEY_PREFIX = "post:";

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * Listen to Debezium CDC events from Kafka topic.
     * Topic name format: {database.server.name}.{schema.name}.{table.name}
     * Example: dbserver1.public.posts
     */
    @KafkaListener(
        topics = "dbserver1.public.posts",
        groupId = "post-cache-invalidation-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handlePostCdc(@Payload LinkedHashMap<String, Object> payload) {
        try {
            String op = (String) payload.get("op");
            if (op == null) {
                logger.warn("Received CDC event without 'op' field: {}", payload);
                return;
            }

            Long postId = extractPostId(payload, op);
            if (postId == null) {
                logger.warn("Could not extract postId from CDC event: {}", payload);
                return;
            }

            String cacheKey = CACHE_KEY_PREFIX + postId;

            // Invalidate cache for update/delete operations
            if ("u".equals(op) || "d".equals(op)) {
                redisTemplate.delete(cacheKey);
                logger.info("Cache invalidated for post:{} (op: {})", postId, op);
            } else if ("c".equals(op)) {
                // For create, we can optionally delete cache (though it shouldn't exist)
                // Or just log - cache will be populated on first read
                logger.debug("Post created: {} - Cache will be populated on first read", postId);
            }

        } catch (Exception e) {
            logger.error("Error processing CDC event for cache invalidation. Payload: {}, Error: {}", 
                payload, e.getMessage(), e);
            // Don't throw - continue processing other messages
        }
    }

    /**
     * Extract post ID from Debezium CDC payload.
     * For update/delete: try 'after' first, then 'before'
     * For create: use 'after'
     */
    private Long extractPostId(LinkedHashMap<String, Object> payload, String op) {
        Map<String, Object> data = null;

        if ("c".equals(op) || "u".equals(op)) {
            // Create or Update: use 'after'
            Object afterObj = payload.get("after");
            if (afterObj instanceof Map) {
                data = (Map<String, Object>) afterObj;
            }
        }

        if (data == null || !data.containsKey("id")) {
            // Fallback to 'before' for delete or if 'after' doesn't have id
            Object beforeObj = payload.get("before");
            if (beforeObj instanceof Map) {
                data = (Map<String, Object>) beforeObj;
            }
        }

        if (data != null && data.containsKey("id")) {
            Object idObj = data.get("id");
            if (idObj instanceof Number) {
                return ((Number) idObj).longValue();
            } else if (idObj instanceof String) {
                try {
                    return Long.parseLong((String) idObj);
                } catch (NumberFormatException e) {
                    logger.warn("Post ID is not a valid number: {}", idObj);
                }
            }
        }

        return null;
    }
}
