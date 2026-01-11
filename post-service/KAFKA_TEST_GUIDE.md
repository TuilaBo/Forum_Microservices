# Hướng dẫn Test Kafka trong Post Service

## 1. Start Kafka (Docker - Cách nhanh nhất)

```bash
# Tạo docker-compose.yml
version: '3.8'
services:
  zookeeper:
    image: confluentinc/cp-zookeeper:latest
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
      ZOOKEEPER_TICK_TIME: 2000
    ports:
      - "2181:2181"

  kafka:
    image: confluentinc/cp-kafka:latest
    depends_on:
      - zookeeper
    ports:
      - "9092:9092"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
```

Chạy:
```bash
docker-compose up -d
```

## 2. Kiểm tra Kafka đã chạy

```bash
# Kiểm tra containers
docker ps

# Xem logs
docker logs kafka
```

## 3. Test bằng Kafka Console Consumer (Xem messages real-time)

Mở terminal mới:

```bash
# Consumer cho topic "post-created"
docker exec -it kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic post-created \
  --from-beginning

# Consumer cho topic "post-updated"
docker exec -it kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic post-updated \
  --from-beginning

# Consumer cho topic "post-deleted"
docker exec -it kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic post-deleted \
  --from-beginning
```

## 4. Test API và xem Kafka messages

### A. Tạo Post (sẽ publish PostCreatedEvent)

```bash
# 1. Login để lấy token
POST http://localhost:8088/auth/login
Content-Type: application/json

{
  "username": "student1",
  "password": "password"
}

# Response: Copy access_token

# 2. Tạo post
POST http://localhost:8082/posts
Authorization: Bearer {access_token}
Content-Type: application/json

{
  "title": "Test Kafka Post",
  "content": "This post will trigger PostCreatedEvent"
}
```

**Kết quả mong đợi:**
- Terminal consumer sẽ hiển thị JSON message:
```json
{
  "postId": 1,
  "title": "Test Kafka Post",
  "content": "This post will trigger PostCreatedEvent",
  "authorId": "...",
  "authorUsername": "student1",
  "createdAt": "2026-01-10T10:30:00",
  "eventType": "PostCreatedEvent",
  "eventTimestamp": "2026-01-10T10:30:00"
```

- Logs trong post-service:
```
INFO  KafkaPostEventProducer - Publishing PostCreatedEvent: PostCreatedEvent{postId=1, title='Test Kafka Post', ...}
INFO  KafkaPostEventProducer - Successfully published PostCreatedEvent to topic: post-created, offset: 0
```

### B. Update Post (sẽ publish PostUpdatedEvent)

```bash
PUT http://localhost:8082/posts/1
Authorization: Bearer {access_token}
Content-Type: application/json

{
  "title": "Updated Title",
  "content": "Updated Content"
}
```

**Kết quả mong đợi:**
- Consumer "post-updated" sẽ nhận message
- Logs: "Successfully published PostUpdatedEvent"

### C. Delete Post (sẽ publish PostDeletedEvent)

```bash
DELETE http://localhost:8082/posts/1
Authorization: Bearer {access_token}
```

**Kết quả mong đợi:**
- Consumer "post-deleted" sẽ nhận message
- Logs: "Successfully published PostDeletedEvent"

## 5. Kiểm tra Topics và Messages

```bash
# List tất cả topics
docker exec -it kafka kafka-topics --list --bootstrap-server localhost:9092

# Xem thông tin topic
docker exec -it kafka kafka-topics --describe --bootstrap-server localhost:9092 --topic post-created

# Đếm số messages trong topic
docker exec -it kafka kafka-run-class kafka.tools.GetOffsetShell \
  --broker-list localhost:9092 \
  --topic post-created
```

## 6. Xem Logs trong Post Service

Trong console của post-service, bạn sẽ thấy:

```
INFO  c.k.p.kafka.impl.KafkaPostEventProducer - Publishing PostCreatedEvent: PostCreatedEvent{postId=1, title='...', ...}
INFO  c.k.p.kafka.impl.KafkaPostEventProducer - Successfully published PostCreatedEvent to topic: post-created, offset: 0
```

## 7. Troubleshooting

### Lỗi: "Connection refused" hoặc "Bootstrap server not available"

**Nguyên nhân:** Kafka chưa start hoặc sai port

**Giải pháp:**
```bash
# Kiểm tra Kafka đang chạy
docker ps | grep kafka

# Kiểm tra port 9092
netstat -an | grep 9092

# Restart Kafka
docker-compose restart kafka
```

### Lỗi: "Topic does not exist"

**Nguyên nhân:** Topic chưa được tạo (Kafka tự động tạo topic khi có message đầu tiên)

**Giải pháp:** 
- Tạo topic thủ công (optional):
```bash
docker exec -it kafka kafka-topics --create \
  --bootstrap-server localhost:9092 \
  --topic post-created \
  --partitions 1 \
  --replication-factor 1
```

### Không thấy messages trong consumer

**Nguyên nhân:** Consumer chưa subscribe đúng topic hoặc chưa có messages

**Giải pháp:**
- Kiểm tra topic name trong code: `TOPIC_POST_CREATED = "post-created"`
- Đảm bảo đã gọi API tạo/update/delete post
- Dùng `--from-beginning` để xem tất cả messages

## 8. Test với Postman/Insomnia

1. Import collection với các requests:
   - POST /auth/login
   - POST /posts (với Bearer token)
   - PUT /posts/{id}
   - DELETE /posts/{id}

2. Mở Kafka consumer terminal song song

3. Gọi API → Xem messages xuất hiện trong consumer terminal

## 9. Verify Event Data Structure

Message trong Kafka sẽ có format JSON:

```json
{
  "postId": 1,
  "title": "Test Post",
  "content": "Content here",
  "authorId": "b125eb37-6726-45e0-8391-b1e502d24260",
  "authorUsername": "student1",
  "createdAt": "2026-01-10T10:30:00",
  "eventType": "PostCreatedEvent",
  "eventTimestamp": "2026-01-10T10:30:00.123456"
}
```

## 10. Next Steps (Tùy chọn)

Sau khi verify Kafka hoạt động, bạn có thể:

1. **Tạo Consumer Service:**
   - notification-service: Subscribe "post-created" → gửi email
   - search-service: Subscribe "post-created/updated" → index vào Elasticsearch
   - analytics-service: Subscribe tất cả events → thống kê

2. **Monitor Kafka:**
   - Dùng Kafka Manager hoặc Confluent Control Center
   - Xem metrics: throughput, latency, lag

3. **Error Handling:**
   - Implement Dead Letter Queue (DLQ) cho failed messages
   - Retry mechanism với exponential backoff
