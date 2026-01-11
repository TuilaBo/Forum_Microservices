# Kafka Roadmap - Xây dựng Hệ thống Event-Driven

## ✅ Đã hoàn thành (Producer Side)

### 1. Kafka Infrastructure
- ✅ Kafka Broker đang chạy (port 9092)
- ✅ Zookeeper đang chạy (port 2181)
- ✅ Topics tự động tạo: `post-created`, `post-updated`, `post-deleted`

### 2. Post Service - Producer
- ✅ `KafkaConfig` - Cấu hình Kafka Producer
- ✅ `KafkaPostEventProducer` - Publish events lên Kafka
- ✅ `PostServiceImpl` - Publish events sau khi:
  - ✅ Create post → `PostCreatedEvent`
  - ✅ Update post → `PostUpdatedEvent`
  - ✅ Delete post → `PostDeletedEvent`

### 3. Events đã được publish thành công
- ✅ Events đã được lưu vào Kafka (offset 0, 1, ...)
- ✅ Messages có thể consume được

---

## 🎯 Bước tiếp theo: Xây dựng Consumer Services

### Architecture hiện tại:

```
┌─────────────┐
│ post-service│ (Producer)
│             │
│ Create Post │ → Publish PostCreatedEvent → Kafka
│ Update Post │ → Publish PostUpdatedEvent → Kafka
│ Delete Post │ → Publish PostDeletedEvent → Kafka
└─────────────┘
       │
       ↓
┌─────────────┐
│   Kafka     │
│             │
│ Topics:     │
│ - post-created│
│ - post-updated│
│ - post-deleted│
└─────────────┘
       │
       ↓
   (Chưa có Consumers)
```

### Architecture mục tiêu:

```
┌─────────────┐
│ post-service│ (Producer)
└──────┬──────┘
       │
       ↓
┌─────────────┐
│   Kafka     │
└──────┬──────┘
       │
       ├─────────────────┬──────────────────┬──────────────┐
       ↓                 ↓                  ↓              ↓
┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐
│notification │  │search-service│  │analytics-   │  │comment-     │
│-service     │  │             │  │service      │  │service      │
│(Consumer)   │  │(Consumer)    │  │(Consumer)   │  │(Consumer)   │
│             │  │             │  │             │  │             │
│- Gửi email  │  │- Index vào  │  │- Thống kê   │  │- Xóa comments│
│- Push notif │  │  Elasticsearch│ │- Dashboard │  │  khi post xóa│
└─────────────┘  └─────────────┘  └─────────────┘  └─────────────┘
```

---

## 📋 Roadmap chi tiết

### Phase 1: Notification Service (Ưu tiên cao) 🔔

**Mục đích:** Gửi thông báo khi có post mới/cập nhật

**Cần làm:**
1. Tạo `notification-service` (Spring Boot)
2. Setup Kafka Consumer
3. Subscribe topic `post-created`
4. Gửi email/push notification

**Use cases:**
- Gửi email cho moderator: "Có bài viết mới cần duyệt"
- Gửi notification cho followers của author
- Gửi email cho admin khi có post mới

**Tech stack:**
- Spring Boot
- Spring Kafka (Consumer)
- Email service (JavaMailSender hoặc SendGrid)
- (Optional) Push notification (Firebase Cloud Messaging)

---

### Phase 2: Search Service (Ưu tiên cao) 🔍

**Mục đích:** Index posts vào search engine để tìm kiếm nhanh

**Cần làm:**
1. Tạo `search-service` (Spring Boot)
2. Setup Kafka Consumer
3. Subscribe topics: `post-created`, `post-updated`, `post-deleted`
4. Index vào Elasticsearch hoặc Solr

**Use cases:**
- User search "Java tutorial" → Tìm thấy posts liên quan
- Full-text search trong content
- Filter by author, date, tags

**Tech stack:**
- Spring Boot
- Spring Kafka (Consumer)
- Elasticsearch hoặc Solr
- (Optional) Redis cache

---

### Phase 3: Analytics Service (Ưu tiên trung bình) 📊

**Mục đích:** Thống kê và phân tích dữ liệu

**Cần làm:**
1. Tạo `analytics-service` (Spring Boot)
2. Setup Kafka Consumer
3. Subscribe tất cả topics
4. Lưu metrics vào database hoặc time-series DB

**Use cases:**
- Số bài viết mỗi ngày/tuần/tháng
- Top authors
- Trending topics
- User engagement metrics
- Dashboard cho admin

**Tech stack:**
- Spring Boot
- Spring Kafka (Consumer)
- PostgreSQL hoặc InfluxDB (time-series)
- (Optional) Grafana cho visualization

