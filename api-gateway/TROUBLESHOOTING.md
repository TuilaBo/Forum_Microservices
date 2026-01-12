# API Gateway Troubleshooting Guide

## 🔍 Vấn Đề: API gọi qua Gateway (8088) không được nhưng gọi trực tiếp (8082) được

### ✅ Giải Pháp

#### 1. Kiểm tra API Gateway đã chạy chưa

```bash
# Kiểm tra port 8088 có đang listen không
netstat -ano | findstr :8088
# hoặc
Get-NetTCPConnection -LocalPort 8088
```

**Nếu chưa chạy:**
```bash
cd api-gateway
mvn spring-boot:run
```

#### 2. Kiểm tra Routes Configuration

Đảm bảo file `api-gateway/src/main/resources/application.properties` có đầy đủ routes:

```properties
# Post Service
spring.cloud.gateway.routes[1].id=post-service
spring.cloud.gateway.routes[1].uri=http://localhost:8082
spring.cloud.gateway.routes[1].predicates[0]=Path=/posts/**
```

#### 3. Restart API Gateway

Sau khi cập nhật `application.properties`, **phải restart** API Gateway:

```bash
# Dừng API Gateway (Ctrl+C)
# Sau đó chạy lại
cd api-gateway
mvn spring-boot:run
```

#### 4. Kiểm tra Post Service đang chạy

```bash
# Test trực tiếp post-service
curl http://localhost:8082/posts

# Test qua Gateway
curl http://localhost:8088/posts
```

#### 5. Kiểm tra Logs

Xem logs của API Gateway để tìm lỗi:

```bash
# Logs sẽ hiển thị:
# - Routes được load
# - Requests được forward đến service nào
# - Errors nếu có
```

#### 6. Test với curl

```bash
# Test GET /posts qua Gateway
curl http://localhost:8088/posts

# Test POST /posts qua Gateway (cần token)
curl -X POST http://localhost:8088/posts \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {token}" \
  -d '{"title":"Test","content":"Test content"}'
```

---

## 🐛 Common Issues

### Issue 1: "503 Service Unavailable"

**Nguyên nhân:** Post-service không chạy hoặc không accessible từ Gateway

**Giải pháp:**
- Kiểm tra post-service đang chạy trên port 8082
- Kiểm tra `spring.cloud.gateway.routes[1].uri=http://localhost:8082` đúng chưa

### Issue 2: "404 Not Found"

**Nguyên nhân:** Route không được config hoặc path không match

**Giải pháp:**
- Kiểm tra route config trong `application.properties`
- Đảm bảo path match: `/posts/**` sẽ match `/posts`, `/posts/1`, `/posts/my-posts`, etc.

### Issue 3: "CORS Error"

**Nguyên nhân:** CORS chưa được config đúng

**Giải pháp:**
- Đảm bảo CORS config trong `application.properties`:
```properties
spring.cloud.gateway.globalcors.cors-configurations[/**].allowedOrigins=*
spring.cloud.gateway.globalcors.cors-configurations[/**].allowedMethods=GET,POST,PUT,DELETE,OPTIONS,PATCH
spring.cloud.gateway.globalcors.cors-configurations[/**].allowedHeaders=*
```

### Issue 4: Routes không được load

**Nguyên nhân:** Format trong `application.properties` sai

**Giải pháp:**
- Đảm bảo format đúng:
```properties
spring.cloud.gateway.routes[0].id=auth-service
spring.cloud.gateway.routes[0].uri=http://localhost:8081
spring.cloud.gateway.routes[0].predicates[0]=Path=/auth/**

spring.cloud.gateway.routes[1].id=post-service
spring.cloud.gateway.routes[1].uri=http://localhost:8082
spring.cloud.gateway.routes[1].predicates[0]=Path=/posts/**
```

---

## ✅ Verification Checklist

- [ ] API Gateway đang chạy trên port 8088
- [ ] Post-service đang chạy trên port 8082
- [ ] File `application.properties` có route cho post-service
- [ ] Đã restart API Gateway sau khi cập nhật config
- [ ] Test trực tiếp `http://localhost:8082/posts` thành công
- [ ] Test qua Gateway `http://localhost:8088/posts` thành công

---

## 🔧 Debug Commands

### Kiểm tra routes đã được load:

Xem logs khi start API Gateway, sẽ thấy:
```
Routes matched: [auth-service, post-service, comment-service, ...]
```

### Test từng bước:

1. **Test Gateway health:**
```bash
curl http://localhost:8088/actuator/health
```

2. **Test direct service:**
```bash
curl http://localhost:8082/posts
```

3. **Test through Gateway:**
```bash
curl http://localhost:8088/posts
```

---

## 📝 Notes

- **Luôn restart API Gateway** sau khi thay đổi `application.properties`
- **Path matching:** `/posts/**` sẽ match tất cả paths bắt đầu bằng `/posts/`
- **Gateway forward:** Gateway sẽ forward request đến `http://localhost:8082/posts` (giữ nguyên path)
