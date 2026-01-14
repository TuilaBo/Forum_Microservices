# Monolith vs Microservices - So Sánh Cách Xử Lý Notification

## 🏗️ Kiến Trúc Monolith

### Cách Xử Lý Trong Monolith

Trong monolith, **KHÔNG CẦN Kafka**. Có thể gọi trực tiếp method hoặc qua database.

#### Cách 1: Gọi Trực Tiếp Method (Synchronous)

```java
// Trong CommentService (monolith)
@Service
public class CommentService {
    
    @Autowired
    private NotificationService notificationService; // Cùng application
    
    public CommentResponse createComment(CreateCommentRequest request, String userId, String username) {
        // Tạo comment
        Comment comment = new Comment();
        comment.setPostId(request.getPostId());
        comment.setContent(request.getContent());
        comment.setAuthorId(userId);
        comment.setAuthorUsername(username);
        
        Comment savedComment = commentRepository.save(comment);
        
        // ✅ GỌI TRỰC TIẾP METHOD - Cùng JVM, không cần network
        String postAuthorId = postService.getPostAuthorId(request.getPostId());
        
        // ✅ GỌI TRỰC TIẾP - Không cần Kafka
        if (!userId.equals(postAuthorId)) {
            notificationService.createCommentNotification(
                postAuthorId,
                username,
                savedComment.getPostId(),
                savedComment.getId(),
                savedComment.getContent()
            );
        }
        
        return mapToResponse(savedComment);
    }
}
```

**Đặc điểm:**
- ✅ Đơn giản, không cần Kafka
- ✅ Synchronous - đợi notification được tạo xong
- ✅ Cùng database, cùng transaction (có thể rollback)
- ❌ Tight coupling - services phụ thuộc nhau
- ❌ Nếu notification-service chậm → comment API chậm theo

#### Cách 2: Gọi Qua Database (Event Sourcing Pattern)

```java
// Trong CommentService (monolith)
@Service
public class CommentService {
    
    public CommentResponse createComment(CreateCommentRequest request, String userId, String username) {
        // Tạo comment
        Comment comment = new Comment();
        // ... set fields
        
        Comment savedComment = commentRepository.save(comment);
        
        // ✅ LƯU EVENT VÀO DATABASE (bảng events hoặc notifications)
        NotificationEvent event = new NotificationEvent();
        event.setType("COMMENT_CREATED");
        event.setPostAuthorId(postAuthorId);
        event.setCommentId(savedComment.getId());
        // ... set các fields khác
        
        notificationEventRepository.save(event); // Lưu vào cùng database
        
        return mapToResponse(savedComment);
    }
}

// Background job hoặc scheduled task đọc events và xử lý
@Component
public class NotificationProcessor {
    
    @Scheduled(fixedDelay = 5000) // Chạy mỗi 5 giây
    public void processNotifications() {
        List<NotificationEvent> events = notificationEventRepository.findUnprocessed();
        
        for (NotificationEvent event : events) {
            notificationService.createCommentNotification(...);
            event.setProcessed(true);
            notificationEventRepository.save(event);
        }
    }
}
```

**Đặc điểm:**
- ✅ Decoupled - comment không phụ thuộc notification
- ✅ Async processing
- ✅ Cùng database - dễ query, transaction
- ❌ Cần background job/scheduler
- ❌ Delay trong xử lý (không real-time)

## 🔀 Kiến Trúc Microservices (Hiện Tại)

### Cách Xử Lý Trong Microservices

Trong microservices, **CẦN Kafka** để decouple các services.

```java
// comment-service (Service riêng biệt)
@Service
public class CommentServiceImpl {
    
    @Autowired
    private CommentEventProducer commentEventProducer; // Kafka Producer
    
    public CommentResponse createComment(...) {
        // Tạo comment
        Comment savedComment = commentRepository.save(comment);
        
        // ✅ GỌI POST-SERVICE QUA HTTP để lấy postAuthorId
        String postAuthorId = postServiceClient.getPostAuthorId(request.getPostId());
        
        // ✅ GỬI EVENT LÊN KAFKA (không gọi trực tiếp notification-service)
        CommentCreatedEvent event = new CommentCreatedEvent(...);
        commentEventProducer.publishCommentCreated(event); // Async, không block
        
        return mapToResponse(savedComment);
    }
}

// notification-service (Service riêng biệt)
@Component
public class CommentEventConsumer {
    
    @KafkaListener(topics = "comment-created")
    public void consumeCommentCreated(@Payload CommentCreatedEvent event) {
        // ✅ NHẬN EVENT TỪ KAFKA
        notificationService.createCommentNotification(...);
    }
}
```

