# KeycloakAdminService - Mục Đích và Cách Hoạt Động

## 🎯 Mục Đích Chính

**KeycloakAdminService** được tạo để **tự động tạo user và gán role trong Keycloak** thay vì phải redirect user đến Keycloak UI để đăng ký thủ công.

---

## ❌ Vấn Đề Khi KHÔNG Có KeycloakAdminService

### Cách 1: Redirect đến Keycloak UI (Cũ)

```java
// AuthServiceImpl.java (CŨ)
public TokenResponse register(RegisterRequest request) {
    // ❌ Chỉ trả về URL để user đăng ký trên Keycloak UI
    String registerUrl = getRegisterUrl(redirectUri);
    throw new UnsupportedOperationException("Use /auth/register-url");
}
```

**Vấn đề:**
- ❌ User phải đăng ký trên Keycloak UI (không phải form của bạn)
- ❌ Không thể tự động gán role ROLE_STUDENT
- ❌ User phải quay lại app sau khi đăng ký
- ❌ Không có control về validation, business logic
- ❌ UX kém (user phải điều hướng giữa 2 trang)

---

## ✅ Giải Pháp: KeycloakAdminService

### Cách 2: Tự Động Tạo User Qua Admin API (Mới)

```java
// AuthServiceImpl.java (MỚI)
public AuthResponse register(RegisterRequest request) {
    // 1. Tạo user trong Keycloak qua Admin API
    String userId = keycloakAdminService.createUser(request);
    
    // 2. Gán role ROLE_STUDENT mặc định
    keycloakAdminService.assignRoleToUser(userId, "ROLE_STUDENT");
    
    // 3. Tự động login và trả về token + user info
    return login(loginRequest);
}
```

**Ưu điểm:**
- ✅ User đăng ký trực tiếp trên form của bạn (Next.js)
- ✅ Tự động gán role ROLE_STUDENT
- ✅ Tự động login sau khi đăng ký
- ✅ Response bao gồm token + user info (không cần redirect)
- ✅ UX tốt (một flow liền mạch)

---

## 🔧 KeycloakAdminService Làm Gì?

### 1. `createUser(RegisterRequest)` - Tạo User Mới

**Mục đích:** Tạo user trong Keycloak qua Admin API.

**Cách hoạt động:**
```java
// 1. Lấy admin token
String adminToken = getAdminAccessToken();

// 2. Gọi Keycloak Admin API
POST /admin/realms/school-forum/users
Authorization: Bearer {adminToken}
Body: {
  "username": "student1",
  "email": "student1@school.edu",
  "password": "password123",
  "firstName": "John",
  "lastName": "Doe",
  "enabled": true,
  "emailVerified": true
}

// 3. Keycloak trả về Location header với user ID
Location: /admin/realms/school-forum/users/{userId}

// 4. Extract user ID và return
return userId;
```

**Tại sao cần admin token?**
- Keycloak Admin API yêu cầu quyền admin để tạo user
- Không thể tạo user bằng client credentials thông thường
- Cần authenticate với admin account (realm `master`)

---

### 2. `assignRoleToUser(String userId, String roleName)` - Gán Role

**Mục đích:** Gán role ROLE_STUDENT cho user mới.

**Cách hoạt động:**
```java
// 1. Lấy role info từ Keycloak
GET /admin/realms/school-forum/roles/ROLE_STUDENT
Authorization: Bearer {adminToken}

// 2. Gán role cho user
POST /admin/realms/school-forum/users/{userId}/role-mappings/realm
Authorization: Bearer {adminToken}
Body: [{role object}]
```

**Tại sao cần gán role?**
- User mới tạo trong Keycloak **KHÔNG có role mặc định**
- Cần gán role ROLE_STUDENT để user có quyền truy cập
- Không thể gán role qua OpenID Connect API (chỉ Admin API)

---

### 3. `getAdminAccessToken()` - Lấy Admin Token

**Mục đích:** Authenticate với Keycloak Admin API.

