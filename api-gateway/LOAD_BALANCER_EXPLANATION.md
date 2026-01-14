# API Gateway - Load Balancer Explanation

## ❌ Trả Lời: CHƯA CÓ Load Balancer

API Gateway hiện tại **CHƯA CÓ load balancing**. Đang sử dụng **static routing** với hardcoded URIs.

---

## 🔍 Phân Tích Config Hiện Tại

### Config Hiện Tại (Static Routing)

```properties
spring.cloud.gateway.routes[0].uri=http://localhost:8081
spring.cloud.gateway.routes[1].uri=http://localhost:8082
```

**Đặc điểm:**
- ✅ **Static URI**: Hardcoded URL cụ thể
- ✅ **Single Instance**: Mỗi service chỉ có 1 instance
- ❌ **No Load Balancing**: Không có phân tải
- ❌ **No Failover**: Nếu service down → 503 error

**Flow:**
```
Request → Gateway → http://localhost:8081 (cố định)
```

---

## 🔄 Load Balancing Là Gì?

### Khái Niệm

**Load Balancing** = Phân tải requests đến nhiều instances của cùng một service.

### Ví Dụ

**Không có Load Balancing (Hiện tại):**
```
Request 1 → Gateway → post-service:8082
Request 2 → Gateway → post-service:8082
Request 3 → Gateway → post-service:8082
```
→ Tất cả requests đều đến 1 instance

**Có Load Balancing:**
```
Request 1 → Gateway → post-service-instance-1:8082
Request 2 → Gateway → post-service-instance-2:8083
Request 3 → Gateway → post-service-instance-3:8084
```
→ Requests được phân tải đến nhiều instances

---

## 🆚 So Sánh: Static Routing vs Load Balancing

| Tiêu Chí | Static Routing (Hiện tại) | Load Balancing |
|----------|---------------------------|----------------|
| **URI Format** | `http://localhost:8081` | `lb://post-service` |
| **Service Discovery** | ❌ Không cần | ✅ Cần (Eureka, Consul) |
| **Multiple Instances** | ❌ Không hỗ trợ | ✅ Hỗ trợ |
| **Failover** | ❌ Nếu service down → 503 | ✅ Tự động chuyển instance khác |
| **Scalability** | ❌ Phải scale thủ công | ✅ Tự động scale |
| **Configuration** | ✅ Đơn giản | ⚠️ Phức tạp hơn |

---

## 🏗️ Kiến Trúc: Static Routing (Hiện Tại)

```
API Gateway
    ↓
Static Routes
    ├─ /auth/** → http://localhost:8081 (cố định)
    ├─ /posts/** → http://localhost:8082 (cố định)
    └─ /users/** → http://localhost:8084 (cố định)
```

