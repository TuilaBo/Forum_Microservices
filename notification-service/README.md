# Notification Service

Service nhận Post Events từ Kafka và gửi email thông báo.

## Cấu trúc Package

```
notification-service/
├── src/main/java/com/khoavdse170395/notificationservice/
│   ├── NotificationServiceApplication.java
│   ├── model/
│   │   └── event/
│   │       ├── PostCreatedEvent.java
│   │       ├── PostUpdatedEvent.java
│   │       └── PostDeletedEvent.java
│   ├── config/
│   │   └── KafkaConsumerConfig.java
│   ├── consumer/
│   │   └── PostEventConsumer.java
│   ├── service/
│   │   ├── EmailService.java
│   │   └── impl/
│   │       └── EmailServiceImpl.java
│   └── controller/
│       └── NotificationController.java
└── src/main/resources/
    └── application.properties
```

## Setup

### 1. Cấu hình Email Gmail

Sửa `application.properties`:

```properties
# Thay 'your-email@gmail.com' bằng email Gmail của bạn
spring.mail.username=your-email@gmail.com
spring.mail.password=mncy qcio ueze vxrl

# Email người nhận
notification.moderator.email=moderator@school.edu
notification.admin.email=admin@school.edu
```

**Lưu ý:** 
- Sử dụng **App Password** của Gmail (không phải password thường)
- App Password: `mncy qcio ueze vxrl` (đã được cung cấp)
- Nếu chưa có App Password, tạo tại: https://myaccount.google.com/apppasswords

### 2. Cấu hình Kafka

Đảm bảo Kafka đang chạy:

```bash
docker ps | findstr kafka
```

Nếu chưa chạy:

```bash
cd post-service
docker-compose -f docker-compose-kafka.yml up -d
```

### 3. Build và Run

```bash
cd notification-service
mvn clean install
mvn spring-boot:run
```

Service sẽ chạy trên port **8083**.

## Flow hoạt động

```
1. User tạo post → post-service
   ↓
2. post-service → Publish PostCreatedEvent → Kafka
   ↓
3. Kafka → PostEventConsumer (notification-service)
   ↓
4. PostEventConsumer → EmailService
   ↓
5. EmailService → Gmail SMTP
   ↓
6. Email được gửi đến moderator và admin
```

## Test

### 1. Test Email Service trực tiếp

```bash
POST http://localhost:8083/notifications/test-email
Content-Type: application/x-www-form-urlencoded

to=test@example.com&subject=Test&body=Hello World
```

### 2. Test với Kafka Events

1. Start notification-service
2. Tạo post từ post-service (POST /posts)
3. Xem logs notification-service:
   ```
   INFO PostEventConsumer - Received PostCreatedEvent: ...
   INFO EmailServiceImpl - Email sent successfully to: moderator@school.edu
   ```
4. Kiểm tra email inbox

### 3. Health Check

```bash
GET http://localhost:8083/notifications/health
```

## Logs

Xem logs để verify:

```bash
# Logs khi nhận event
INFO PostEventConsumer - Received PostCreatedEvent: PostCreatedEvent{postId=1, ...}

# Logs khi gửi email thành công
INFO EmailServiceImpl - Email sent successfully to: moderator@school.edu

# Logs khi có lỗi
ERROR EmailServiceImpl - Failed to send email to: moderator@school.edu
```

## Troubleshooting

### Lỗi: "Authentication failed"

**Nguyên nhân:** Email hoặc App Password sai

**Giải pháp:**
- Kiểm tra `spring.mail.username` và `spring.mail.password` trong `application.properties`
- Đảm bảo sử dụng App Password (không phải password thường)

### Lỗi: "Connection refused" (Kafka)

**Nguyên nhân:** Kafka chưa start

**Giải pháp:**
```bash
docker ps | findstr kafka
# Nếu không có, start Kafka
cd post-service
docker-compose -f docker-compose-kafka.yml up -d
```

### Không nhận được events

**Nguyên nhân:** Consumer group chưa subscribe đúng topic

**Giải pháp:**
- Kiểm tra `spring.kafka.consumer.group-id` trong `application.properties`
- Kiểm tra logs: `INFO PostEventConsumer - Received PostCreatedEvent`
- Verify topic tồn tại: `docker exec -it kafka kafka-topics --list --bootstrap-server localhost:9092`

## API Endpoints

- `GET /notifications/health` - Health check
- `POST /notifications/test-email` - Test gửi email

## Dependencies

- Spring Boot Web
- Spring Kafka (Consumer)
- Spring Mail (Gmail SMTP)
- Jackson (JSON deserialization)

## Port

- **8083** (có thể thay đổi trong `application.properties`)
