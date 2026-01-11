# Hướng dẫn setup API Gateway và test API đăng nhập/đăng ký

## 1. Cấu hình Keycloak Client

**QUAN TRỌNG**: Để endpoint `/auth/login` hoạt động, bạn cần bật **"Direct access grants"** trong Keycloak client.

### Bước 1: Vào Keycloak Admin Console
1. Truy cập: http://localhost:8080
2. Đăng nhập với admin/admin
3. Chọn realm `school-forum`

### Bước 2: Cấu hình Client `forum-frontend`
1. Vào **Clients** → Chọn client `forum-frontend`
2. Tab **"Capability config"**
3. **BẬT** toggle **"Direct access grants"** → ON
4. Click **"Save"**

**Giải thích**: "Direct access grants" cho phép client gọi token endpoint với `grant_type=password` (username/password trực tiếp), không cần redirect flow.

---

## 2. Chạy Services

### Chạy auth-service (port 8081):
```bash
cd auth-service
./mvnw spring-boot:run
```

### Chạy api-gateway (port 8088):
```bash
cd api-gateway
./mvnw spring-boot:run
```

---

## 3. Test API qua API Gateway (port 8088)

### 3.1. Đăng nhập trực tiếp (username/password)

**Request:**
```bash
POST http://localhost:8088/auth/login
Content-Type: application/json

{
  "username": "student1",
  "password": "password123"
}
```

**Response:**
```json
{
  "accessToken": "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJ...",
  "refreshToken": "...",
  "tokenType": "Bearer",
  "expiresIn": 300,
  "scope": "openid profile email"
}
```

### 3.2. Lấy thông tin user (cần token)

**Request:**
```bash
GET http://localhost:8088/auth/me
Authorization: Bearer <ACCESS_TOKEN>
```

**Response:**
```json
{
  "id": "1234-5678-...",
  "username": "student1",
  "email": "student1@school.edu",
  "realmRoles": {
    "roles": ["ROLE_STUDENT"]
  },
  "resourceAccess": {},
  "issuedAt": "2026-01-09T10:00:00Z",
  "expiresAt": "2026-01-09T10:05:00Z"
}
```

### 3.3. Lấy login/register URL

**Request:**
```bash
GET http://localhost:8088/auth/login-url
```

**Response:**
```json
{
  "loginUrl": "http://localhost:8080/realms/school-forum/protocol/openid-connect/auth?...",
  "registerUrl": "http://localhost:8080/realms/school-forum/protocol/openid-connect/registrations?...",
  "message": "..."
}
```

---

## 4. Test với curl

### Đăng nhập:
```bash
curl -X POST http://localhost:8088/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"student1","password":"password123"}'
```

### Lấy thông tin user:
```bash
curl http://localhost:8088/auth/me \
  -H "Authorization: Bearer <ACCESS_TOKEN>"
```

---

## 5. Kiến trúc

```
Client (Front-end / Postman)
    ↓
API Gateway (port 8088)
    ↓ /auth/**
Auth Service (port 8081)
    ↓
Keycloak (port 8080)
```

**Flow đăng nhập:**
1. Client gọi: `POST http://localhost:8088/auth/login`
2. API Gateway forward đến: `POST http://localhost:8081/auth/login`
3. Auth Service gọi Keycloak: `POST /realms/school-forum/protocol/openid-connect/token` với `grant_type=password`
4. Keycloak validate username/password → trả về token
5. Auth Service trả về token cho client

**Flow validate token:**
1. Client gọi: `GET http://localhost:8088/auth/me` với `Authorization: Bearer <TOKEN>`
2. API Gateway forward đến: `GET http://localhost:8081/auth/me`
3. Auth Service validate token với Keycloak (issuer + signature)
4. Auth Service trả về user info

---

## 6. Troubleshooting

### Lỗi "Invalid grant" khi login
- Kiểm tra client `forum-frontend` đã bật "Direct access grants" chưa
- Kiểm tra username/password đúng chưa
- Kiểm tra user đã được set password trong Keycloak chưa

### Lỗi "Connection refused" khi gọi API Gateway
- Kiểm tra api-gateway đang chạy trên port 8088 chưa
- Kiểm tra auth-service đang chạy trên port 8081 chưa

### Lỗi "401 Unauthorized" khi gọi /auth/me
- Kiểm tra token còn hiệu lực chưa
- Kiểm tra format header: `Authorization: Bearer <TOKEN>` (có space sau Bearer)
- Kiểm tra token có đúng từ Keycloak realm `school-forum` không

---

## 7. Thêm routes cho các service khác

Khi bạn có thêm services (post-service, comment-service...), thêm vào `application.properties`:

```properties
# Post service
spring.cloud.gateway.routes[1].id=post-service
spring.cloud.gateway.routes[1].uri=http://localhost:8082
spring.cloud.gateway.routes[1].predicates[0]=Path=/posts/**

# Comment service
spring.cloud.gateway.routes[2].id=comment-service
spring.cloud.gateway.routes[2].uri=http://localhost:8083
spring.cloud.gateway.routes[2].predicates[0]=Path=/comments/**
```
