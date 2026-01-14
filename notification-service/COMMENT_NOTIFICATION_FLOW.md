# Comment Notification Flow - Luồng Xử Lý Notification Khi Có Comment

## 📋 Tổng Quan

Khi một user comment vào bài viết của user khác, hệ thống sẽ tự động tạo notification cho chủ bài viết thông qua Kafka event-driven architecture.

## 🔄 Flow Hoàn Chỉnh

```
User B comment vào bài viết của User A
  ↓
CommentController.createComment() (comment-service)
  ↓
CommentServiceImpl.createComment()
  ├─ PostServiceClient.getPostAuthorId() → Lấy postAuthorId = User A
  ├─ commentRepository.save() → Lưu comment vào DB
  └─ commentEventProducer.publishCommentCreated(event) ← Publish event
      ↓
KafkaCommentEventProducer.publishCommentCreated()
  └─ kafkaTemplate.send("comment-created", postId, event)
      ↓
Kafka Broker
  └─ Topic: "comment-created"
      ↓
CommentEventConsumer.consumeCommentCreated() (notification-service)
  ├─ Kiểm tra: authorId == postAuthorId? → Skip notification
  └─ Nếu khác: createCommentNotification()
      ↓
NotificationServiceImpl.createCommentNotification()
  └─ Lưu notification vào database
      ↓
Database (notification_forum_db)
  └─ Notification table
```

## 🔍 Chi Tiết Từng Bước

### 1. Comment Service - Publish Event

**File:** `comment-service/src/main/java/com/khoavdse170395/commentservice/service/impl/CommentServiceImpl.java`

```java
@Override
public CommentResponse createComment(CreateCommentRequest request, String userId, String username) {
    // Lấy postAuthorId từ post-service
    String postAuthorId = postServiceClient.getPostAuthorId(request.getPostId());
    
    // Tạo comment
    Comment comment = new Comment();
    comment.setPostId(request.getPostId());
    comment.setContent(request.getContent());
    comment.setAuthorId(userId);  // User B (người comment)
    comment.setAuthorUsername(username);
    
    Comment savedComment = commentRepository.save(comment);
    
    // Publish CommentCreatedEvent lên Kafka
    CommentCreatedEvent event = new CommentCreatedEvent(
        savedComment.getId(),
        savedComment.getPostId(),
        savedComment.getContent(),
        savedComment.getAuthorId(),      // User B
        savedComment.getAuthorUsername(),
        postAuthorId,                    // User A (chủ bài viết)
        savedComment.getCreatedAt()
    );
    commentEventProducer.publishCommentCreated(event);
    
    return mapToResponse(savedComment);
}
```

**Kafka Producer:** `comment-service/src/main/java/com/khoavdse170395/commentservice/kafka/impl/KafkaCommentEventProducer.java`

```java
@Override
public void publishCommentCreated(CommentCreatedEvent event) {
    // Gửi message lên Kafka topic "comment-created"
    // Key: postId (để messages cùng post đi vào cùng partition)
    CompletableFuture<SendResult<String, Object>> future = 
        kafkaTemplate.send(TOPIC_COMMENT_CREATED, event.getPostId().toString(), event);
    
    // Callback để log kết quả
    future.whenComplete((result, exception) -> {
        if (exception == null) {
            logger.info("Successfully published CommentCreatedEvent to topic: {}", TOPIC_COMMENT_CREATED);
        } else {
            logger.error("Failed to publish CommentCreatedEvent", exception);
        }
    });
}
```

### 2. Notification Service - Consume Event

**File:** `notification-service/src/main/java/com/khoavdse170395/notificationservice/consumer/CommentEventConsumer.java`

```java
@KafkaListener(
    topics = "comment-created", 
    groupId = "notification-service-group",
    containerFactory = "kafkaListenerContainerFactory"
)
public void consumeCommentCreated(@Payload LinkedHashMap<String, Object> payload) {
    // Convert LinkedHashMap sang CommentCreatedEvent
    CommentCreatedEvent event = objectMapper.convertValue(payload, CommentCreatedEvent.class);
    
    // ⭐ KIỂM TRA: Không gửi notification nếu người comment chính là chủ bài viết
    if (event.getPostAuthorId() != null && !event.getPostAuthorId().isEmpty()) {
        // So sánh authorId (người comment) với postAuthorId (chủ bài viết)
        if (event.getAuthorId() != null && event.getAuthorId().equals(event.getPostAuthorId())) {
            logger.info("Skipping notification: User {} commented on their own post", event.getAuthorId());
            return; // Không tạo notification
        }
        
        // Tạo notification cho chủ bài viết
        notificationService.createCommentNotification(
            event.getPostAuthorId(),        // User A (chủ bài viết)
            event.getAuthorUsername(),      // User B (người comment)
            event.getPostId(),
            event.getCommentId(),
            event.getContent()
        );
    }
}
```

### 3. Notification Service - Tạo Notification

**File:** `notification-service/src/main/java/com/khoavdse170395/notificationservice/service/impl/NotificationServiceImpl.java`

```java
@Override
public void createCommentNotification(String postAuthorId, String commentAuthorUsername, 
                                     Long postId, Long commentId, String commentContent) {
    Notification notification = new Notification();
    notification.setUserId(postAuthorId); // Người nhận notification
    notification.setType("COMMENT_ON_POST");
    notification.setTitle("Có comment mới trên bài viết của bạn");
    notification.setMessage(String.format("%s đã comment vào bài viết của bạn: \"%s\"", 
        commentAuthorUsername, 
        commentContent.length() > 50 ? commentContent.substring(0, 50) + "..." : commentContent));
    notification.setRelatedPostId(postId);
    notification.setRelatedCommentId(commentId);
    notification.setIsRead(false);
    
    notificationRepository.save(notification);
}
```

## ✅ Logic Kiểm Tra

### Trường hợp 1: User khác comment vào bài viết của mình
- **Input:** `authorId = "user-b"`, `postAuthorId = "user-a"`
- **Kết quả:** ✅ Tạo notification cho `user-a`

### Trường hợp 2: User tự comment vào bài viết của mình
- **Input:** `authorId = "user-a"`, `postAuthorId = "user-a"`
- **Kết quả:** ❌ Skip notification (không tạo)

## 📊 Event Schema

**CommentCreatedEvent:**
```json
{
  "commentId": 123,
  "postId": 456,
  "content": "Great post!",
  "authorId": "user-b-id",
  "authorUsername": "userB",
  "postAuthorId": "user-a-id",
  "createdAt": "2026-01-13T20:00:00",
  "eventType": "CommentCreatedEvent",
  "eventTimestamp": "2026-01-13T20:00:00"
}
```

## 🔧 Kafka Configuration

**Topic:** `comment-created`
- **Producer:** `comment-service`
- **Consumer:** `notification-service` (groupId: `notification-service-group`)
- **Key:** `postId` (String) - để đảm bảo messages cùng post đi vào cùng partition
- **Value:** `CommentCreatedEvent` (JSON)

## 📝 Lưu Ý

1. **Không gửi email:** Notification chỉ được lưu vào database, không gửi email
2. **Async processing:** Kafka xử lý bất đồng bộ, không block API response
3. **Error handling:** Nếu Kafka fail, không ảnh hưởng đến việc tạo comment
4. **Self-comment check:** Tự động skip notification khi user comment vào bài viết của chính mình

## 🧪 Test Flow

1. **User A** tạo post
2. **User B** comment vào post của User A
3. Kiểm tra database: Có notification cho User A
4. **User A** tự comment vào post của mình
5. Kiểm tra database: Không có notification mới
