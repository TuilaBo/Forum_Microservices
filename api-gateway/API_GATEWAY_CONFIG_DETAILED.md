# API Gateway Configuration - Giải Thích Chi Tiết

## 📋 Tổng Quan

API Gateway sử dụng **Spring Cloud Gateway** - một reactive, non-blocking gateway framework. Gateway này đóng vai trò là single entry point cho tất cả requests từ frontend đến các microservices.

---

## 🏗️ Kiến Trúc

```
Frontend (Next.js)
    ↓
API Gateway (port 8088)
    ├─ Route: /auth/** → Auth Service (8081)
    ├─ Route: /posts/** → Post Service (8082)
    ├─ Route: /comments/** → Comment Service (8083)
    ├─ Route: /users/** → User Service (8084)
    └─ Route: /notifications/** → Notification Service (8085)
```

---

## 📝 File: `application.properties`

### 1. Basic Configuration

```properties
spring.application.name=api-gateway
server.port=8088
```

**Giải thích:**
- `spring.application.name`: Tên service trong Spring Cloud ecosystem
- `server.port`: Port mà API Gateway lắng nghe (8088)

---

### 2. Routes Configuration

#### Cú Pháp Route Configuration

```properties
spring.cloud.gateway.routes[INDEX].id=route-id
spring.cloud.gateway.routes[INDEX].uri=target-service-url
spring.cloud.gateway.routes[INDEX].predicates[0]=Path=/path/**
```

**Giải thích:**
- `routes[INDEX]`: Mảng các routes, INDEX bắt đầu từ 0
- `id`: Unique identifier cho route (dùng để reference trong logs)
- `uri`: URL của target service
- `predicates[0]`: Điều kiện để match route (Path predicate)

#### Route 1: Auth Service

```properties
spring.cloud.gateway.routes[0].id=auth-service
spring.cloud.gateway.routes[0].uri=http://localhost:8081
spring.cloud.gateway.routes[0].predicates[0]=Path=/auth/**
```

**Giải thích:**
- **ID**: `auth-service` - tên route
- **URI**: `http://localhost:8081` - target service
- **Predicate**: `Path=/auth/**` - match tất cả paths bắt đầu bằng `/auth/`

**Ví dụ routing:**
```
Request: GET http://localhost:8088/auth/login
  ↓
Match predicate: Path=/auth/**
  ↓
Forward to: http://localhost:8081/auth/login
```

**Lưu ý:** Path được giữ nguyên khi forward. `/auth/login` → `/auth/login` (không strip prefix)

#### Route 2: Post Service

```properties
spring.cloud.gateway.routes[1].id=post-service
spring.cloud.gateway.routes[1].uri=http://localhost:8082
spring.cloud.gateway.routes[1].predicates[0]=Path=/posts/**
```

**Ví dụ routing:**
```
Request: GET http://localhost:8088/posts?page=0&size=10
  ↓
Match predicate: Path=/posts/**
  ↓
Forward to: http://localhost:8082/posts?page=0&size=10
```

#### Route 3-5: Comment, User, Notification Services

Tương tự như trên, mỗi service có một route riêng.

---

### 3. HTTP Client Configuration

```properties
spring.cloud.gateway.httpclient.connect-timeout=10000
spring.cloud.gateway.httpclient.response-timeout=30000
```

**Giải thích:**
- `connect-timeout=10000`: Thời gian chờ kết nối đến target service (10 giây = 10000ms)
  - Nếu không kết nối được trong 10s → trả về timeout error
- `response-timeout=30000`: Thời gian chờ response từ target service (30 giây = 30000ms)
  - Nếu service không trả response trong 30s → trả về timeout error

**Tại sao cần:**
- Tránh Gateway bị block khi service chậm
- Fail fast - trả lỗi nhanh thay vì đợi mãi
- Bảo vệ Gateway khỏi slow services

**Ví dụ:**
```
Request → Gateway → Post Service (chậm, mất 35s)
  ↓
Gateway đợi 30s
  ↓
Timeout → Trả về 504 Gateway Timeout
```

---

## 🔧 File: `CorsConfig.java`

### Tại Sao Cần CORS Config?

Khi frontend (Next.js) gọi API từ domain khác (CORS), browser sẽ:
1. Gửi **preflight request** (OPTIONS) trước
2. Kiểm tra CORS headers từ server
3. Nếu được phép → gửi actual request

### Chi Tiết Configuration

