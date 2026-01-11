# Realm và Client trong Keycloak - Giải thích chi tiết

## 1. REALM là gì và tại sao cần nó?

### Realm = "Vương quốc" độc lập của users và applications

**Realm** trong Keycloak giống như một **database riêng biệt** cho users, roles, clients, và cấu hình bảo mật.

### Tại sao cần Realm?

1. **Phân tách hoàn toàn giữa các hệ thống khác nhau:**
   - Realm `school-forum` → chỉ chứa users/roles cho diễn đàn trường
   - Realm `e-commerce` → chứa users/roles cho hệ thống thương mại điện tử
   - Realm `hr-system` → chứa users/roles cho hệ thống nhân sự
   - **Users trong realm này KHÔNG THỂ login vào realm kia** (trừ khi bạn config cross-realm)

2. **Mỗi realm có cấu hình riêng:**
   - Password policy (độ dài, độ phức tạp)
   - Token expiration time
   - Login themes (giao diện đăng nhập)
   - Email settings
   - Social login providers (Google, Facebook...)

3. **Realm = Namespace cho URLs:**
   - Mỗi realm có URL riêng: `http://keycloak:8080/realms/school-forum`
   - Token từ realm này có `iss` (issuer) = `http://keycloak:8080/realms/school-forum`
   - auth-service của bạn chỉ validate token từ realm `school-forum` (config trong `application.properties`)

### Trong project của bạn:

```
Keycloak Server
├── Realm: master (mặc định, dùng cho admin)
└── Realm: school-forum (realm bạn tạo cho diễn đàn)
    ├── Users: student1, teacher1, admin1...
    ├── Roles: ROLE_STUDENT, ROLE_TEACHER, ROLE_MODERATOR...
    ├── Clients: forum-frontend, api-gateway...
    └── Settings: password policy, token expiration...
```

**Khi user login vào diễn đàn:**
- User chỉ tồn tại trong realm `school-forum`
- Token được phát từ realm `school-forum`
- Token có `iss` = `http://localhost:8080/realms/school-forum`
- auth-service chỉ chấp nhận token từ issuer này

---

## 2. CLIENT là gì và tại sao cần nó?

### Client = Application muốn sử dụng Keycloak để authenticate users

**Client** trong Keycloak đại diện cho một **application** (front-end, backend service, mobile app...) muốn dùng Keycloak để xác thực.

### Tại sao cần Client?

1. **Mỗi application có cấu hình riêng:**
   - **Redirect URIs**: Keycloak chỉ redirect về các URL được phép
   - **Client type**: Public (SPA, mobile) vs Confidential (backend service)
   - **Scopes**: Application này cần quyền gì (openid, profile, email...)
   - **Token settings**: Access token lifetime, refresh token...

2. **Bảo mật:**
   - Public client (SPA): Không có secret, dùng PKCE
   - Confidential client: Có client secret, dùng để authenticate chính client đó

3. **Client Roles (phân quyền theo application):**
   - Realm roles: `ROLE_STUDENT`, `ROLE_TEACHER` (áp dụng cho toàn realm)
   - Client roles: `POST_CREATE`, `POST_DELETE` (chỉ áp dụng cho client `post-service`)

### Trong project của bạn:

```
Realm: school-forum
├── Client: forum-frontend (Public client)
│   ├── Type: Public (SPA)
│   ├── Redirect URIs: http://localhost:8081/auth/callback
│   ├── Scopes: openid, profile, email
│   └── Dùng để: Front-end đăng nhập user
│
├── Client: api-gateway (Confidential client - tùy chọn)
│   ├── Type: Confidential
│   ├── Client Secret: abc123...
│   ├── Redirect URIs: http://localhost:8080/callback
│   └── Dùng để: Gateway authenticate riêng (nếu cần)
│
└── Client: post-service (Resource client - tùy chọn)
    ├── Type: Resource (không login, chỉ để định nghĩa client roles)
    └── Dùng để: Định nghĩa roles như POST_CREATE, POST_DELETE
```

