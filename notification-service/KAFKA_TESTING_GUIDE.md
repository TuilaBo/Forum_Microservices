# Hướng Dẫn Test Kafka - Hiểu Cách Kafka Hoạt Động

## Mục tiêu

Test để hiểu:
1. ✅ Kafka Producer (post-service) gửi messages
2. ✅ Kafka Consumer (notification-service) nhận messages
3. ✅ Messages được lưu trong Kafka topics
4. ✅ Event-driven flow hoạt động như thế nào

---

## Bước 1: Kiểm Tra Kafka Đang Chạy

### 1.1. Kiểm tra Kafka container:

```bash
docker ps | findstr kafka
```

**Kết quả mong đợi:**
```
CONTAINER ID   IMAGE                        STATUS
abc123         confluentinc/cp-kafka:7.4.0  Up 2 hours
```

### 1.2. Kiểm tra Kafka logs:

```bash
docker logs kafka --tail 20
```

**Kết quả mong đợi:** Không có errors, Kafka đang chạy bình thường

---

## Bước 2: Kiểm Tra Topics

### 2.1. List tất cả topics:

```bash
docker exec -it kafka kafka-topics \
  --bootstrap-server localhost:9092 \
  --list
```

**Kết quả mong đợi:**
```
post-created
post-updated
post-deleted
```

### 2.2. Xem chi tiết topic `post-created`:

```bash
docker exec -it kafka kafka-topics \
  --bootstrap-server localhost:9092 \
  --describe \
  --topic post-created
```

**Kết quả mong đợi:**
```
Topic: post-created	PartitionCount: 1	ReplicationFactor: 1	Configs: segment.ms=604800000
	Topic: post-created	Partition: 0	Leader: 1	Replicas: 1	Isr: 1
```

**Giải thích:**
- `PartitionCount: 1` → Topic có 1 partition
- `ReplicationFactor: 1` → 1 replica (đủ cho development)
- `Leader: 1` → Broker ID 1 là leader

---

## Bước 3: Kiểm Tra Services Đang Chạy

### 3.1. post-service (port 8082):

```bash
# Test health check
curl http://localhost:8082/actuator/health
```

**Kết quả mong đợi:**
```json
{"status":"UP"}
```

### 3.2. notification-service (port 8083):

```bash
# Test health check
curl http://localhost:8083/notifications/health
```

**Kết quả mong đợi:**
```
Notification Service is up and running!
```

---

## Bước 4: Test End-to-End Flow

### 4.1. Tạo Post Mới (Trigger Event)

**Bước 1:** Lấy access token từ Keycloak:

```bash
# Login và lấy token
curl -X POST http://localhost:8081/auth/login \
  -H "Content-Type: application/json" \
  -d "{
    \"username\": \"student1\",
    \"password\": \"your-password\"
  }"
```

**Copy `access_token` từ response**

**Bước 2:** Tạo post mới:

```bash
curl -X POST http://localhost:8082/posts \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -d "{
    \"title\": \"Test Kafka Flow\",
    \"content\": \"Đây là bài viết test để hiểu Kafka hoạt động\",
    \"imageUrls\": []
  }"
```

**Kết quả mong đợi:**
```json
{
  "id": 1,
  "title": "Test Kafka Flow",
  "content": "Đây là bài viết test để hiểu Kafka hoạt động",
  "authorId": "...",
  "authorUsername": "student1",
  "status": "PENDING",
  "createdAt": "2026-01-11T18:30:00",
  "updatedAt": "2026-01-11T18:30:00",
  "imageUrls": []
}
```

---

## Bước 5: Xem Logs Của Các Services

### 5.1. post-service Logs (Producer):

**Tìm dòng:**
```
INFO KafkaPostEventProducer - Publishing PostCreatedEvent: PostCreatedEvent{postId=1, ...}
INFO KafkaPostEventProducer - Successfully published PostCreatedEvent to topic: post-created, offset: 0
```

**Giải thích:**
- `Publishing PostCreatedEvent` → post-service đang gửi event
- `Successfully published` → Event đã được gửi lên Kafka
- `offset: 0` → Message được lưu ở offset 0 trong partition

### 5.2. notification-service Logs (Consumer):

