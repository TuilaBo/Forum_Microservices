# API Gateway - Routes Configuration

## 📋 Tổng Quan

API Gateway (port **8088**) route tất cả requests đến các microservices.

---

## 🔌 Routes Configuration

### 1. Auth Service
- **Path:** `/auth/**`
- **Target:** `http://localhost:8081`
- **Endpoints:**
  - `POST /auth/login` - Đăng nhập
  - `POST /auth/register` - Đăng ký
  - `GET /auth/me` - Lấy thông tin user hiện tại

**Example:**
```
http://localhost:8088/auth/login
→ Routes to → http://localhost:8081/auth/login
```

---

### 2. Post Service
- **Path:** `/posts/**`
- **Target:** `http://localhost:8082`
- **Endpoints:**
  - `POST /posts` - Tạo post
  - `GET /posts` - Lấy danh sách posts
  - `GET /posts/{id}` - Lấy post theo ID
  - `PUT /posts/{id}` - Cập nhật post
  - `DELETE /posts/{id}` - Xóa post

**Example:**
```
http://localhost:8088/posts
→ Routes to → http://localhost:8082/posts
```

---

### 3. Comment Service
- **Path:** `/comments/**`
- **Target:** `http://localhost:8083`
- **Endpoints:**
  - `POST /comments` - Tạo comment
  - `GET /comments/post/{postId}` - Lấy comments của post
  - `PUT /comments/{id}` - Cập nhật comment
  - `DELETE /comments/{id}` - Xóa comment

**Example:**
```
http://localhost:8088/comments
→ Routes to → http://localhost:8083/comments
```

---

### 4. User Service
- **Path:** `/users/**`
- **Target:** `http://localhost:8084`
- **Endpoints:**
  - `GET /users/me` - Lấy profile user hiện tại
  - `GET /users/{id}` - Lấy user theo ID
  - `PUT /users/me` - Cập nhật profile
  - `POST /users/me/avatar` - Upload avatar
  - `GET /users/search` - Tìm kiếm users

**Example:**
```
http://localhost:8088/users/me
→ Routes to → http://localhost:8084/users/me
```

---

### 5. Notification Service
- **Path:** `/notifications/**`
- **Target:** `http://localhost:8085`
- **Endpoints:**
  - `GET /notifications` - Lấy danh sách notifications
  - `PUT /notifications/{id}/read` - Đánh dấu đã đọc
  - `GET /notifications/unread-count` - Đếm notifications chưa đọc

**Example:**
```
http://localhost:8088/notifications
→ Routes to → http://localhost:8085/notifications
```

---

## 📊 Service Ports Summary

| Service | Port | Base Path | Gateway Route |
|---------|------|-----------|---------------|
| **API Gateway** | 8088 | `/` | - |
| **Auth Service** | 8081 | `/auth` | `/auth/**` |
| **Post Service** | 8082 | `/posts` | `/posts/**` |
| **Comment Service** | 8083 | `/comments` | `/comments/**` |
| **User Service** | 8084 | `/users` | `/users/**` |
| **Notification Service** | 8085 | `/notifications` | `/notifications/**` |
| **Moderation Service** | - | - | Chưa có controller |

---

## 🔄 Flow Example

### User đăng nhập qua API Gateway:

```
Next.js Frontend
  ↓
POST http://localhost:8088/auth/login
  ↓
API Gateway (port 8088)
  ↓
Routes to: http://localhost:8081/auth/login
  ↓
Auth Service (port 8081)
  ↓
Response: { accessToken, user, ... }
  ↓
API Gateway
  ↓
Next.js Frontend
```

---

## ✅ Testing Routes

### Test Auth Service:
```bash
curl -X POST http://localhost:8088/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"student1","password":"password123"}'
```

### Test Post Service:
```bash
curl http://localhost:8088/posts \
  -H "Authorization: Bearer {token}"
```

### Test Comment Service:
```bash
curl http://localhost:8088/comments/post/1 \
  -H "Authorization: Bearer {token}"
```

### Test User Service:
```bash
curl http://localhost:8088/users/me \
  -H "Authorization: Bearer {token}"
```

### Test Notification Service:
```bash
curl http://localhost:8088/notifications \
  -H "Authorization: Bearer {token}"
```

---

## 🔒 CORS Configuration

API Gateway đã config CORS để cho phép:
- **Origins:** `*` (tất cả)
- **Methods:** GET, POST, PUT, DELETE, OPTIONS, PATCH
- **Headers:** `*` (tất cả)
- **Credentials:** true

---

## 📝 Lưu Ý

1. **Tất cả requests** từ Next.js nên đi qua API Gateway (`http://localhost:8088`)
2. **Không gọi trực tiếp** đến các service (8081, 8082, 8083, 8084, 8085)
3. **API Gateway** sẽ forward requests đến service tương ứng
4. **CORS** được handle ở API Gateway level

---

## 🎯 Next.js Integration

Thay vì gọi trực tiếp:
```typescript
// ❌ Không nên
fetch('http://localhost:8081/auth/login')
fetch('http://localhost:8082/posts')
```

Nên gọi qua API Gateway:
```typescript
// ✅ Nên dùng
const API_BASE_URL = 'http://localhost:8088';

fetch(`${API_BASE_URL}/auth/login`)
fetch(`${API_BASE_URL}/posts`)
fetch(`${API_BASE_URL}/comments`)
fetch(`${API_BASE_URL}/users/me`)
fetch(`${API_BASE_URL}/notifications`)
```
