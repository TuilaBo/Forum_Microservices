# Fix lỗi "Invalid grant" / "Invalid user credentials"

## 🔴 Lỗi:
```
401 Unauthorized: "{"error":"invalid_grant","error_description":"Invalid user credentials"}"
```

## ✅ Giải pháp:

### 1. Bật "Direct Access Grants" trong Keycloak Client

**Bước 1:** Vào Keycloak Admin Console
- URL: `http://localhost:8080`
- Login: `admin` / `admin`
- Chọn realm: `school-forum`

**Bước 2:** Cấu hình Client
1. Vào **Clients** → Chọn client `forum-frontend`
2. Tab **"Capability config"** (hoặc **"Settings"**)
3. Tìm **"Direct access grants"** → Bật toggle **ON**
4. Click **"Save"**

**Giải thích:** 
- "Direct access grants" cho phép client gọi token endpoint với `grant_type=password`
- Không có cấu hình này, Keycloak sẽ từ chối request với username/password trực tiếp

---

### 2. Kiểm tra User tồn tại và có password

**Bước 1:** Kiểm tra user trong Keycloak
1. Vào **Users** → Tìm user bạn đang login
2. Kiểm tra:
   - ✅ User **Enabled** = ON
   - ✅ User có **Email verified** = ON (nếu cần)
   - ✅ User có **password được set**

**Bước 2:** Set password cho user (nếu chưa có)
1. Vào user → Tab **"Credentials"**
2. Click **"Set password"**
3. Nhập password → **"Set password"**
4. **Tắt** toggle **"Temporary"** (nếu không muốn user phải đổi password lần đầu)

---

### 3. Kiểm tra Username/Password đúng

**Test trực tiếp với Keycloak:**
```bash
curl -X POST http://localhost:8080/realms/school-forum/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password&client_id=forum-frontend&username=YOUR_USERNAME&password=YOUR_PASSWORD"
```

**Nếu vẫn lỗi:**
- Kiểm tra username có đúng không (case-sensitive)
- Kiểm tra password có đúng không
- Kiểm tra user có bị disabled không

---

### 4. Kiểm tra Client Configuration

**Trong Keycloak Client `forum-frontend`:**

**Tab "Settings":**
- ✅ **Access Type**: `public` hoặc `confidential`
- ✅ **Direct Access Grants Enabled**: `ON`
- ✅ **Standard Flow Enabled**: `ON` (nếu dùng authorization code flow)

**Tab "Capability config":**
- ✅ **Direct access grants**: `ON`

---

## 🧪 Test sau khi fix:

```bash
# Test login qua API Gateway
curl -X POST http://localhost:8088/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"student1","password":"password123"}'
```

**Response mong đợi:**
```json
{
  "accessToken": "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJ...",
  "refreshToken": "...",
  "tokenType": "Bearer",
  "expiresIn": 300,
  "user": { ... }
}
```

---

## 📝 Checklist:

- [ ] Keycloak đang chạy trên port 8080
- [ ] Client `forum-frontend` có **Direct access grants** = ON
- [ ] User tồn tại trong realm `school-forum`
- [ ] User có password được set
- [ ] User **Enabled** = ON
- [ ] Username/password đúng (case-sensitive)
- [ ] Auth-service đang chạy trên port 8081
- [ ] API Gateway đang chạy trên port 8088

---

## 🔍 Debug thêm:

**Xem logs Keycloak:**
```bash
# Nếu Keycloak chạy bằng Docker
docker logs <keycloak-container-id>

# Hoặc xem file logs trong Keycloak server
tail -f /path/to/keycloak/logs/server.log
```

**Test trực tiếp Keycloak token endpoint:**
```bash
curl -v -X POST http://localhost:8080/realms/school-forum/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password&client_id=forum-frontend&username=student1&password=password123"
```

Nếu test này thành công nhưng qua auth-service vẫn lỗi → Kiểm tra config trong `application.properties`
