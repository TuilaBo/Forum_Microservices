# Comment Service - Roadmap Implementation

## 📋 Tổng Quan

Comment Service quản lý comments cho posts trong forum. Service này sẽ:
- CRUD operations cho comments
- Phân quyền (chỉ author mới được sửa/xóa comment của mình)
- Tích hợp với post-service (validate post tồn tại)
- Kafka events (CommentCreated, CommentUpdated, CommentDeleted)
- Notification khi có comment mới

---

## 🎯 Features Cần Implement

### 1. Core Features
- ✅ Create comment (với postId)
- ✅ Update comment (chỉ author)
- ✅ Delete comment (chỉ author)
- ✅ Get comment by ID
- ✅ Get all comments của một post (có phân trang)
- ✅ Get all comments của một user (có phân trang)

### 2. Security & Authorization
- ✅ Keycloak integration (giống post-service)
- ✅ Chỉ user đã đăng nhập mới được comment
- ✅ Chỉ author mới được sửa/xóa comment của mình

### 3. Kafka Integration
- ✅ Publish CommentCreatedEvent
- ✅ Publish CommentUpdatedEvent
- ✅ Publish CommentDeletedEvent
- ✅ Consumer có thể subscribe để xử lý (notification-service)

### 4. Database
- ✅ PostgreSQL database
- ✅ Comment entity với relationships
- ✅ JPA/Hibernate

---

## 🏗️ Architecture

### Database Schema

```sql
CREATE TABLE comments (
    id BIGSERIAL PRIMARY KEY,
    post_id BIGINT NOT NULL,
    author_id VARCHAR(255) NOT NULL,
    author_username VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    parent_comment_id BIGINT, -- For nested comments (optional)
    FOREIGN KEY (parent_comment_id) REFERENCES comments(id)
);
```

### Package Structure

```
comment-service/
├── src/main/java/com/khoavdse170395/commentservice/
│   ├── config/
│   │   ├── SecurityConfig.java
│   │   ├── KafkaConfig.java
│   │   ├── OpenApiConfig.java
│   │   └── JwtConverterConfig.java
│   ├── controller/
│   │   └── CommentController.java
│   ├── model/
│   │   ├── Comment.java (Entity)
│   │   ├── dto/
│   │   │   ├── CreateCommentRequest.java
│   │   │   ├── UpdateCommentRequest.java
│   │   │   └── CommentResponse.java
│   │   └── event/
│   │       ├── CommentCreatedEvent.java
│   │       ├── CommentUpdatedEvent.java
│   │       └── CommentDeletedEvent.java
│   ├── repository/
│   │   └── CommentRepository.java
│   ├── service/
│   │   ├── CommentService.java
│   │   └── impl/
│   │       └── CommentServiceImpl.java
│   ├── kafka/
│   │   ├── CommentEventProducer.java
│   │   └── impl/
│   │       └── KafkaCommentEventProducer.java
│   └── security/
│       └── JwtAuthConverter.java
```

---

## 📝 Implementation Steps

### Step 1: Database Setup
- [ ] Tạo database: `comment_forum_db`
- [ ] Config PostgreSQL trong `application.properties`
- [ ] Add JPA dependencies

### Step 2: Model & Repository
- [ ] Tạo `Comment` entity
- [ ] Tạo DTOs (CreateCommentRequest, UpdateCommentRequest, CommentResponse)
- [ ] Tạo `CommentRepository` interface

### Step 3: Service Layer
- [ ] Tạo `CommentService` interface
- [ ] Implement `CommentServiceImpl`
- [ ] Business logic: validation, authorization

### Step 4: Security
- [ ] Add Spring Security dependencies
- [ ] Config Keycloak (giống post-service)
- [ ] Tạo `JwtAuthConverter`
- [ ] Config `SecurityConfig`

### Step 5: Controller
- [ ] Tạo `CommentController`
- [ ] Implement CRUD APIs
- [ ] Add Swagger documentation

