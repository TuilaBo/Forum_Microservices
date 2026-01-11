# Zookeeper là gì và tại sao Kafka cần nó?

## 1. Zookeeper là gì?

**Apache Zookeeper** là một **distributed coordination service** (dịch vụ điều phối phân tán) được thiết kế để quản lý và đồng bộ hóa các services trong hệ thống phân tán.

### Đặc điểm:
- **Centralized Configuration Management**: Quản lý cấu hình tập trung
- **Synchronization**: Đồng bộ hóa giữa các nodes
- **Naming Service**: Dịch vụ đặt tên (như DNS)
- **Group Services**: Quản lý nhóm services
- **High Availability**: Tính sẵn sàng cao

---

## 2. Kafka dùng Zookeeper để làm gì?

### A. Quản lý Metadata (Thông tin về cluster)

```
Kafka Cluster:
├─ Broker 1 (port 9092)
├─ Broker 2 (port 9093)
└─ Broker 3 (port 9094)

Zookeeper lưu:
- Broker nào đang sống/chết
- Topics nào tồn tại
- Partitions của mỗi topic
- Leader partition là broker nào
- Replicas của partitions
```

**Ví dụ:**
```
Topic: "post-created"
├─ Partition 0: Leader = Broker 1, Replicas = [Broker 1, Broker 2]
├─ Partition 1: Leader = Broker 2, Replicas = [Broker 2, Broker 3]
└─ Partition 2: Leader = Broker 3, Replicas = [Broker 3, Broker 1]

→ Zookeeper lưu tất cả thông tin này
```

### B. Leader Election (Bầu chọn Leader)

Khi một broker chết:
1. Zookeeper phát hiện broker chết
2. Zookeeper tổ chức bầu chọn leader mới cho partitions
3. Các brokers còn lại vote để chọn leader
4. Leader mới được chọn và lưu vào Zookeeper

**Ví dụ:**
```
Trước: Partition 0 → Leader = Broker 1
Broker 1 chết
Sau: Partition 0 → Leader = Broker 2 (được bầu chọn)
```

### C. Configuration Management (Quản lý cấu hình)

Zookeeper lưu:
- Cấu hình topics (retention, partitions, replication factor)
- Cấu hình brokers
- Access control lists (ACLs)
- Quotas (giới hạn throughput)

**Ví dụ:**
```
Topic "post-created":
- retention.ms = 7 days
- partitions = 3
- replication.factor = 2

→ Lưu trong Zookeeper
```

### D. Service Discovery (Tìm kiếm dịch vụ)

Khi Producer/Consumer muốn kết nối Kafka:
1. Kết nối Zookeeper trước
2. Zookeeper trả về danh sách brokers đang sống
3. Producer/Consumer kết nối trực tiếp với brokers

**Flow:**
```
Producer muốn gửi message
    ↓
Kết nối Zookeeper: "Brokers nào đang sống?"
    ↓
Zookeeper: "Broker 1, Broker 2, Broker 3"
    ↓
Producer kết nối Broker 1
    ↓
Broker 1: "Topic 'post-created' có partitions: 0, 1, 2"
    ↓
Producer gửi message đến partition phù hợp
```

### E. Distributed Locking (Khóa phân tán)

Đảm bảo chỉ 1 broker là Controller tại một thời điểm:
- Controller quản lý partition leadership
- Zookeeper đảm bảo chỉ có 1 controller active
- Nếu controller chết, Zookeeper bầu controller mới

---

## 3. So sánh: Có Zookeeper vs Không có Zookeeper

### Kafka với Zookeeper (Traditional - hiện tại)

```
┌─────────────┐
│  Zookeeper  │ ← Quản lý metadata, leader election
└──────┬──────┘
       │
       ↓
┌─────────────┐
│   Kafka     │ ← Chỉ lo lưu messages
└─────────────┘
```

**Ưu điểm:**
- ✅ Đã được test kỹ, ổn định
- ✅ Hỗ trợ tốt cho production
- ✅ Dễ scale

**Nhược điểm:**
- ❌ Phụ thuộc vào Zookeeper (nếu Zookeeper chết → Kafka không hoạt động)
- ❌ Cần quản lý 2 services (Kafka + Zookeeper)

### Kafka KRaft Mode (Không cần Zookeeper - tương lai)

```
┌─────────────┐
│   Kafka     │ ← Tự quản lý metadata (không cần Zookeeper)
└─────────────┘
```

