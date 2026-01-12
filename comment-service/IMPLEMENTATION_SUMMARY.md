# Comment Service - Implementation Summary

## ✅ Đã Hoàn Thành

### 1. Database Setup
- ✅ Database: `comment_forum_db`
- ✅ User: `postgres`
- ✅ Password: `sa`
- ✅ Port: `8083`

### 2. Model & Repository
- ✅ `Comment` entity với JPA annotations
- ✅ `CreateCommentRequest` DTO
- ✅ `UpdateCommentRequest` DTO
- ✅ `CommentResponse` DTO
- ✅ `CommentCreatedEvent` (cho Kafka)
- ✅ `CommentRepository` với methods:
  - `findByPostId()` - Lấy comments của post
  - `findByAuthorId()` - Lấy comments của user

### 3. Service Layer
- ✅ `CommentService` interface
- ✅ `CommentServiceImpl` với business logic:
  - `createComment()` - Tạo comment + publish Kafka event
  - `updateComment()` - Sửa comment (author only, KHÔNG publish event)
  - `deleteComment()` - Xóa comment (author only, KHÔNG publish event)
  - `getCommentById()` - Lấy comment theo ID
  - `getCommentsByPost()` - Lấy comments của post (pagination)
  - `getCommentsByAuthor()` - Lấy comments của user (pagination)
- ✅ `PostServiceClient` - Gọi post-service API để lấy postAuthorId

### 4. Controller
- ✅ `CommentController` với các endpoints:
  - `POST /comments` - Tạo comment (cần auth)
  - `PUT /comments/{id}` - Sửa comment (author only)
  - `DELETE /comments/{id}` - Xóa comment (author only)
  - `GET /comments/{id}` - Lấy comment (public)
  - `GET /comments/post/{postId}` - Lấy comments của post (public, pagination)
  - `GET /comments/user/{authorId}` - Lấy comments của user (public, pagination)

### 5. Security
- ✅ `SecurityConfig` - Keycloak integration
- ✅ `JwtAuthConverter` - Map roles từ Keycloak
- ✅ `JwtConverterConfig` - Bean configuration
- ✅ Phân quyền:
  - Chỉ đăng nhập mới được tạo comment
  - Chỉ author mới được sửa/xóa comment

### 6. Kafka Integration
- ✅ `KafkaConfig` - Producer configuration
- ✅ `CommentEventProducer` interface
- ✅ `KafkaCommentEventProducer` implementation
- ✅ **CHỈ publish event khi tạo comment mới** (không publish khi update/delete)
- ✅ Topic: `comment-created`
- ✅ Event chứa `postAuthorId` để notification-service gửi email

### 7. Swagger
- ✅ `OpenApiConfig` - Swagger UI configuration
- ✅ Access tại: `http://localhost:8083/swagger-ui.html`

---

## 📋 API Endpoints

### Create Comment
```http
POST /comments
Authorization: Bearer {token}
Content-Type: application/json

{
  "postId": 1,
  "content": "Great post! Thanks for sharing."
}
```

### Update Comment
```http
PUT /comments/{id}
Authorization: Bearer {token}
Content-Type: application/json

{
  "content": "Updated comment content"
}
```

### Delete Comment
```http
DELETE /comments/{id}
Authorization: Bearer {token}
```

### Get Comment by ID
```http
GET /comments/{id}
```

### Get Comments by Post
```http
GET /comments/post/{postId}?page=0&size=10&sortBy=createdAt&sortDir=DESC
```

### Get Comments by User
```http
GET /comments/user/{authorId}?page=0&size=10&sortBy=createdAt&sortDir=DESC
```

---

## 🔄 Kafka Event Flow

### CommentCreatedEvent
**Topic:** `comment-created`

**Khi nào publish:**
- ✅ Khi user tạo comment mới
- ❌ KHÔNG publish khi update comment
- ❌ KHÔNG publish khi delete comment

**Event Structure:**
```json
{
  "commentId": 1,
  "postId": 1,
  "content": "Great post!",
  "authorId": "user-b-id",
  "authorUsername": "userB",
  "postAuthorId": "user-a-id",  // ← Để gửi notification cho user A
  "createdAt": "2026-01-11T10:00:00",
  "eventType": "CommentCreatedEvent",
  "eventTimestamp": "2026-01-11T10:00:00"
}
```

**Flow:**
```
User B comment bài viết của User A
  ↓
CommentServiceImpl.createComment()
  ↓
PostServiceClient.getPostAuthorId() → Gọi post-service API
  ↓
CommentCreatedEvent (với postAuthorId = User A)
  ↓
KafkaProducer.publishCommentCreated()
  ↓
Kafka Topic: "comment-created"
  ↓
Notification Service (subscribe)
  ↓
Gửi email cho User A: "User B đã comment bài viết của bạn"
```

---

## 🗄️ Database Schema

```sql
CREATE TABLE comments (
    id BIGSERIAL PRIMARY KEY,
    post_id BIGINT NOT NULL,
    author_id VARCHAR(255) NOT NULL,
    author_username VARCHAR(255),
    content TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);
```

---

## 🚀 Setup & Run

### 1. Tạo Database
```sql
CREATE DATABASE comment_forum_db;
```

### 2. Update application.properties
Đã config sẵn:
- Database: `comment_forum_db`
- Port: `8083`
- Kafka: `localhost:9092`
- Post Service URL: `http://localhost:8082`

### 3. Run Service
```bash
cd comment-service
mvn clean install
mvn spring-boot:run
```

### 4. Access Swagger
```
http://localhost:8083/swagger-ui.html
```

---

## 📝 Next Steps

### Để notification-service nhận CommentCreatedEvent:

1. **Thêm consumer vào notification-service:**
   - Tạo `CommentEventConsumer`
   - Subscribe topic `comment-created`
   - Gửi email cho `postAuthorId`

2. **Test Flow:**
   - User A tạo post
   - User B comment post của User A
   - Kiểm tra email gửi cho User A

---

## ✅ Checklist

- [x] Database setup
- [x] Model & Repository
- [x] Service layer
- [x] Controller & APIs
- [x] Security (Keycloak)
- [x] Kafka Producer (chỉ CommentCreatedEvent)
- [x] Swagger documentation
- [ ] Test APIs
- [ ] Test Kafka events
- [ ] Integrate với notification-service

---

## 🎯 Key Points

1. **Kafka chỉ publish khi tạo comment mới** (theo yêu cầu)
2. **Event chứa postAuthorId** để notification-service biết gửi email cho ai
3. **PostServiceClient** gọi REST API đến post-service để lấy postAuthorId
4. **Phân quyền:** Chỉ author mới được sửa/xóa comment
5. **Public endpoints:** GET comments không cần auth