---

### Phase 4: Comment Service (Ưu tiên trung bình) 💬

**Mục đích:** Quản lý comments, tự động xóa khi post bị xóa

**Cần làm:**
1. Tạo `comment-service` (Spring Boot)
2. Setup Kafka Consumer
3. Subscribe topic `post-deleted`
4. Xóa tất cả comments của post khi nhận event

**Use cases:**
- User comment vào post
- Khi post bị xóa → Tự động xóa tất cả comments
- Real-time comments (WebSocket)

**Tech stack:**
- Spring Boot
- Spring Kafka (Consumer)
- PostgreSQL
- (Optional) WebSocket cho real-time

---

### Phase 5: Cache Invalidation Service (Ưu tiên thấp) 🗄️

**Mục đích:** Xóa cache khi post được cập nhật

**Cần làm:**
1. Tạo `cache-service` hoặc tích hợp vào `api-gateway`
2. Setup Kafka Consumer
3. Subscribe topic `post-updated`
4. Xóa cache của post đó

**Use cases:**
- Cache post data trong Redis
- Khi post được update → Xóa cache
- Lần request tiếp theo sẽ lấy data mới từ DB

**Tech stack:**
- Spring Boot
- Spring Kafka (Consumer)
- Redis

---

## 🛠️ Implementation Guide

### Bước 1: Tạo Notification Service (Ví dụ)

#### 1.1. Tạo project structure

```
notification-service/
├── src/main/java/com/khoavdse170395/notificationservice/
│   ├── NotificationServiceApplication.java
│   ├── config/
│   │   └── KafkaConsumerConfig.java
│   ├── consumer/
│   │   └── PostEventConsumer.java
│   ├── service/
│   │   ├── EmailService.java
│   │   └── impl/
│   │       └── EmailServiceImpl.java
│   └── model/
│       └── PostCreatedEvent.java (shared với post-service)
```

#### 1.2. Dependencies (pom.xml)

```xml
<dependencies>
    <!-- Spring Boot Web -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    
    <!-- Spring Kafka -->
    <dependency>
        <groupId>org.springframework.kafka</groupId>
        <artifactId>spring-kafka</artifactId>
    </dependency>
    
    <!-- Email -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-mail</artifactId>
    </dependency>
</dependencies>
```

#### 1.3. Kafka Consumer Config

```java
@Configuration
@EnableKafka
public class KafkaConsumerConfig {
    
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;
    
    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "notification-service-group");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        return new DefaultKafkaConsumerFactory<>(props);
    }
    
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory = 
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        return factory;
    }
}
```

#### 1.4. Consumer Implementation

```java
@Component
public class PostEventConsumer {
    
    @Autowired
    private EmailService emailService;
    
    @KafkaListener(topics = "post-created", groupId = "notification-service-group")
    public void consumePostCreated(PostCreatedEvent event) {
        System.out.println("Received PostCreatedEvent: " + event);
        
        // Gửi email cho moderator
        emailService.sendEmailToModerator(
            "Có bài viết mới cần duyệt",
            "Post ID: " + event.getPostId() + 
            "\nTitle: " + event.getTitle() + 
            "\nAuthor: " + event.getAuthorUsername()
        );
        
        // Gửi notification cho followers (nếu có)
        // ...
    }
    
    @KafkaListener(topics = "post-updated", groupId = "notification-service-group")
    public void consumePostUpdated(PostUpdatedEvent event) {
        // Xử lý post updated
    }
    
    @KafkaListener(topics = "post-deleted", groupId = "notification-service-group")
    public void consumePostDeleted(PostDeletedEvent event) {
        // Xử lý post deleted
    }
}
```

#### 1.5. Application Properties

```properties
spring.application.name=notification-service
server.port=8083

# Kafka Configuration
spring.kafka.bootstrap-servers=localhost:9092
spring.kafka.consumer.group-id=notification-service-group
spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer
spring.kafka.consumer.value-deserializer=org.springframework.kafka.support.serializer.JsonDeserializer
spring.kafka.consumer.properties.spring.json.trusted.packages=*

# Email Configuration
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your-email@gmail.com
spring.mail.password=your-password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
```

---

## 📊 Priority Matrix

| Service | Priority | Complexity | Impact | Timeline |
|---------|----------|------------|--------|----------|
| **Notification Service** | 🔴 High | Medium | High | 1-2 days |
| **Search Service** | 🔴 High | High | High | 3-5 days |
| **Analytics Service** | 🟡 Medium | Medium | Medium | 2-3 days |
| **Comment Service** | 🟡 Medium | Medium | Medium | 2-3 days |
| **Cache Service** | 🟢 Low | Low | Low | 1 day |