### Step 6: Kafka Integration
- [ ] Add Kafka dependencies
- [ ] Config `KafkaConfig` (Producer)
- [ ] Tạo Event classes
- [ ] Implement `CommentEventProducer`
- [ ] Publish events trong `CommentServiceImpl`

### Step 7: Testing
- [ ] Test APIs với Postman/curl
- [ ] Test Kafka events
- [ ] Test authorization

---

## 🔗 Dependencies Cần Thêm

### pom.xml

```xml
<!-- Database -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
</dependency>

<!-- Security -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
</dependency>

<!-- Kafka -->
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>

<!-- Swagger -->
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.3.0</version>
</dependency>
```

---

## 📡 API Endpoints

### Comment APIs

```
POST   /api/comments              - Create comment
GET    /api/comments/{id}         - Get comment by ID
PUT    /api/comments/{id}         - Update comment (author only)
DELETE /api/comments/{id}         - Delete comment (author only)
GET    /api/comments/post/{postId} - Get all comments của post (pagination)
GET    /api/comments/user/{userId} - Get all comments của user (pagination)
```

### Request/Response Examples

**Create Comment:**
```json
POST /api/comments
{
  "postId": 1,
  "content": "Great post! Thanks for sharing."
}
```

**Response:**
```json
{
  "id": 1,
  "postId": 1,
  "authorId": "user-123",
  "authorUsername": "john_doe",
  "content": "Great post! Thanks for sharing.",
  "createdAt": "2026-01-11T10:00:00",
  "updatedAt": null
}
```

---

## 🔄 Kafka Events

### Topics
- `comment-created` - Khi comment mới được tạo
- `comment-updated` - Khi comment được cập nhật
- `comment-deleted` - Khi comment bị xóa

### Event Structure

**CommentCreatedEvent:**
```json
{
  "commentId": 1,
  "postId": 1,
  "authorId": "user-123",
  "authorUsername": "john_doe",
  "content": "Great post!",
  "createdAt": "2026-01-11T10:00:00",
  "eventType": "CommentCreatedEvent",
  "eventTimestamp": "2026-01-11T10:00:00"
}
```

---

## 🔐 Security Rules

1. **Create Comment:**
   - ✅ Phải đăng nhập (`@PreAuthorize("isAuthenticated()")`)
   - ✅ Validate postId tồn tại (có thể gọi post-service hoặc cache)

2. **Update Comment:**
   - ✅ Phải đăng nhập
   - ✅ Chỉ author mới được sửa (`authorId == currentUserId`)

3. **Delete Comment:**
   - ✅ Phải đăng nhập
   - ✅ Chỉ author mới được xóa (`authorId == currentUserId`)

4. **Get Comments:**
   - ✅ Public (không cần đăng nhập)

---

## 🎯 Next Steps

1. **Setup Database:**
   ```sql
   CREATE DATABASE comment_forum_db;
   ```

2. **Update application.properties:**
   ```properties
   spring.datasource.url=jdbc:postgresql://localhost:5432/comment_forum_db
   spring.datasource.username=postgres
   spring.datasource.password=sa
   server.port=8083
   ```

3. **Follow Implementation Steps** (từ Step 1 → Step 7)

---

## 📊 Integration với Services Khác

### Post Service
- Validate postId tồn tại khi tạo comment
- Có thể gọi REST API hoặc dùng Kafka events

### Notification Service
- Subscribe `comment-created` topic
- Gửi email cho post author khi có comment mới

---

## ✅ Checklist

- [ ] Database setup
- [ ] Model & Repository
- [ ] Service layer
- [ ] Security (Keycloak)
- [ ] Controller & APIs
- [ ] Kafka integration
- [ ] Swagger documentation
- [ ] Testing

---

## 🚀 Ready to Start?

Bạn muốn tôi bắt đầu implement từ đâu?
1. Database setup + Model
2. Service layer
3. Controller + APIs
4. Security
5. Kafka integration

Hoặc implement tất cả theo thứ tự?
