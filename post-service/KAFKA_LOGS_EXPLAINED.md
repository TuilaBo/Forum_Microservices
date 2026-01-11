# Giải thích Kafka Logs

## Logs bạn đang thấy

### 1. Topic "post-created" được tạo thành công

```
[2026-01-11 10:24:01,940] TRACE [Broker id=1] Cached leader info 
UpdateMetadataPartitionState(
  topicName='post-created', 
  partitionIndex=0, 
  controllerEpoch=1, 
  leader=1, 
  leaderEpoch=0, 
  isr=[1], 
  zkVersion=0, 
  replicas=[1], 
  offlineReplicas=[]
) for partition post-created-0
```

**Giải thích:**
- ✅ **Topic "post-created" đã được tạo tự động** (Kafka auto-create topics)
- ✅ **Partition 0** được tạo cho topic này
- ✅ **Leader = Broker 1** (broker hiện tại của bạn)
- ✅ **ISR = [1]** (In-Sync Replicas - các replicas đang đồng bộ)
- ✅ **Replicas = [1]** (chỉ có 1 replica vì bạn chỉ có 1 broker)

**Nghĩa là:**
- Khi bạn publish `PostCreatedEvent` lần đầu tiên, Kafka tự động tạo topic "post-created"
- Topic này có 1 partition (partition 0)
- Broker 1 là leader của partition này

---

### 2. Metadata được cập nhật

```
[2026-01-11 10:24:01,942] INFO [Broker id=1] Add 1 partitions and deleted 0 partitions 
from metadata cache in response to UpdateMetadata request
```

**Giải thích:**
- ✅ Broker 1 đã **thêm 1 partition** vào metadata cache
- ✅ Partition đó là "post-created-0"
- ✅ **Không có partition nào bị xóa** (deleted 0 partitions)

**Nghĩa là:**
- Kafka Controller (quản lý cluster) đã thông báo cho Broker 1 về partition mới
- Broker 1 đã cập nhật metadata cache của mình
- Bây giờ Broker 1 biết topic "post-created" tồn tại và có partition 0

---

### 3. Controller xác nhận metadata update

```
[2026-01-11 10:24:01,943] TRACE [Controller id=1 epoch=1] Received response 
UpdateMetadataResponseData(errorCode=0) for request UPDATE_METADATA
```

**Giải thích:**
- ✅ **Controller (Broker 1) đã nhận response** từ Broker 1
- ✅ **errorCode=0** → Không có lỗi, thành công
- ✅ **UPDATE_METADATA request** đã hoàn thành

**Nghĩa là:**
- Controller đã gửi metadata update request
- Broker 1 đã xử lý và trả về success
- Metadata đã được đồng bộ

---

### 4. Automatic Preferred Replica Leader Election

```
[2026-01-11 10:24:07,495] INFO [Controller id=1] Processing automatic preferred replica leader election
[2026-01-11 10:24:07,495] TRACE [Controller id=1] Checking need to trigger auto leader balancing
[2026-01-11 10:24:07,501] DEBUG [Controller id=1] Topics not in preferred replica for broker 1 Map()
[2026-01-11 10:24:07,504] TRACE [Controller id=1] Leader imbalance ratio for broker 1 is 0.0
```

**Giải thích:**

#### A. Automatic Preferred Replica Leader Election
- Kafka tự động kiểm tra xem leader partitions có đúng "preferred replica" không
- **Preferred replica** = replica đầu tiên trong danh sách replicas (thường là replica được assign đầu tiên)

**Ví dụ:**
```
Topic "post-created", Partition 0:
- Replicas = [1, 2, 3]
- Preferred replica = 1 (replica đầu tiên)
- Current leader = 1 ✅ (đúng preferred replica)
```

#### B. Auto Leader Balancing
- Kafka kiểm tra xem có cần cân bằng leader không
- Nếu một broker làm leader quá nhiều partitions → có thể mất cân bằng

#### C. Leader Imbalance Ratio = 0.0
- ✅ **0.0 = Không có mất cân bằng**
- Tất cả partitions đều có leader đúng preferred replica
- Không cần thay đổi gì

