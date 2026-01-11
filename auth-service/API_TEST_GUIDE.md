# Hướng dẫn test Auth Service bằng API (Postman/curl)

## Bước 1: Setup Keycloak

Xem file `KEYCLOAK_SETUP.md` để setup Keycloak realm và client.

**Tóm tắt nhanh:**
1. Chạy Keycloak: `docker run -d --name keycloak -p 8080:8080 -e KEYCLOAK_ADMIN=admin -e KEYCLOAK_ADMIN_PASSWORD=admin quay.io/keycloak/keycloak:latest start-dev`
2. Tạo realm `school-forum`
3. Tạo client `forum-frontend` với redirect URI: `http://localhost:8081/auth/callback`
4. Bật User Registration trong realm settings

---

## Bước 2: Chạy Auth Service

```bash
cd auth-service
./mvnw spring-boot:run
```

Service sẽ chạy trên: `http://localhost:8081`

---

## Bước 3: Test các API endpoints

### 3.1. Lấy Login URL và Register URL

**Request:**
```bash
GET http://localhost:8081/auth/login-url
```

**Response:**
```json
{
  "loginUrl": "http://localhost:8080/realms/school-forum/protocol/openid-connect/auth?client_id=forum-frontend&redirect_uri=http://localhost:8081/auth/callback&response_type=code&scope=openid profile email",
  "registerUrl": "http://localhost:8080/realms/school-forum/protocol/openid-connect/registrations?client_id=forum-frontend&redirect_uri=http://localhost:8081/auth/callback&response_type=code&scope=openid profile email",
  "message": "Sử dụng loginUrl để đăng nhập hoặc registerUrl để đăng ký. Sau đó redirect về redirectUri với code parameter."
}
```

**Hoặc với custom redirect URI:**
```bash
GET http://localhost:8081/auth/login-url?redirectUri=http://localhost:3000/callback
```

---

### 3.2. Đăng ký / Đăng nhập với Keycloak

**Cách 1: Dùng browser để lấy code**

1. Copy `loginUrl` hoặc `registerUrl` từ response ở bước 3.1
2. Mở URL đó trong browser
3. Đăng nhập/đăng ký trong Keycloak UI
4. Sau khi thành công, Keycloak sẽ redirect về `redirectUri` với parameter `code`
5. Copy giá trị `code` từ URL

**Ví dụ URL sau redirect:**
```
http://localhost:8081/auth/callback?code=abc123xyz...
```

**Cách 2: Lấy token trực tiếp bằng Direct Access Grant (nếu đã bật)**

Nếu bạn đã bật "Direct access grants" trong Keycloak client, có thể lấy token trực tiếp:

```bash
POST http://localhost:8080/realms/school-forum/protocol/openid-connect/token
Content-Type: application/x-www-form-urlencoded

grant_type=password&client_id=forum-frontend&username=student1&password=password123
```

**Response:**
```json
{
  "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJ...",
  "refresh_token": "...",
  "token_type": "Bearer",
  "expires_in": 300
}
```

---

### 3.3. Đổi code thành token

**Request:**
```bash
POST http://localhost:8081/auth/token
Content-Type: application/x-www-form-urlencoded

code=<CODE_TỪ_BƯỚC_3.2>&redirectUri=http://localhost:8081/auth/callback
```

**Hoặc dùng GET (callback endpoint tự động xử lý):**
```bash
GET http://localhost:8081/auth/callback?code=<CODE_TỪ_BƯỚC_3.2>
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

---

### 3.4. Test validate token với `/auth/me`

**Request:**
```bash
GET http://localhost:8081/auth/me
Authorization: Bearer <ACCESS_TOKEN_TỪ_BƯỚC_3.3>
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

---

## Test với Postman Collection

### Collection JSON (import vào Postman):