**Tìm dòng:**
```
INFO PostEventConsumer - Received PostCreatedEvent: PostCreatedEvent{postId=1, ...}
INFO EmailServiceImpl - Email sent successfully to: sonh1496@gmail.com
INFO EmailServiceImpl - Email sent successfully to: check.dangkhoa2@gmail.com
INFO PostEventConsumer - Successfully processed PostCreatedEvent for postId: 1
```

**Giải thích:**
- `Received PostCreatedEvent` → notification-service đã nhận event từ Kafka
- `Email sent successfully` → Email đã được gửi
- `Successfully processed` → Event đã được xử lý xong

---

## Bước 6: Xem Messages Trong Kafka Topics

### 6.1. Consume messages từ topic `post-created`:

```bash
docker exec -it kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic post-created \
  --from-beginning \
  --max-messages 1
```

**Kết quả mong đợi:**
```json
{
  "postId": 1,
  "title": "Test Kafka Flow",
  "content": "Đây là bài viết test để hiểu Kafka hoạt động",
  "authorId": "...",
  "authorUsername": "student1",
  "createdAt": [2026, 1, 11, 18, 30, 0, ...],
  "eventType": "PostCreatedEvent",
  "eventTimestamp": [2026, 1, 11, 18, 30, 0, ...]
}
```

**Giải thích:**
- Message được lưu dưới dạng JSON trong Kafka
- `from-beginning` → Đọc từ đầu topic
- `max-messages 1` → Chỉ đọc 1 message

### 6.2. Xem consumer groups:

```bash
docker exec -it kafka kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --list
```

**Kết quả mong đợi:**
```
notification-service-group
```

### 6.3. Xem offset của consumer group:

```bash
docker exec -it kafka kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --group notification-service-group \
  --describe
```

**Kết quả mong đợi:**
```
GROUP                TOPIC           PARTITION  CURRENT-OFFSET  LAG
notification-service post-created     0          1               0
```

**Giải thích:**
- `CURRENT-OFFSET: 1` → Consumer đã đọc đến offset 1
- `LAG: 0` → Không có messages chưa được xử lý

---

## Bước 7: Test Các Scenarios Khác

### 7.1. Test Update Post (PostUpdatedEvent):

```bash
# Update post
curl -X PUT http://localhost:8082/posts/1 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -d "{
    \"title\": \"Updated Title\",
    \"content\": \"Updated content\"
  }"
```

**Xem logs:**
- post-service: `Publishing PostUpdatedEvent`
- notification-service: `Received PostUpdatedEvent`

### 7.2. Test Delete Post (PostDeletedEvent):

```bash
# Delete post
curl -X DELETE http://localhost:8082/posts/1 \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

**Xem logs:**
- post-service: `Publishing PostDeletedEvent`
- notification-service: `Received PostDeletedEvent`

---

## Bước 8: Test Consumer Behavior

### 8.1. Stop notification-service:

```bash
# Stop service (Ctrl+C)
```

### 8.2. Tạo nhiều posts mới:

```bash
# Tạo 3 posts
curl -X POST http://localhost:8082/posts ...
curl -X POST http://localhost:8082/posts ...
curl -X POST http://localhost:8082/posts ...
```

### 8.3. Kiểm tra messages trong Kafka:

```bash
docker exec -it kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic post-created \
  --from-beginning
```

**Kết quả:** Sẽ thấy 3 messages mới

### 8.4. Start lại notification-service:

```bash
cd notification-service
mvn spring-boot:run
```

**Quan sát logs:**
- notification-service sẽ nhận TẤT CẢ 3 messages ngay lập tức
- Điều này chứng minh: Messages được lưu trong Kafka, không mất khi consumer down

---

## Bước 9: Test Multiple Consumers

### 9.1. Tạo consumer mới (giả lập):

```bash
# Consume messages với group khác
docker exec -it kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic post-created \
  --group test-consumer-group \
  --from-beginning
```

**Giải thích:**
- Mỗi consumer group nhận TẤT CẢ messages
- `notification-service-group` và `test-consumer-group` đều nhận cùng messages
- Đây là **pub-sub pattern**: 1 producer → nhiều consumers

---

## Bước 10: Visualize Flow

### Flow Diagram:

```
1. User tạo post
   ↓
2. post-service lưu vào database
   ↓