```java
@Configuration
public class CorsConfig {

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration config = new CorsConfiguration();

        // 1. Allowed Origins
        config.setAllowedOriginPatterns(Arrays.asList(
            "http://localhost:*",
            "http://127.0.0.1:*"
        ));
        
        // 2. Allow Credentials
        config.setAllowCredentials(true);
        
        // 3. Allowed Methods
        config.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"
        ));
        
        // 4. Allowed Headers
        config.setAllowedHeaders(Arrays.asList("*"));
        
        // 5. Exposed Headers
        config.setExposedHeaders(Arrays.asList(
            "Authorization",
            "Content-Type",
            "Access-Control-Allow-Origin",
            "Access-Control-Allow-Credentials"
        ));
        
        // 6. Max Age
        config.setMaxAge(3600L);
        
        // 7. Apply to all paths
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        
        return new CorsWebFilter(source);
    }
}
```

### Giải Thích Từng Phần

#### 1. Allowed Origin Patterns

```java
config.setAllowedOriginPatterns(Arrays.asList(
    "http://localhost:*",
    "http://127.0.0.1:*"
));
```

**Giải thích:**
- Cho phép tất cả localhost ports (3000, 3001, 5173, v.v.)
- `*` là wildcard cho port number
- Tại sao dùng `allowedOriginPatterns` thay vì `allowedOrigins`?
  - `allowedOrigins` không cho phép wildcard khi `allowCredentials=true`
  - `allowedOriginPatterns` cho phép pattern matching với credentials

**Ví dụ:**
- ✅ `http://localhost:3000` → Allowed
- ✅ `http://localhost:5173` → Allowed
- ✅ `http://127.0.0.1:3000` → Allowed
- ❌ `http://example.com:3000` → Not allowed

#### 2. Allow Credentials

```java
config.setAllowCredentials(true);
```

**Giải thích:**
- Cho phép gửi cookies, authorization headers
- Cần thiết để gửi JWT token trong headers
- Khi `allowCredentials=true`, không thể dùng `allowedOrigins=*`

**Ví dụ:**
```javascript
// Frontend có thể gửi credentials
fetch('http://localhost:8088/auth/login', {
  credentials: 'include', // Gửi cookies
  headers: {
    'Authorization': 'Bearer token' // Gửi token
  }
});
```

#### 3. Allowed Methods

```java
config.setAllowedMethods(Arrays.asList(
    "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"
));
```

**Giải thích:**
- Các HTTP methods được phép
- `OPTIONS` là bắt buộc cho preflight requests
- Các methods khác tùy theo API endpoints

#### 4. Allowed Headers

```java
config.setAllowedHeaders(Arrays.asList("*"));
```

**Giải thích:**
- Cho phép tất cả headers từ client
- Bao gồm: `Authorization`, `Content-Type`, `X-Custom-Header`, v.v.
- `*` = wildcard cho tất cả headers

**Ví dụ headers được phép:**
```
Authorization: Bearer token
Content-Type: application/json
X-Requested-With: XMLHttpRequest
Custom-Header: value
```

#### 5. Exposed Headers

```java
config.setExposedHeaders(Arrays.asList(
    "Authorization",
    "Content-Type",
    "Access-Control-Allow-Origin",
    "Access-Control-Allow-Credentials"
));
```

**Giải thích:**
- Headers mà frontend có thể đọc được từ response
- Mặc định, browser chỉ expose một số headers cơ bản
- Cần expose `Authorization` nếu muốn frontend đọc được

#### 6. Max Age

```java
config.setMaxAge(3600L);
```

**Giải thích:**
- Thời gian browser cache preflight response (1 giờ = 3600s)
- Trong 1 giờ, browser không gửi preflight request lại
- Giảm số lượng OPTIONS requests

**Flow:**
```
Request 1: OPTIONS → Server → Cache 1 giờ
Request 2-100: Không cần OPTIONS (dùng cache)
Sau 1 giờ: OPTIONS lại
```

#### 7. Apply to All Paths

```java
UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
source.registerCorsConfiguration("/**", config);
```

**Giải thích:**
- Áp dụng CORS config cho tất cả paths (`/**`)
- Có thể config riêng cho từng path nếu cần

---

## 🔄 Spring Cloud Gateway Concepts

### 1. Routes

**Route** = Quy tắc định tuyến request đến target service.

**Components:**
- **ID**: Unique identifier
- **URI**: Target service URL
- **Predicates**: Điều kiện match route
- **Filters**: Xử lý request/response (optional)

### 2. Predicates

**Predicate** = Điều kiện để match route.