**Đặc điểm:**
- ✅ Decoupled - services độc lập hoàn toàn
- ✅ Async - không block comment API
- ✅ Scalable - có thể scale từng service riêng
- ✅ Fault tolerant - nếu notification-service down, comment vẫn hoạt động
- ❌ Phức tạp hơn - cần Kafka infrastructure
- ❌ Network overhead - gọi HTTP giữa services

## 📊 So Sánh Chi Tiết

| Tiêu Chí | Monolith (Gọi Trực Tiếp) | Monolith (Database Events) | Microservices (Kafka) |
|----------|---------------------------|----------------------------|----------------------|
| **Độ phức tạp** | ⭐ Đơn giản | ⭐⭐ Trung bình | ⭐⭐⭐ Phức tạp |
| **Coupling** | ❌ Tight coupling | ✅ Loose coupling | ✅ Loose coupling |
| **Performance** | ⚠️ Blocking | ✅ Async | ✅ Async |
| **Scalability** | ❌ Scale cả app | ❌ Scale cả app | ✅ Scale từng service |
| **Fault Tolerance** | ❌ Nếu notification fail → comment fail | ✅ Nếu notification fail → comment vẫn OK | ✅ Nếu notification-service down → comment vẫn OK |
| **Infrastructure** | ✅ Không cần thêm | ✅ Chỉ cần database | ❌ Cần Kafka |
| **Network Calls** | ✅ Không có (cùng JVM) | ✅ Không có (cùng DB) | ❌ Có (HTTP + Kafka) |
| **Transaction** | ✅ Cùng transaction | ✅ Cùng database | ❌ Distributed transaction |
| **Real-time** | ✅ Ngay lập tức | ⚠️ Có delay (scheduled) | ✅ Ngay lập tức (async) |

## 🔄 Flow So Sánh

### Monolith - Gọi Trực Tiếp

```
User comment
  ↓
CommentService.createComment()
  ├─ Save comment to DB
  ├─ Call PostService.getPostAuthorId() → Direct method call
  └─ Call NotificationService.createCommentNotification() → Direct method call
      ↓
  Notification saved to DB
  ↓
Return response to user
```

**Thời gian:** ~50-100ms (tất cả synchronous)

### Monolith - Database Events

```
User comment
  ↓
CommentService.createComment()
  ├─ Save comment to DB
  └─ Save notification_event to DB
      ↓
Return response to user (ngay lập tức)
  ↓
Background Job (chạy mỗi 5 giây)
  ├─ Read unprocessed events
  └─ Process notifications
```

**Thời gian:** Response ngay (~20ms), notification sau 0-5 giây

### Microservices - Kafka

```
User comment
  ↓
CommentService.createComment() (comment-service)
  ├─ Save comment to DB
  ├─ Call PostService.getPostAuthorId() → HTTP call
  └─ Publish event to Kafka → Async
      ↓
Return response to user (ngay lập tức)
  ↓
Kafka Broker
  ↓
CommentEventConsumer (notification-service)
  └─ Process notification
```

**Thời gian:** Response ngay (~30-50ms), notification sau ~10-50ms

## 💡 Khi Nào Dùng Cái Nào?

### Monolith - Gọi Trực Tiếp
- ✅ Ứng dụng nhỏ, đơn giản
- ✅ Cần transaction consistency
- ✅ Team nhỏ
- ✅ Không cần scale riêng từng phần

### Monolith - Database Events
- ✅ Ứng dụng vừa, cần decoupling
- ✅ Không muốn setup Kafka
- ✅ Chấp nhận delay nhỏ trong notification
- ✅ Cần async processing nhưng không muốn phức tạp

### Microservices - Kafka
- ✅ Ứng dụng lớn, nhiều teams
- ✅ Cần scale từng service riêng
- ✅ Services độc lập về công nghệ
- ✅ Cần high availability
- ✅ Có infrastructure để quản lý Kafka

## 🎯 Kết Luận

**Trong Monolith:**
- ✅ **KHÔNG CẦN Kafka**
- ✅ Có thể gọi trực tiếp method (synchronous)
- ✅ Hoặc lưu event vào database và xử lý sau (async)

**Trong Microservices (hiện tại):**
- ✅ **CẦN Kafka** để decouple services
- ✅ Không thể gọi trực tiếp vì services ở các JVM/container khác nhau
- ✅ Kafka là cách tốt nhất để communication giữa services

**Tóm lại:** Đúng - trong monolith phải gọi API/method riêng, nhưng không cần Kafka vì có thể gọi trực tiếp trong cùng application.