---

## 🎯 Quick Start: Notification Service

### Step 1: Tạo project

```bash
# Sử dụng Spring Initializr hoặc copy từ post-service
# Dependencies: Web, Kafka, Mail
```

### Step 2: Copy Event classes

```bash
# Copy PostCreatedEvent, PostUpdatedEvent, PostDeletedEvent
# từ post-service sang notification-service
```

### Step 3: Implement Consumer

```java
@KafkaListener(topics = "post-created")
public void handlePostCreated(PostCreatedEvent event) {
    // Gửi email
}
```

### Step 4: Test

```bash
# 1. Start notification-service
# 2. Tạo post từ post-service
# 3. Xem email được gửi
```

---

## 🔄 Event Flow Example

### Scenario: User tạo post mới

```
1. User → POST /posts
   ↓
2. post-service → Save vào PostgreSQL
   ↓
3. post-service → Publish PostCreatedEvent → Kafka
   ↓
4. Kafka lưu message vào topic "post-created"
   ↓
5. Consumers nhận event (song song):
   ├─ notification-service → Gửi email cho moderator
   ├─ search-service → Index vào Elasticsearch
   ├─ analytics-service → Update statistics
   └─ comment-service → (Không làm gì, chờ user comment)
```

---

## 📝 Best Practices

### 1. Error Handling

```java
@KafkaListener(topics = "post-created")
public void consumePostCreated(PostCreatedEvent event) {
    try {
        // Process event
    } catch (Exception e) {
        // Log error
        // Send to Dead Letter Queue (DLQ)
        // Hoặc retry với exponential backoff
    }
}
```

### 2. Idempotency

```java
// Đảm bảo xử lý event nhiều lần vẫn cho kết quả giống nhau
@KafkaListener(topics = "post-created")
public void consumePostCreated(PostCreatedEvent event) {
    // Kiểm tra đã xử lý event này chưa (dùng database)
    if (alreadyProcessed(event.getPostId())) {
        return; // Skip
    }
    
    // Process event
    markAsProcessed(event.getPostId());
}
```

### 3. Consumer Groups

```java
// Mỗi service có consumer group riêng
spring.kafka.consumer.group-id=notification-service-group
spring.kafka.consumer.group-id=search-service-group
spring.kafka.consumer.group-id=analytics-service-group
```

### 4. Monitoring

- Monitor consumer lag (messages chưa được xử lý)
- Monitor processing time
- Monitor error rate
- Alert khi consumer down

---

## 🎓 Learning Path

### Beginner
1. ✅ Producer (đã làm)
2. ⏭️ Consumer (bước tiếp theo)
3. ⏭️ Error handling
4. ⏭️ Testing

### Intermediate
1. ⏭️ Consumer groups
2. ⏭️ Partitioning
3. ⏭️ Idempotency
4. ⏭️ Dead Letter Queue

### Advanced
1. ⏭️ Exactly-once semantics
2. ⏭️ Schema registry (Avro)
3. ⏭️ Kafka Streams
4. ⏭️ Kafka Connect

---

## 🚀 Next Steps

### Immediate (Hôm nay):
1. ✅ Verify Kafka đang hoạt động
2. ⏭️ Tạo `notification-service` với Kafka Consumer
3. ⏭️ Test: Tạo post → Xem email được gửi

### Short-term (Tuần này):
1. ⏭️ Hoàn thiện notification-service
2. ⏭️ Tạo search-service với Elasticsearch
3. ⏭️ Test end-to-end flow

### Long-term (Tháng này):
1. ⏭️ Analytics service
2. ⏭️ Comment service
3. ⏭️ Monitoring và alerting
4. ⏭️ Performance optimization

---

## 📚 Resources

- [Spring Kafka Documentation](https://spring.io/projects/spring-kafka)
- [Kafka Consumer Best Practices](https://kafka.apache.org/documentation/#consumerconfigs)
- [Event-Driven Architecture Patterns](https://martinfowler.com/articles/201701-event-driven.html)

---

## ✅ Checklist

### Producer Side (Đã hoàn thành)
- [x] Kafka Broker setup
- [x] Kafka Producer config
- [x] Event classes
- [x] Publish events (create, update, delete)
- [x] Verify events trong Kafka

### Consumer Side (Cần làm)
- [ ] Notification service
- [ ] Search service
- [ ] Analytics service
- [ ] Comment service
- [ ] Error handling
- [ ] Monitoring

---

**Bước tiếp theo:** Bắt đầu với **Notification Service** - đơn giản nhất và có impact cao nhất!