**Types:**
- `Path=/auth/**` - Match path pattern
- `Method=GET` - Match HTTP method
- `Header=X-Requested-With` - Match header
- `Query=param=value` - Match query parameter

**Ví dụ:**
```properties
# Match GET requests to /posts
spring.cloud.gateway.routes[0].predicates[0]=Path=/posts/**
spring.cloud.gateway.routes[0].predicates[1]=Method=GET
```

### 3. Filters

**Filter** = Xử lý request/response trước/sau khi forward.

**Types:**
- **Pre-filter**: Xử lý request trước khi forward
- **Post-filter**: Xử lý response sau khi nhận từ service

**Ví dụ filters có thể thêm:**
- `AddRequestHeader` - Thêm header vào request
- `AddResponseHeader` - Thêm header vào response
- `RewritePath` - Thay đổi path
- `Retry` - Retry khi service fail
- `CircuitBreaker` - Circuit breaker pattern

---

## 📊 Request Flow Chi Tiết

### Ví Dụ: GET /posts

```
1. Client Request
   GET http://localhost:8088/posts?page=0&size=10
   Headers: Authorization: Bearer token

2. API Gateway nhận request
   ↓
3. CORS Preflight (nếu cần)
   OPTIONS /posts
   → CorsWebFilter xử lý
   → Trả về CORS headers
   ↓
4. Route Matching
   - Check predicates: Path=/posts/**
   - Match route: post-service
   ↓
5. Forward Request
   GET http://localhost:8082/posts?page=0&size=10
   Headers: Authorization: Bearer token
   ↓
6. Post Service xử lý
   ↓
7. Response từ Post Service
   Status: 200 OK
   Body: { posts: [...] }
   ↓
8. API Gateway forward response
   ↓
9. Client nhận response
```

---

## 🔍 Debugging & Monitoring

### 1. Xem Routes Đã Load

Khi start API Gateway, logs sẽ hiển thị:
```
Route matched: post-service
Matching path: "/posts/**"
```

### 2. Test Route

```bash
# Test route hoạt động
curl http://localhost:8088/posts

# Xem response headers
curl -v http://localhost:8088/posts
```

### 3. Common Issues

#### Issue 1: 404 Not Found
**Nguyên nhân:** Route không match hoặc service không chạy
**Giải pháp:**
- Kiểm tra route config trong `application.properties`
- Kiểm tra service đang chạy
- Restart API Gateway

#### Issue 2: 503 Service Unavailable
**Nguyên nhân:** Service không accessible
**Giải pháp:**
- Kiểm tra service đang chạy
- Kiểm tra URI trong route config
- Kiểm tra network/firewall

#### Issue 3: CORS Error
**Nguyên nhân:** CORS config không đúng
**Giải pháp:**
- Kiểm tra `CorsConfig.java`
- Kiểm tra origin từ frontend
- Kiểm tra `allowedOriginPatterns`

---

## 🚀 Advanced Configurations (Có Thể Thêm)

### 1. Load Balancing

```properties
# Sử dụng service discovery (Eureka, Consul)
spring.cloud.gateway.routes[0].uri=lb://post-service
```

### 2. Retry Configuration

```properties
spring.cloud.gateway.routes[0].filters[0]=Retry=3
```

### 3. Circuit Breaker

```properties
spring.cloud.gateway.routes[0].filters[0]=CircuitBreaker=post-service-cb
```

### 4. Rate Limiting (với Redis)

```java
@Bean
public RateLimiter rateLimiter() {
    return RedisRateLimiter.builder()
        .setReplenishRate(100) // 100 requests/second
        .setBurstCapacity(200) // Burst capacity
        .build();
}
```

---

## 📝 Tóm Tắt

### Các Config Chính:

1. **Routes**: Định nghĩa cách route requests đến services
2. **CORS**: Xử lý cross-origin requests từ frontend
3. **HTTP Client**: Timeout configuration
4. **Filters**: Xử lý request/response (có thể thêm)

### Key Points:

- ✅ API Gateway là single entry point
- ✅ Routes được match theo predicates
- ✅ CORS được handle ở Gateway level
- ✅ Timeout config bảo vệ Gateway khỏi slow services
- ✅ Reactive, non-blocking architecture

---

## 🎯 Best Practices

1. **Luôn route qua Gateway**: Frontend không nên gọi trực tiếp services
2. **CORS config ở Gateway**: Không cần config CORS ở mỗi service
3. **Timeout hợp lý**: Đủ lớn cho normal requests, đủ nhỏ để fail fast
4. **Logging**: Enable logging để debug routing issues
5. **Health Checks**: Monitor Gateway và services health