**Nghĩa là:**
- Kafka đang tự động kiểm tra và đảm bảo leader partitions được phân bổ đúng
- Trong trường hợp của bạn (chỉ có 1 broker), không có vấn đề gì
- Nếu có nhiều brokers, Kafka sẽ tự động cân bằng leaders

---

## Tóm tắt: Kafka đang làm gì?

### Timeline:

```
10:24:01.940 → Topic "post-created" được tạo
              → Partition 0 được tạo
              → Broker 1 là leader

10:24:01.942 → Metadata cache được cập nhật
              → Broker 1 biết về topic mới

10:24:01.943 → Controller xác nhận update thành công

10:24:07.495 → Kafka tự động kiểm tra leader balancing
              → Không có vấn đề (imbalance = 0.0)
```

### Kết luận:

✅ **Kafka đang hoạt động bình thường!**

- Topic "post-created" đã được tạo thành công
- Partition 0 đã sẵn sàng nhận messages
- Metadata đã được đồng bộ
- Leader election đang hoạt động đúng
- Không có lỗi nào (errorCode=0)

---

## Các logs quan trọng khác bạn có thể thấy

### 1. Khi publish message thành công

```
INFO KafkaPostEventProducer - Publishing PostCreatedEvent: PostCreatedEvent{postId=1, ...}
INFO KafkaPostEventProducer - Successfully published PostCreatedEvent to topic: post-created, offset: 0
```

### 2. Khi có lỗi

```
ERROR KafkaPostEventProducer - Failed to publish PostCreatedEvent to topic: post-created
ERROR [Broker id=1] Error processing request
```

### 3. Khi consumer nhận message

```
INFO [Consumer] Received message from topic: post-created, partition: 0, offset: 0
```

### 4. Khi broker restart

```
INFO [KafkaServer id=1] started
INFO [Controller id=1] Ready to serve as the new controller
```

---

## Cách verify Kafka hoạt động đúng

### 1. Kiểm tra topic đã được tạo

```bash
docker exec -it kafka kafka-topics --list --bootstrap-server localhost:9092
```

**Kết quả mong đợi:**
```
post-created
post-updated
post-deleted
```

### 2. Kiểm tra chi tiết topic

```bash
docker exec -it kafka kafka-topics --describe --bootstrap-server localhost:9092 --topic post-created
```

**Kết quả mong đợi:**
```
Topic: post-created	PartitionCount: 1	ReplicationFactor: 1
	Topic: post-created	Partition: 0	Leader: 1	Replicas: 1	Isr: 1
```

### 3. Xem messages trong topic

```bash
docker exec -it kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic post-created \
  --from-beginning
```

### 4. Kiểm tra logs không có ERROR

```bash
docker logs kafka | findstr "ERROR"
```

**Kết quả mong đợi:** Không có dòng nào (hoặc chỉ có warnings không quan trọng)

---

## Troubleshooting

### Nếu thấy ERROR trong logs:

1. **Connection refused**
   - Zookeeper chưa start
   - Kiểm tra: `docker ps | findstr zookeeper`

2. **Topic creation failed**
   - Kiểm tra `KAFKA_AUTO_CREATE_TOPICS_ENABLE=true` trong docker-compose

3. **Leader election failed**
   - Broker không kết nối được Zookeeper
   - Kiểm tra: `docker logs kafka | findstr "Zookeeper"`

4. **Metadata update failed**
   - Network issue giữa Kafka và Zookeeper
   - Kiểm tra: `docker network ls`

---

## Kết luận

**Logs bạn đang thấy = Kafka hoạt động hoàn hảo! ✅**

- Topic "post-created" đã được tạo
- Sẵn sàng nhận messages
- Metadata đã được đồng bộ
- Leader election đang hoạt động đúng

**Bước tiếp theo:**
1. Gọi API tạo post
2. Xem logs: `docker logs kafka -f`
3. Xem messages: `docker exec -it kafka kafka-console-consumer --bootstrap-server localhost:9092 --topic post-created --from-beginning`