**Ưu điểm:**
- ✅ Không cần Zookeeper (đơn giản hơn)
- ✅ Startup nhanh hơn
- ✅ Có thể scale tốt hơn

**Nhược điểm:**
- ❌ Vẫn đang trong giai đoạn development (chưa production-ready hoàn toàn)
- ❌ Một số tính năng chưa hỗ trợ đầy đủ

---

## 4. Zookeeper trong project của bạn

### Cấu hình hiện tại:

```yaml
# docker-compose-kafka.yml
zookeeper:
  image: confluentinc/cp-zookeeper:latest
  ports:
    - "2181:2181"
  environment:
    ZOOKEEPER_CLIENT_PORT: 2181

kafka:
  environment:
    KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181  # ← Kafka kết nối Zookeeper
```

### Flow hoạt động:

```
1. Zookeeper start trước
   ↓
2. Kafka start và kết nối Zookeeper
   ↓
3. Zookeeper lưu metadata:
   - Broker ID = 1
   - Port = 9092
   - Topics (sẽ được tạo khi có message đầu tiên)
   ↓
4. Khi bạn tạo post → Kafka publish event
   ↓
5. Kafka lưu message vào topic
   ↓
6. Zookeeper cập nhật metadata (nếu topic mới được tạo)
```

---

## 5. Kiểm tra Zookeeper đang làm gì

### A. Xem logs Zookeeper

```bash
docker logs zookeeper -f
```

**Logs bạn sẽ thấy:**
```
- Zookeeper đang lắng nghe port 2181
- Client (Kafka) kết nối
- Tạo/update nodes trong Zookeeper tree
```

### B. Kết nối Zookeeper CLI (nếu muốn xem chi tiết)

```bash
# Vào container Zookeeper
docker exec -it zookeeper bash

# Chạy Zookeeper CLI
bin/zkCli.sh

# Xem cấu trúc dữ liệu
ls /
ls /brokers
ls /brokers/ids
```

**Cấu trúc Zookeeper tree:**
```
/
├── brokers/
│   ├── ids/
│   │   └── 1  (Broker ID = 1)
│   └── topics/
│       └── post-created/
│           └── partitions/
│               └── 0/
├── controller/
└── config/
```

---

## 6. Tóm tắt

### Zookeeper = "Bộ não" của Kafka cluster

**Nhiệm vụ chính:**
1. ✅ Quản lý metadata (topics, partitions, brokers)
2. ✅ Leader election (bầu chọn leader khi broker chết)
3. ✅ Service discovery (tìm brokers đang sống)
4. ✅ Configuration management (lưu cấu hình)
5. ✅ Distributed locking (đảm bảo chỉ 1 controller)

### Tại sao cần Zookeeper?

**Không có Zookeeper:**
- Kafka không biết brokers nào đang sống
- Không biết topics nào tồn tại
- Không biết leader partition là broker nào
- → **Kafka không thể hoạt động**

**Có Zookeeper:**
- Kafka biết tất cả thông tin về cluster
- Có thể tự động recover khi broker chết
- Có thể scale dễ dàng
- → **Kafka hoạt động ổn định**

---

## 7. Best Practices

### A. Zookeeper Cluster (Production)

Trong production, nên chạy Zookeeper cluster (3-5 nodes) để đảm bảo high availability:

```yaml
zookeeper-1:
  # ...
zookeeper-2:
  # ...
zookeeper-3:
  # ...
```

### B. Monitoring

Monitor Zookeeper:
- CPU, Memory usage
- Connection count
- Request latency
- Node count

### C. Backup

Backup Zookeeper data định kỳ (quan trọng cho production).

---

## 8. Kết luận

**Zookeeper là gì?**
→ Dịch vụ điều phối phân tán, quản lý metadata và đồng bộ hóa cho Kafka.

**Tại sao Kafka cần Zookeeper?**
→ Kafka không thể hoạt động mà không biết:
- Brokers nào đang sống
- Topics nào tồn tại
- Leader partitions là gì
- → Cần Zookeeper để lưu và quản lý thông tin này

**Trong project của bạn:**
- Zookeeper chạy trên port 2181
- Kafka kết nối Zookeeper qua `zookeeper:2181`
- Khi bạn publish events, Zookeeper cập nhật metadata
- Nếu Kafka restart, Zookeeper giúp Kafka biết lại trạng thái trước đó

**Tương lai:**
- Kafka đang phát triển KRaft mode (không cần Zookeeper)
- Nhưng hiện tại, Zookeeper vẫn là standard và production-ready
