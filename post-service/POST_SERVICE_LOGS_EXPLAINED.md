# Giải thích Logs Post Service

## 1. Kafka Producer được khởi tạo thành công ✅

```
INFO o.a.k.clients.producer.KafkaProducer : [Producer clientId=post-service-producer-1] 
Instantiated an idempotent producer.
```

**Giải thích:**
- ✅ **Kafka Producer đã được tạo thành công**
- ✅ **Idempotent producer** = Đảm bảo không gửi duplicate messages
- ✅ **Client ID**: `post-service-producer-1` (Spring tự động tạo)

**Nghĩa là:**
- `KafkaConfig` đã hoạt động đúng
- `KafkaTemplate` đã được inject thành công
- Producer sẵn sàng gửi messages

---

## 2. Kafka Version Info

```
INFO o.a.kafka.common.utils.AppInfoParser : Kafka version: 4.1.1
INFO o.a.kafka.common.utils.AppInfoParser : Kafka commitId: be816b82d25370ce
```

**Giải thích:**
- ✅ **Kafka Client version: 4.1.1** (version của Spring Kafka dependency)
- ✅ **Commit ID**: ID của commit trong Kafka source code

**Lưu ý:**
- Đây là version của **Kafka Client** (trong Spring Kafka)
- Không phải version của Kafka Broker (7.4.0)
- Client và Broker có thể khác version (backward compatible)

---

## 3. Warning: LEADER_NOT_AVAILABLE (Không phải lỗi nghiêm trọng) ⚠️

```
WARN org.apache.kafka.clients.NetworkClient : [Producer clientId=post-service-producer-1] 
The metadata response from the cluster reported a recoverable issue with correlation id 2 : 
{post-created=LEADER_NOT_AVAILABLE}
```

**Giải thích:**

### Tại sao có warning này?

1. **Topic "post-created" vừa được tạo tự động**
   - Kafka auto-create topic khi có message đầu tiên
   - Topic mới tạo → Leader chưa được bầu chọn ngay

2. **Leader Election đang diễn ra**
   - Zookeeper đang bầu chọn leader cho partition
   - Quá trình này mất vài trăm milliseconds

3. **Producer retry tự động**
   - Producer tự động retry khi gặp `LEADER_NOT_AVAILABLE`
   - Sau khi leader được bầu → Producer gửi message thành công

### Có phải lỗi không?

**KHÔNG!** Đây là **recoverable issue** (vấn đề có thể tự khắc phục):
- ✅ Producer tự động retry
- ✅ Sau khi leader sẵn sàng → Message được gửi thành công
- ✅ Không ảnh hưởng đến functionality

### Khi nào sẽ không thấy warning này?

- Khi topic đã tồn tại trước đó (leader đã được bầu)
- Khi bạn tạo topic thủ công trước khi gửi messages
- Khi có nhiều brokers (leader election nhanh hơn)

---

## 4. Cluster ID được xác định ✅

```
INFO org.apache.kafka.clients.Metadata : [Producer clientId=post-service-producer-1] 
Cluster ID: UvPPSGWOT567rzdbobnjYw
```

**Giải thích:**
- ✅ **Producer đã kết nối thành công với Kafka cluster**
- ✅ **Cluster ID**: Unique ID của Kafka cluster
- ✅ **Metadata đã được fetch** (biết brokers, topics, partitions)

**Nghĩa là:**
- Connection giữa post-service và Kafka broker đã thành công
- Producer biết về cluster topology

---

## 5. Transaction Manager được khởi tạo ✅

```
INFO o.a.k.c.p.internals.TransactionManager : [Producer clientId=post-service-producer-1] 
ProducerId set to 0 with epoch 0
```

**Giải thích:**
- ✅ **Transaction Manager** được setup cho idempotent producer
- ✅ **ProducerId = 0, Epoch = 0**: Initial state
- ✅ Đảm bảo message ordering và exactly-once semantics

---

## 6. PostCreatedEvent được publish thành công ✅✅

### Event 1 (Offset 0):
```
INFO c.k.p.kafka.impl.KafkaPostEventProducer : Publishing PostCreatedEvent: 
PostCreatedEvent{postId=2, title='Javaday1', authorId='ed0a9b53-13d0-4bef-a872-894e9b4d9b07', 
authorUsername='dangkhoavo10', eventTimestamp=2026-01-11T17:27:30.165700600}

INFO c.k.p.kafka.impl.KafkaPostEventProducer : Successfully published PostCreatedEvent 
to topic: post-created, offset: 0
```