3. post-service publish PostCreatedEvent → Kafka topic "post-created"
   ↓
4. Kafka lưu message (persistent storage)
   ↓
5. notification-service (consumer) nhận message
   ↓
6. notification-service xử lý: gửi email
   ↓
7. Consumer commit offset → Đánh dấu đã xử lý
```

### Timeline:

```
Time 0s:  User tạo post
Time 0.1s: post-service lưu DB
Time 0.2s: post-service publish event → Kafka
Time 0.3s: Kafka lưu message
Time 0.4s: notification-service nhận message
Time 0.5s: notification-service gửi email
Time 0.6s: Consumer commit offset
```

---

## Bước 11: Kiểm Tra Email

### 11.1. Kiểm tra inbox:

- **Moderator email:** `sonh1496@gmail.com`
- **Admin email:** `check.dangkhoa2@gmail.com`

**Email sẽ có:**
- Subject: `[Forum Notification] New Post Awaiting Approval: Test Kafka Flow`
- Body: HTML với thông tin post

---

## Bước 12: Advanced Testing

### 12.1. Test với nhiều partitions:

```bash
# Tạo topic với 3 partitions
docker exec -it kafka kafka-topics \
  --bootstrap-server localhost:9092 \
  --create \
  --topic test-topic \
  --partitions 3 \
  --replication-factor 1
```

**Giải thích:**
- Messages được phân bổ vào các partitions
- Mỗi partition có thể xử lý song song

### 12.2. Test message ordering:

```bash
# Tạo 5 posts liên tiếp
for i in {1..5}; do
  curl -X POST http://localhost:8082/posts ...
done
```

**Quan sát:**
- Messages trong cùng partition được xử lý theo thứ tự
- Messages ở partitions khác có thể xử lý song song

---

## Troubleshooting

### Lỗi: "Topic not found"

```bash
# Tạo topic manually
docker exec -it kafka kafka-topics \
  --bootstrap-server localhost:9092 \
  --create \
  --topic post-created \
  --partitions 1 \
  --replication-factor 1
```

### Lỗi: "Consumer group not found"

- Normal behavior khi chưa có consumer nào subscribe
- Sẽ tự động tạo khi consumer start

### Lỗi: "No messages in topic"

- Kiểm tra post-service có publish events không
- Xem logs post-service: `Publishing PostCreatedEvent`

---

## Key Concepts Hiểu Được Sau Khi Test

### 1. **Producer (post-service)**
- Gửi messages lên Kafka
- Không biết ai sẽ nhận messages

### 2. **Consumer (notification-service)**
- Subscribe topic và nhận messages
- Không biết ai gửi messages

### 3. **Topic**
- Category/feed name
- Messages được lưu persistent

### 4. **Partition**
- Topic được chia thành partitions
- Mỗi partition là một log file

### 5. **Offset**
- Vị trí của message trong partition
- Consumer commit offset sau khi xử lý

### 6. **Consumer Group**
- Nhiều consumers cùng group chia sẻ messages
- Mỗi consumer xử lý một phần messages

### 7. **Event-Driven Architecture**
- Services giao tiếp qua events
- Loose coupling, scalable, reliable

---

## Quick Test Commands

```bash
# 1. List topics
docker exec -it kafka kafka-topics --bootstrap-server localhost:9092 --list

# 2. Consume messages
docker exec -it kafka kafka-console-consumer --bootstrap-server localhost:9092 --topic post-created --from-beginning

# 3. Check consumer groups
docker exec -it kafka kafka-consumer-groups --bootstrap-server localhost:9092 --list

# 4. Check offsets
docker exec -it kafka kafka-consumer-groups --bootstrap-server localhost:9092 --group notification-service-group --describe

# 5. Create post (trigger event)
curl -X POST http://localhost:8082/posts -H "Authorization: Bearer TOKEN" -d '{"title":"Test","content":"Test"}'
```

---

## Kết Luận

Sau khi test, bạn sẽ hiểu:
- ✅ Kafka là message broker (trung gian)
- ✅ Producer gửi, Consumer nhận
- ✅ Messages được lưu persistent
- ✅ Event-driven architecture hoạt động như thế nào
- ✅ Services không biết về nhau, chỉ biết về Kafka

**Đây là foundation của microservices architecture!**