**Vấn đề:**
- Nếu post-service (8082) down → Tất cả requests đến /posts/** sẽ fail
- Không thể scale post-service (chạy nhiều instances)
- Phải config thủ công nếu muốn thêm instance

---

## 🏗️ Kiến Trúc: Load Balancing (Nếu Thêm)

```
API Gateway
    ↓
Service Discovery (Eureka/Consul)
    ├─ post-service
    │   ├─ Instance 1: localhost:8082
    │   ├─ Instance 2: localhost:8083
    │   └─ Instance 3: localhost:8084
    └─ user-service
        ├─ Instance 1: localhost:8084
        └─ Instance 2: localhost:8085
```

**Lợi ích:**
- Tự động phân tải requests
- Failover tự động
- Scale dễ dàng (chỉ cần start thêm instance)

---

## 🔧 Cách Thêm Load Balancing

### Option 1: Spring Cloud LoadBalancer (Recommended)

#### Bước 1: Thêm Dependency

```xml
<!-- api-gateway/pom.xml -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-loadbalancer</artifactId>
</dependency>
```

#### Bước 2: Config Multiple Instances

```properties
# application.properties
# Thay vì:
# spring.cloud.gateway.routes[1].uri=http://localhost:8082

# Dùng:
spring.cloud.gateway.routes[1].uri=lb://post-service

# Config instances
post-service.ribbon.listOfServers=http://localhost:8082,http://localhost:8083,http://localhost:8084
```

#### Bước 3: Load Balancing Algorithms

Spring Cloud Gateway hỗ trợ:
- **Round Robin** (mặc định): Luân phiên
- **Random**: Ngẫu nhiên
- **Weighted**: Theo trọng số

**Config:**
```properties
# Round Robin (mặc định)
spring.cloud.loadbalancer.configurations=default

# Hoặc custom
@Bean
public ReactorLoadBalancer<ServiceInstance> loadBalancer() {
    return new RoundRobinLoadBalancer(...);
}
```

### Option 2: Service Discovery (Eureka/Consul)

#### Bước 1: Setup Eureka Server

```xml
<!-- eureka-server/pom.xml -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-server</artifactId>
</dependency>
```

#### Bước 2: Register Services với Eureka

```properties
# post-service/application.properties
spring.application.name=post-service
eureka.client.service-url.defaultZone=http://localhost:8761/eureka/
```

#### Bước 3: Config Gateway

```properties
# api-gateway/application.properties
spring.cloud.gateway.routes[1].uri=lb://post-service

# Eureka config
eureka.client.service-url.defaultZone=http://localhost:8761/eureka/
```

**Flow:**
```
1. Services register với Eureka
2. Gateway query Eureka để lấy danh sách instances
3. Gateway load balance requests đến các instances
```

---

## 📊 Load Balancing Algorithms

### 1. Round Robin (Mặc định)

**Cách hoạt động:**
```
Request 1 → Instance 1
Request 2 → Instance 2
Request 3 → Instance 3
Request 4 → Instance 1 (quay lại)
```

**Ưu điểm:**
- Đơn giản, công bằng
- Phân tải đều

**Nhược điểm:**
- Không tính đến load của từng instance
- Không tính đến response time

### 2. Random

**Cách hoạt động:**
```
Request 1 → Instance 2 (random)
Request 2 → Instance 1 (random)
Request 3 → Instance 3 (random)
```

**Ưu điểm:**
- Tránh pattern có thể dự đoán

### 3. Weighted Round Robin

**Cách hoạt động:**
```
Instance 1: weight=3 → Nhận 3 requests
Instance 2: weight=2 → Nhận 2 requests
Instance 3: weight=1 → Nhận 1 request
```

**Ưu điểm:**
- Phân tải theo capacity của instance

---

## 🎯 Khi Nào Cần Load Balancing?

### ✅ Cần Load Balancing Khi:

1. **High Traffic**: Nhiều requests, 1 instance không đủ
2. **High Availability**: Cần failover khi instance down
3. **Scalability**: Muốn scale horizontal (thêm instances)
4. **Production**: Production environment thường cần

### ❌ Không Cần Load Balancing Khi:

1. **Development**: Chỉ chạy 1 instance mỗi service
2. **Low Traffic**: Traffic thấp, 1 instance đủ
3. **Simple Setup**: Muốn setup đơn giản
4. **Single Server**: Chỉ có 1 server

---

## 🔍 Kiểm Tra: Có Load Balancing Không?

### Cách 1: Check URI Format

```properties
# ❌ Static routing (KHÔNG có load balancing)
spring.cloud.gateway.routes[0].uri=http://localhost:8081

# ✅ Load balancing (CÓ load balancing)
spring.cloud.gateway.routes[0].uri=lb://auth-service
```

**Hiện tại:** Tất cả routes dùng `http://localhost:XXXX` → **KHÔNG có load balancing**

### Cách 2: Check Dependencies

```xml
<!-- ❌ Không có trong pom.xml -->
<!-- spring-cloud-starter-loadbalancer -->
<!-- spring-cloud-starter-netflix-eureka-client -->
```

**Hiện tại:** Không có dependencies cho load balancing → **KHÔNG có load balancing**

### Cách 3: Check Service Discovery

```properties
# ❌ Không có config
# eureka.client.service-url.defaultZone=...
```

**Hiện tại:** Không có service discovery → **KHÔNG có load balancing**

---

## 💡 Kết Luận

### Trạng Thái Hiện Tại:

❌ **CHƯA CÓ Load Balancing**

**Lý do:**
- Dùng static URIs (`http://localhost:8081`)
- Không có service discovery
- Không có load balancer dependencies
- Mỗi service chỉ có 1 instance

**Phù hợp với:**
- Development environment
- Simple setup
- Low traffic

### Nếu Muốn Thêm Load Balancing:

1. **Thêm dependency**: `spring-cloud-starter-loadbalancer`
2. **Đổi URI format**: `http://localhost:8081` → `lb://auth-service`
3. **Config instances**: List các instances của service
4. **Optional**: Setup service discovery (Eureka/Consul)

**Thời gian implement:** 2-3 ngày (nếu dùng Eureka)

---

## 🎤 Câu Trả Lời Cho Interview

### "API Gateway của bạn có load balancing không?"

**Trả lời:**
"Hiện tại chưa có. Tôi đang dùng static routing với hardcoded URIs vì đây là development setup. Tuy nhiên, tôi hiểu cách implement load balancing với Spring Cloud Gateway:

1. **Static Load Balancing**: Dùng `lb://service-name` với `listOfServers`
2. **Service Discovery**: Dùng Eureka/Consul để tự động discover instances
3. **Algorithms**: Round Robin, Random, Weighted

Trong production, tôi sẽ implement load balancing để:
- Phân tải requests
- Failover tự động
- Scale horizontal dễ dàng"

### "Tại sao bạn chưa implement?"

**Trả lời:**
"Vì đây là development environment với:
- Mỗi service chỉ chạy 1 instance
- Traffic thấp, không cần scale
- Setup đơn giản để focus vào business logic

Nhưng tôi đã chuẩn bị sẵn kiến thức và có thể implement nhanh khi cần."