**Cách hoạt động:**
```java
POST /realms/master/protocol/openid-connect/token
Body: {
  "grant_type": "password",
  "client_id": "admin-cli",
  "username": "admin",
  "password": "admin"
}

Response: {
  "access_token": "...",
  "expires_in": 300
}
```

**Lưu ý:**
- Dùng realm `master` (admin realm)
- Client `admin-cli` là client mặc định cho admin operations
- Token có quyền gọi Admin API

---

### 4. `getUserInfoFromToken(Jwt jwt)` - Extract User Info

**Mục đích:** Parse JWT token để lấy user info và roles.

**Cách hoạt động:**
```java
// JWT token chứa:
{
  "sub": "user-id",
  "preferred_username": "student1",
  "email": "student1@school.edu",
  "given_name": "John",
  "family_name": "Doe",
  "realm_access": {
    "roles": ["ROLE_STUDENT"]
  }
}

// Extract và return Map
```

---

## 📊 So Sánh: Có vs Không Có KeycloakAdminService

| Aspect | Không Có Admin Service | Có Admin Service |
|--------|------------------------|------------------|
| **User đăng ký** | Trên Keycloak UI | Trên form của bạn (Next.js) |
| **Gán role** | Phải làm thủ công | Tự động ROLE_STUDENT |
| **Flow** | Redirect → Keycloak → Redirect lại | Một flow liền mạch |
| **Response** | Chỉ có URL | Token + User info |
| **UX** | Kém (2 trang) | Tốt (1 trang) |
| **Control** | Không có | Full control |

---

## 🔄 Flow Hoàn Chỉnh Khi Register

```
User submit form trên Next.js
  ↓
POST /auth/register
  ↓
AuthServiceImpl.register()
  ├─ KeycloakAdminService.createUser()
  │   ├─ getAdminAccessToken() → Lấy admin token
  │   └─ POST /admin/realms/.../users → Tạo user
  │
  ├─ KeycloakAdminService.assignRoleToUser()
  │   ├─ GET /admin/realms/.../roles/ROLE_STUDENT → Lấy role
  │   └─ POST /admin/realms/.../users/{userId}/role-mappings → Gán role
  │
  └─ login() → Tự động login
      ├─ POST /realms/.../token → Lấy access token
      ├─ Decode JWT → Extract user info
      └─ Return AuthResponse (token + user info)
  ↓
Next.js nhận response
  ├─ Lưu token vào localStorage
  ├─ Lưu user info vào state
  └─ Redirect về trang chủ
```

---

## ⚠️ Lưu Ý Bảo Mật

### 1. Admin Credentials

**Hiện tại:**
```properties
keycloak.admin.username=admin
keycloak.admin.password=admin
```

**Vấn đề:**
- ❌ Hardcode credentials trong code
- ❌ Dễ bị lộ nếu code bị leak

**Giải pháp Production:**
- ✅ Dùng environment variables
- ✅ Hoặc dùng service account với client credentials
- ✅ Hoặc dùng Keycloak Service Account

### 2. Rate Limiting

- Keycloak Admin API có thể bị rate limit
- Nên implement retry mechanism
- Hoặc cache admin token (nhưng phải handle expiration)

---

## ✅ Tóm Tắt

**KeycloakAdminService được tạo để:**

1. ✅ **Tự động tạo user** trong Keycloak (không cần redirect đến Keycloak UI)
2. ✅ **Tự động gán role ROLE_STUDENT** cho user mới
3. ✅ **Cải thiện UX** (user đăng ký trên form của bạn, không phải Keycloak UI)
4. ✅ **Full control** về validation và business logic
5. ✅ **Response format phù hợp** với Next.js (token + user info)

**Không có KeycloakAdminService:**
- ❌ User phải đăng ký trên Keycloak UI
- ❌ Không thể tự động gán role
- ❌ UX kém (redirect giữa các trang)

**Có KeycloakAdminService:**
- ✅ User đăng ký trên form của bạn
- ✅ Tự động gán role ROLE_STUDENT
- ✅ UX tốt (một flow liền mạch)