---

## 3. Flow hoạt động: Realm + Client + auth-service

### Scenario: User đăng nhập vào diễn đàn

**Bước 1: User click "Đăng nhập"**
```
Front-end (forum-frontend) → Gọi API: GET /auth/login-url
```

**Bước 2: auth-service trả về login URL**
```
Response: {
  "loginUrl": "http://localhost:8080/realms/school-forum/protocol/openid-connect/auth
              ?client_id=forum-frontend
              &redirect_uri=http://localhost:8081/auth/callback
              &response_type=code
              &scope=openid profile email"
}
```

**Giải thích URL:**
- `/realms/school-forum` → **Realm** bạn muốn login vào
- `client_id=forum-frontend` → **Client** (application) đang request login
- `redirect_uri` → URL Keycloak sẽ redirect về sau khi login (phải match với config trong client)
- `scope` → Quyền bạn muốn lấy (openid = identity, profile = username/name, email = email)

**Bước 3: User nhập username/password trong Keycloak UI**
- Keycloak kiểm tra user có tồn tại trong realm `school-forum` không
- Keycloak kiểm tra password đúng không
- Keycloak kiểm tra client `forum-frontend` có quyền redirect về `redirect_uri` không

**Bước 4: Keycloak redirect về với code**
```
http://localhost:8081/auth/callback?code=abc123xyz...
```

**Bước 5: Front-end đổi code thành token**
```
POST /auth/token
Body: code=abc123xyz...&redirectUri=http://localhost:8081/auth/callback
```

**Bước 6: auth-service gọi Keycloak để đổi code**
```
POST http://localhost:8080/realms/school-forum/protocol/openid-connect/token
Body: grant_type=authorization_code&code=...&client_id=forum-frontend&redirect_uri=...
```

**Keycloak kiểm tra:**
- Code có hợp lệ không (chưa dùng, chưa hết hạn)
- `client_id` có match với code không
- `redirect_uri` có match với code không
- User có quyền truy cập client này không

**Bước 7: Keycloak trả về access token (JWT)**
```json
{
  "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJ...",
  "token_type": "Bearer",
  "expires_in": 300
}
```

**Token này chứa:**
- `iss`: `http://localhost:8080/realms/school-forum` (realm)
- `sub`: User ID trong realm `school-forum`
- `preferred_username`: Username
- `email`: Email
- `realm_access.roles`: `["ROLE_STUDENT"]` (realm roles)
- `resource_access.forum-frontend.roles`: `[]` (client roles, nếu có)

**Bước 8: Front-end gọi API với token**
```
GET /auth/me
Authorization: Bearer <access_token>
```

**Bước 9: auth-service validate token**
- Spring Security đọc `issuer-uri` từ config: `http://localhost:8080/realms/school-forum`
- Spring Security gọi Keycloak để lấy public keys (JWKS)
- Spring Security verify signature của token
- Spring Security check `iss` trong token phải = `http://localhost:8080/realms/school-forum`
- Spring Security check `exp` (token chưa hết hạn)

**Bước 10: auth-service trả về user info**
```json
{
  "id": "1234-5678-...",
  "username": "student1",
  "email": "student1@school.edu",
  "realmRoles": {"roles": ["ROLE_STUDENT"]},
  "resourceAccess": {}
}
```

---

## 4. Tại sao cần tạo Realm và Client trên UI?

### Realm `school-forum`:
- **Tách biệt users/roles** của diễn đàn khỏi các hệ thống khác
- **Cấu hình riêng** cho diễn đàn (password policy, token expiration...)
- **URL riêng** để auth-service biết validate token từ đâu

### Client `forum-frontend`:
- **Định nghĩa application** muốn dùng Keycloak
- **Bảo mật redirect URIs**: Chỉ cho phép redirect về URLs được phép
- **Cấu hình scopes**: Application này cần quyền gì
- **Client type**: Public (không cần secret) cho SPA