```json
{
  "info": {
    "name": "Auth Service Test",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "item": [
    {
      "name": "Get Login URLs",
      "request": {
        "method": "GET",
        "header": [],
        "url": {
          "raw": "http://localhost:8081/auth/login-url",
          "protocol": "http",
          "host": ["localhost"],
          "port": "8081",
          "path": ["auth", "login-url"]
        }
      }
    },
    {
      "name": "Exchange Code for Token",
      "request": {
        "method": "POST",
        "header": [
          {
            "key": "Content-Type",
            "value": "application/x-www-form-urlencoded"
          }
        ],
        "body": {
          "mode": "urlencoded",
          "urlencoded": [
            {
              "key": "code",
              "value": "{{code}}",
              "type": "text"
            },
            {
              "key": "redirectUri",
              "value": "http://localhost:8081/auth/callback",
              "type": "text"
            }
          ]
        },
        "url": {
          "raw": "http://localhost:8081/auth/token",
          "protocol": "http",
          "host": ["localhost"],
          "port": "8081",
          "path": ["auth", "token"]
        }
      }
    },
    {
      "name": "Get Current User (me)",
      "request": {
        "method": "GET",
        "header": [
          {
            "key": "Authorization",
            "value": "Bearer {{accessToken}}",
            "type": "text"
          }
        ],
        "url": {
          "raw": "http://localhost:8081/auth/me",
          "protocol": "http",
          "host": ["localhost"],
          "port": "8081",
          "path": ["auth", "me"]
        }
      }
    }
  ]
}
```

---

## Test với curl

### 1. Lấy login URL:
```bash
curl http://localhost:8081/auth/login-url
```

### 2. Đổi code thành token:
```bash
curl -X POST http://localhost:8081/auth/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "code=<CODE>&redirectUri=http://localhost:8081/auth/callback"
```

### 3. Test /auth/me:
```bash
curl http://localhost:8081/auth/me \
  -H "Authorization: Bearer <ACCESS_TOKEN>"
```

---

## Flow test đầy đủ (Step by step)

### Step 1: Lấy login URL
```bash
curl http://localhost:8081/auth/login-url
```

Copy `loginUrl` từ response.

### Step 2: Mở login URL trong browser
Paste `loginUrl` vào browser, đăng nhập với user đã tạo trong Keycloak.

### Step 3: Copy code từ redirect URL
Sau khi login thành công, browser redirect về:
```
http://localhost:8081/auth/callback?code=abc123xyz...
```

Copy giá trị `code` (phần sau `code=`).

### Step 4: Đổi code thành token
```bash
curl -X POST http://localhost:8081/auth/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "code=<CODE_VỪA_COPY>&redirectUri=http://localhost:8081/auth/callback"
```

Copy `accessToken` từ response.

### Step 5: Test validate token
```bash
curl http://localhost:8081/auth/me \
  -H "Authorization: Bearer <ACCESS_TOKEN>"
```

Bạn sẽ nhận được thông tin user với roles.

---

## Troubleshooting

### Lỗi "Invalid redirect URI"
- Kiểm tra redirect URI trong Keycloak client phải match với `redirectUri` bạn dùng
- Ví dụ: nếu dùng `http://localhost:8081/auth/callback` thì trong Keycloak phải có `http://localhost:8081/*` hoặc chính xác `http://localhost:8081/auth/callback`

### Lỗi "Invalid code"
- Code chỉ dùng được 1 lần và có thời gian hết hạn ngắn (thường vài phút)
- Nếu code đã dùng hoặc hết hạn, phải login lại để lấy code mới

### Lỗi "401 Unauthorized" khi gọi `/auth/me`
- Kiểm tra token còn hiệu lực chưa (check `expiresAt`)
- Kiểm tra format header: `Authorization: Bearer <TOKEN>` (có space sau Bearer)
- Kiểm tra `issuer-uri` trong `application.properties` khớp với issuer trong token

### Token không có roles
- Kiểm tra user đã được gán role trong Keycloak chưa
- Kiểm tra client scope "roles" có mapper "realm roles" với "Add to access token" = ON

---

## Swagger UI

Sau khi chạy auth-service, truy cập Swagger UI:
```
http://localhost:8081/swagger-ui.html
```

Bạn có thể test tất cả endpoints ở đây, nhưng cần:
1. Lấy token trước (bằng cách trên)
2. Click "Authorize" trong Swagger UI
3. Nhập: `Bearer <ACCESS_TOKEN>`
4. Test các endpoints

