# Comment Kafka Flow - Giải Thích Chi Tiết

## 📍 Đoạn Code Publish Event Khi Comment

### 1. CommentServiceImpl.java - Tạo Comment và Publish Event

**File:** `comment-service/src/main/java/com/khoavdse170395/commentservice/service/impl/CommentServiceImpl.java`

```java
@Override
public CommentResponse createComment(CreateCommentRequest request, String userId, String username) {
    // Bước 1: Lấy postAuthorId từ post-service
    String postAuthorId = postServiceClient.getPostAuthorId(request.getPostId());
    
    // Bước 2: Tạo comment
    Comment comment = new Comment();
    comment.setPostId(request.getPostId());
    comment.setContent(request.getContent());
    comment.setAuthorId(userId);
    comment.setAuthorUsername(username);
    
    Comment savedComment = commentRepository.save(comment);
    
    // ⭐ BƯỚC 3: Publish CommentCreatedEvent lên Kafka
    CommentCreatedEvent event = new CommentCreatedEvent(
        savedComment.getId(),
        savedComment.getPostId(),
        savedComment.getContent(),
        savedComment.getAuthorId(),
        savedComment.getAuthorUsername(),
        postAuthorId, // ID của người sở hữu post (để gửi notification)
        savedComment.getCreatedAt()
    );
    commentEventProducer.publishCommentCreated(event); // ← ĐÂY LÀ ĐOẠN CODE BẮN MESSAGE LÊN KAFKA
    
    return mapToResponse(savedComment);
}
```

**Giải thích:**
- **Dòng 55:** `commentEventProducer.publishCommentCreated(event)` 
  - → Gọi producer để publish event lên Kafka
  - → Event chứa `postAuthorId` (người sở hữu post) để notification-service biết gửi thông báo cho ai

---

### 2. KafkaCommentEventProducer.java - Gửi Message Lên Kafka

**File:** `comment-service/src/main/java/com/khoavdse170395/commentservice/kafka/impl/KafkaCommentEventProducer.java`

```java
@Override
public void publishCommentCreated(CommentCreatedEvent event) {
    try {
        logger.info("Publishing CommentCreatedEvent: {}", event);
        
        // ⭐ ĐOẠN CODE NÀY BẮN MESSAGE LÊN KAFKA
        CompletableFuture<SendResult<String, Object>> future = 
            kafkaTemplate.send(TOPIC_COMMENT_CREATED, event.getPostId().toString(), event);
        // ↑
        // Topic: "comment-created"
        // Key: postId (để messages cùng post đi vào cùng partition)
        // Value: event object (tự động serialize thành JSON)
        
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
    }
}
```

**Giải thích:**
- **Dòng 45-46:** `kafkaTemplate.send(TOPIC_COMMENT_CREATED, event.getPostId().toString(), event)`
  - → **Đây là đoạn code bắn message lên Kafka**
  - → Topic: `"comment-created"`
  - → Key: `postId` (String)
  - → Value: `CommentCreatedEvent` object (tự động serialize thành JSON)

---

## 🔄 Flow Hoàn Chỉnh

```
User B comment bài viết của User A
  ↓
CommentController.createComment()
  ↓
CommentServiceImpl.createComment()
  ├─ PostServiceClient.getPostAuthorId() → Lấy postAuthorId = User A
  ├─ commentRepository.save() → Lưu comment vào DB
  └─ commentEventProducer.publishCommentCreated(event) ← BẮN MESSAGE
      ↓
KafkaCommentEventProducer.publishCommentCreated()
  └─ kafkaTemplate.send("comment-created", postId, event) ← ĐOẠN CODE NÀY
      ↓
Kafka Broker
  └─ Topic: "comment-created"
      ↓
Notification Service (Consumer)
  └─ CommentEventConsumer.consumeCommentCreated()
      └─ Lưu notification vào database (KHÔNG gửi email)
```

---

## 📝 Tóm Tắt

**Đoạn code bắn message lên Kafka:**

1. **CommentServiceImpl.java (dòng 55):**
   ```java
   commentEventProducer.publishCommentCreated(event);
   ```

2. **KafkaCommentEventProducer.java (dòng 45-46):**
   ```java
   kafkaTemplate.send(TOPIC_COMMENT_CREATED, event.getPostId().toString(), event);
   ```

**Đây là 2 đoạn code chính publish event lên Kafka!**
