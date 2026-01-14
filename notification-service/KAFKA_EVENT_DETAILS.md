# Kafka Event Details - Thông Tin Được Gửi Qua Kafka

## ✅ Đúng - Kafka Gửi TẤT CẢ Thông Tin Cần Thiết

Khi một user comment vào bài viết, **Kafka gửi một event duy nhất** chứa **TẤT CẢ** thông tin cần thiết để notification-service xử lý mà **KHÔNG CẦN** gọi thêm API nào khác.

## 📦 CommentCreatedEvent - Tất Cả Thông Tin Trong 1 Event

### Event Structure

```json
{
  "commentId": 123,
  "postId": 456,
  "content": "Great post! Thanks for sharing.",
  "authorId": "user-b-uuid",
  "authorUsername": "student2",
  "postAuthorId": "user-a-uuid",
  "createdAt": "2026-01-13T20:30:00",
  "eventType": "CommentCreatedEvent",
  "eventTimestamp": "2026-01-13T20:30:00"
}
```

### Chi Tiết Từng Field

| Field | Mô Tả | Nguồn | Cần Cho Notification? |
|-------|-------|-------|----------------------|
| `commentId` | ID của comment vừa tạo | comment-service DB | ✅ Có (để link đến comment) |
| `postId` | ID của bài viết | Request từ client | ✅ Có (để link đến post) |
| `content` | Nội dung comment | Request từ client | ✅ Có (hiển thị trong notification) |
| `authorId` | ID của người comment | JWT token | ✅ Có (để check self-comment) |
| `authorUsername` | Username của người comment | JWT token | ✅ Có (hiển thị trong notification) |
| `postAuthorId` | ID của chủ bài viết | **Lấy từ post-service** | ✅ Có (người nhận notification) |
| `createdAt` | Thời gian tạo comment | comment-service DB | ✅ Có (timestamp) |
| `eventType` | Loại event | Hardcoded | ℹ️ Metadata |
| `eventTimestamp` | Thời gian event được tạo | System time | ℹ️ Metadata |

## 🔄 Flow: Tất Cả Thông Tin Được Thu Thập Trước Khi Gửi

```
1. User B comment vào bài viết của User A
   ↓
2. CommentServiceImpl.createComment()
   ├─ Lấy authorId, authorUsername từ JWT token ✅
   ├─ Lấy postId, content từ request ✅
   ├─ Lưu comment vào DB → có commentId, createdAt ✅
   └─ Gọi PostServiceClient.getPostAuthorId() → có postAuthorId ✅
   ↓
3. Tạo CommentCreatedEvent với TẤT CẢ thông tin ✅
   ↓
4. Gửi lên Kafka (1 message duy nhất)
   ↓
5. Notification-service nhận event
   └─ Có ĐỦ thông tin để tạo notification
   └─ KHÔNG CẦN gọi thêm API nào
```

## 💡 Tại Sao Thiết Kế Như Vậy?

### ✅ Ưu Điểm

1. **Self-contained Event**: Event chứa đầy đủ thông tin, không cần query thêm
2. **Decoupled**: notification-service không phụ thuộc vào comment-service hay post-service
3. **Performance**: Không cần thêm HTTP calls sau khi nhận event
4. **Reliability**: Nếu post-service down sau khi event đã gửi, vẫn có thể xử lý notification

### 📝 Lưu Ý

- `postAuthorId` được lấy từ **post-service** TRƯỚC KHI gửi event
- Nếu post-service không available khi tạo comment → event sẽ không có `postAuthorId` → notification sẽ bị skip
- Đây là trade-off: đảm bảo event self-contained nhưng phụ thuộc vào post-service khi tạo comment

## 🔍 Code Thực Tế

### CommentServiceImpl - Thu Thập Tất Cả Thông Tin

```java
@Override
public CommentResponse createComment(CreateCommentRequest request, String userId, String username) {
    // 1. Lấy postAuthorId từ post-service (TRƯỚC KHI gửi event)
    String postAuthorId = postServiceClient.getPostAuthorId(request.getPostId());
    
    // 2. Tạo comment với thông tin từ request và JWT
    Comment comment = new Comment();
    comment.setPostId(request.getPostId());      // Từ request
    comment.setContent(request.getContent());     // Từ request
    comment.setAuthorId(userId);                  // Từ JWT
    comment.setAuthorUsername(username);          // Từ JWT
    
    Comment savedComment = commentRepository.save(comment);
    // → Có commentId và createdAt từ DB
    
    // 3. Tạo event với TẤT CẢ thông tin đã thu thập
    CommentCreatedEvent event = new CommentCreatedEvent(
        savedComment.getId(),           // ✅ commentId
        savedComment.getPostId(),       // ✅ postId
        savedComment.getContent(),       // ✅ content
        savedComment.getAuthorId(),     // ✅ authorId
        savedComment.getAuthorUsername(), // ✅ authorUsername
        postAuthorId,                   // ✅ postAuthorId (từ post-service)
        savedComment.getCreatedAt()     // ✅ createdAt
    );
    
    // 4. Gửi 1 event duy nhất chứa TẤT CẢ thông tin
    commentEventProducer.publishCommentCreated(event);
    
    return mapToResponse(savedComment);
}
```

### CommentEventConsumer - Nhận Và Xử Lý Ngay

```java
@KafkaListener(topics = "comment-created", groupId = "notification-service-group")
public void consumeCommentCreated(@Payload LinkedHashMap<String, Object> payload) {
    CommentCreatedEvent event = objectMapper.convertValue(payload, CommentCreatedEvent.class);
    
    // ✅ Có TẤT CẢ thông tin trong event, không cần gọi API nào khác
    
    // Check self-comment
    if (event.getAuthorId().equals(event.getPostAuthorId())) {
        return; // Skip
    }
    
    // Tạo notification với thông tin từ event
    notificationService.createCommentNotification(
        event.getPostAuthorId(),    // ✅ Từ event
        event.getAuthorUsername(),   // ✅ Từ event
        event.getPostId(),           // ✅ Từ event
        event.getCommentId(),        // ✅ Từ event
        event.getContent()           // ✅ Từ event
    );
    
    // KHÔNG CẦN gọi:
    // - comment-service để lấy thông tin comment
    // - post-service để lấy thông tin post
    // - user-service để lấy thông tin user
}
```

## 📊 So Sánh: Self-Contained vs. Minimal Event

### ❌ Cách Không Tốt (Minimal Event)
```json
{
  "commentId": 123,
  "postId": 456
}
```
→ Notification-service phải gọi thêm:
- `GET /comments/123` để lấy content, authorId, authorUsername
- `GET /posts/456` để lấy postAuthorId
- → Nhiều HTTP calls, chậm, phụ thuộc nhiều services

### ✅ Cách Hiện Tại (Self-Contained Event)
```json
{
  "commentId": 123,
  "postId": 456,
  "content": "...",
  "authorId": "...",
  "authorUsername": "...",
  "postAuthorId": "...",
  "createdAt": "..."
}
```
→ Notification-service chỉ cần:
- Nhận event
- Parse JSON
- Tạo notification
- → Nhanh, không phụ thuộc services khác

## ✅ Kết Luận

**Đúng - Kafka gửi TẤT CẢ thông tin cần thiết trong một event duy nhất.**

Notification-service **KHÔNG CẦN** gọi thêm API nào sau khi nhận event. Tất cả thông tin đã được thu thập và đóng gói trong `CommentCreatedEvent` trước khi gửi lên Kafka.