### Event 2 (Offset 1):
```
INFO c.k.p.kafka.impl.KafkaPostEventProducer : Successfully published PostCreatedEvent 
to topic: post-created, offset: 1
```

**Giải thích:**
- ✅ **2 events đã được publish thành công**
- ✅ **Offset 0**: Message đầu tiên trong topic
- ✅ **Offset 1**: Message thứ hai trong topic
- ✅ **Topic**: `post-created`
- ✅ **Post ID**: 2 (có thể là post thứ 2 được tạo)

**Nghĩa là:**
- Kafka integration hoạt động hoàn hảo!
- Events đã được lưu vào Kafka
- Có thể verify bằng consumer

---

## 7. Database Operations (Hibernate)

```
Hibernate: insert into posts (author_id, author_username, content, created_at, status, title, updated_at) 
values (?, ?, ?, ?, ?, ?, ?)

Hibernate: insert into post_images (post_id, image_url) values (?, ?)
```

**Giải thích:**
- ✅ **Post được lưu vào PostgreSQL** trước khi publish event
- ✅ **Images được lưu vào bảng `post_images`**
- ✅ **Transaction flow đúng**: Save DB → Publish Event

**Flow:**
```
1. User tạo post
   ↓
2. Save vào PostgreSQL ✅
   ↓
3. Publish PostCreatedEvent lên Kafka ✅
   ↓
4. Return response cho user ✅
```

---

## 8. Warning: PageImpl Serialization (Không liên quan Kafka) ⚠️

```
WARN ration$PageModule$WarningLoggingModifier : Serializing PageImpl instances as-is is not supported, 
meaning that there is no guarantee about the stability of the resulting JSON structure!
```

**Giải thích:**

### Vấn đề:
- Spring Data `PageImpl` không được serialize trực tiếp
- JSON structure có thể thay đổi giữa các versions

### Giải pháp (Tùy chọn - không bắt buộc):

**Option 1: Dùng PagedModel (Spring HATEOAS)**
```java
@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)
```

**Option 2: Tạo custom DTO**
```java
public class PostPageResponse {
    private List<PostResponse> content;
    private int totalPages;
    private long totalElements;
    // ...
}
```

**Option 3: Bỏ qua warning (OK cho development)**
- Warning này không ảnh hưởng functionality
- Chỉ là best practice warning

---

## Tóm tắt: Tất cả đều OK! ✅

### ✅ Kafka Integration:
- Producer khởi tạo thành công
- Kết nối cluster thành công
- Events được publish thành công (offset 0, 1)
- Warning `LEADER_NOT_AVAILABLE` là bình thường (recoverable)

### ✅ Database:
- Posts được lưu vào PostgreSQL
- Images được lưu vào `post_images` table
- Transaction flow đúng

### ⚠️ Warnings (Không nghiêm trọng):
- `LEADER_NOT_AVAILABLE`: Bình thường khi topic mới tạo
- `PageImpl serialization`: Best practice warning (có thể bỏ qua)

---

## Verify Events trong Kafka

Bạn có thể verify events đã được lưu:

```bash
# Xem messages trong topic post-created
docker exec -it kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic post-created \
  --from-beginning

# Kết quả mong đợi:
# {"postId":2,"title":"Javaday1","content":"...","authorId":"...","authorUsername":"dangkhoavo10",...}
# {"postId":3,"title":"...","content":"...","authorId":"...","authorUsername":"...",...}
```

---

## Troubleshooting

### Nếu không thấy "Successfully published":
1. Kiểm tra Kafka đang chạy: `docker ps | findstr kafka`
2. Kiểm tra connection: `docker logs kafka | findstr "ERROR"`
3. Kiểm tra `application.properties`: `spring.kafka.bootstrap-servers=localhost:9092`

### Nếu thấy ERROR thay vì WARN:
- `Connection refused` → Kafka chưa start
- `Topic authorization failed` → Kiểm tra ACLs
- `Serialization error` → Kiểm tra `JsonSerializer` config

---

## Kết luận

**Kafka integration hoạt động hoàn hảo! ✅**

- ✅ Producer đã kết nối Kafka
- ✅ Events đã được publish (offset 0, 1)
- ✅ Database operations thành công
- ⚠️ Warnings là bình thường, không ảnh hưởng functionality

**Bước tiếp theo:**
1. Tạo consumer service để nhận events
2. Hoặc verify bằng console consumer (đã hướng dẫn ở trên)