---

## 5. So sánh Realm vs Client

| Aspect | Realm | Client |
|--------|-------|--------|
| **Mục đích** | Container cho users, roles, clients | Đại diện cho một application |
| **Số lượng** | Thường ít (1-5 realms) | Nhiều (mỗi app = 1 client) |
| **Users** | Users thuộc về realm | Users không thuộc về client |
| **Roles** | Realm roles (ROLE_STUDENT) | Client roles (POST_CREATE) |
| **URL** | `/realms/{realm-name}` | Không có URL riêng |
| **Token issuer** | `iss` trong token = realm URL | `aud` trong token = client_id |

---

## 6. Best Practices cho project của bạn

### Realm:
- ✅ Tạo 1 realm `school-forum` cho toàn bộ hệ thống diễn đàn
- ✅ Không tạo nhiều realms (trừ khi bạn có nhiều hệ thống hoàn toàn độc lập)

### Clients:
- ✅ `forum-frontend`: Public client cho front-end SPA
- ✅ `api-gateway`: Confidential client (nếu gateway cần authenticate riêng)
- ✅ `post-service`, `comment-service`: Resource clients để định nghĩa client roles (tùy chọn)

### Roles:
- ✅ Realm roles: `ROLE_STUDENT`, `ROLE_TEACHER`, `ROLE_MODERATOR`, `ROLE_ADMIN`
- ✅ Client roles (nếu cần): `POST_CREATE`, `POST_DELETE`, `COMMENT_CREATE`...

---

## 7. Tóm tắt: Realm và Client làm gì?

**Realm `school-forum`:**
- Chứa tất cả users của diễn đàn
- Chứa tất cả roles (realm roles)
- Chứa tất cả clients (applications)
- Cấu hình bảo mật riêng cho diễn đàn
- Token từ realm này có issuer = `http://localhost:8080/realms/school-forum`

**Client `forum-frontend`:**
- Đại diện cho front-end application
- Định nghĩa redirect URIs được phép
- Định nghĩa scopes cần thiết
- Public client (không cần secret cho SPA)

**Khi user login:**
1. User login vào **realm** `school-forum`
2. Thông qua **client** `forum-frontend`
3. Nhận token từ realm `school-forum`
4. Token được validate bởi auth-service (check issuer = realm URL)
5. Token chứa roles từ realm và client (nếu có)

---

## 8. Checklist khi tạo Realm và Client

### Khi tạo Realm `school-forum`:
- [ ] Tên realm: `school-forum`
- [ ] Bật "User registration" (nếu muốn user tự đăng ký)
- [ ] Cấu hình password policy
- [ ] Cấu hình token expiration (mặc định 5 phút cho access token)

### Khi tạo Client `forum-frontend`:
- [ ] Client ID: `forum-frontend`
- [ ] Client authentication: OFF (Public client)
- [ ] Standard flow: ON
- [ ] Valid redirect URIs: `http://localhost:8081/*` (hoặc chính xác `http://localhost:8081/auth/callback`)
- [ ] Web origins: `http://localhost:8081`
- [ ] Scopes: `openid`, `profile`, `email`

### Sau khi tạo:
- [ ] Test login URL: `GET http://localhost:8081/auth/login-url`
- [ ] Test login flow end-to-end
- [ ] Verify token có đúng `iss` = realm URL
- [ ] Verify token có roles nếu user đã được gán role

---

## Kết luận

**Realm** = "Vương quốc" độc lập cho users và applications  
**Client** = Application muốn dùng Keycloak để authenticate

Trong project của bạn:
- **Realm `school-forum`**: Chứa users, roles, clients của diễn đàn
- **Client `forum-frontend`**: Front-end application đăng nhập user
- **auth-service**: Validate token từ realm `school-forum`

Tất cả hoạt động cùng nhau để tạo ra một hệ thống xác thực tập trung, an toàn cho diễn đàn trường học của bạn.

