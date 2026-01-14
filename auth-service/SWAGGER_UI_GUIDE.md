# Swagger UI - Auth Service

## 🔗 URL Swagger UI

**Auth Service Swagger UI:**
```
http://localhost:8081/swagger-ui/index.html
```

**OpenAPI JSON:**
```
http://localhost:8081/v3/api-docs
```

## ⚠️ Lưu ý về Port

- **Auth Service:** Port `8081` (KHÔNG phải 8085)
- **Notification Service:** Port `8085`
- **Post Service:** Port `8082`
- **User Service:** Port `8084`
- **Comment Service:** Port `8083`
- **API Gateway:** Port `8088`

## 📋 Các Endpoints trong Swagger

### Public Endpoints (Không cần authentication)
- `POST /auth/login` - Đăng nhập
- `POST /auth/register` - Đăng ký
- `POST /auth/refresh` - Refresh token
- `GET /auth/login-url` - Lấy URL đăng nhập Keycloak
- `GET /auth/register-url` - Lấy URL đăng ký Keycloak
- `POST /auth/token` - Đổi code thành token
- `GET /auth/callback` - Callback từ Keycloak

### Protected Endpoints (Cần JWT token)
- `GET /auth/me` - Lấy thông tin user hiện tại

## 🔐 Cách sử dụng Swagger UI

### 1. Truy cập Swagger UI
Mở trình duyệt và vào: `http://localhost:8081/swagger-ui/index.html`

### 2. Test Public Endpoints
Các endpoint như `/auth/login`, `/auth/register` có thể test trực tiếp mà không cần authentication.

### 3. Test Protected Endpoints
Để test endpoint `/auth/me`:

1. **Đăng nhập trước để lấy token:**
   - Click vào `POST /auth/login`
   - Click "Try it out"
   - Nhập request body:
   ```json
   {
     "username": "student1",
     "password": "password123"
   }
   ```
   - Click "Execute"
   - Copy `accessToken` từ response

2. **Authorize trong Swagger:**
   - Click nút "Authorize" ở góc trên bên phải
   - Trong field "bearer-keycloak", nhập: `Bearer {accessToken}`
   - Ví dụ: `Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...`
   - Click "Authorize"
   - Click "Close"

3. **Test endpoint `/auth/me`:**
   - Click vào `GET /auth/me`
   - Click "Try it out"
   - Click "Execute"
   - Xem kết quả

## 🧪 Ví dụ Request/Response

### POST /auth/login
**Request:**
```json
{
  "username": "student1",
  "password": "password123"
}
```

**Response:**
```json
{
  "accessToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 300,
  "user": {
    "id": "c4144f5a-0226-4fd4-a596-e9d0da3959b7",
    "username": "student1",
    "email": "student1@school.edu",
    "firstName": "John",
    "lastName": "Doe"
  }
}
```

### GET /auth/me (Protected)
**Headers:**
```
Authorization: Bearer {accessToken}
```

**Response:**
```json
{
  "id": "c4144f5a-0226-4fd4-a596-e9d0da3959b7",
  "username": "student1",
  "email": "student1@school.edu",
  "realmAccess": {...},
  "resourceAccess": {...},
  "issuedAt": "2026-01-12T10:00:00Z",
  "expiresAt": "2026-01-12T10:05:00Z"
}
```

## 🔧 Troubleshooting

### Swagger UI không load được
1. Kiểm tra auth-service đã chạy chưa:
   ```bash
   # Kiểm tra log hoặc process
   ```
2. Kiểm tra port 8081 có đang được sử dụng không
3. Restart auth-service

### 401 Unauthorized khi test protected endpoints
- Đảm bảo đã click "Authorize" và nhập token đúng format: `Bearer {token}`
- Kiểm tra token chưa hết hạn (thường là 5 phút)
- Nếu token hết hạn, dùng `/auth/refresh` để lấy token mới

### Swagger UI hiển thị "Failed to load API definition"
- Kiểm tra endpoint `/v3/api-docs` có hoạt động không: `http://localhost:8081/v3/api-docs`
- Kiểm tra SecurityConfig có cho phép `/v3/api-docs/**` không

## 📝 Cấu hình Swagger

Swagger được cấu hình trong:
- `OpenApiConfig.java` - Cấu hình OpenAPI info và security scheme
- `SecurityConfig.java` - Cho phép truy cập `/swagger-ui/**` và `/v3/api-docs/**`
- `pom.xml` - Dependency `springdoc-openapi-starter-webmvc-ui`

## 🔗 Related Documentation

- `NEXTJS_API_DOCUMENTATION.md` - Hướng dẫn tích hợp với Next.js
- `TOKEN_EXPIRATION_AND_REFRESH.md` - Thông tin về token expiration và refresh
- `FIX_INVALID_GRANT_ERROR.md` - Xử lý lỗi invalid_grant
