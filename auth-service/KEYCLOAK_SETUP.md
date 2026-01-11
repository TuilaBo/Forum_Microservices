# Hướng dẫn cấu hình Keycloak cho Diễn đàn trường

## Bước 1: Cài đặt và chạy Keycloak

### Option 1: Docker (Khuyến nghị)
```bash
docker run -d \
  --name keycloak \
  -p 8080:8080 \
  -e KEYCLOAK_ADMIN=admin \
  -e KEYCLOAK_ADMIN_PASSWORD=admin \
  quay.io/keycloak/keycloak:latest \
  start-dev
```

### Option 2: Download và chạy standalone
1. Tải Keycloak từ: https://www.keycloak.org/downloads
2. Giải nén và chạy:
```bash
cd keycloak-XX.X.X/bin
./standalone.sh  # Linux/Mac
standalone.bat   # Windows
```

3. Truy cập: http://localhost:8080
4. Đăng nhập với admin/admin (hoặc tài khoản bạn đã set)

---

## Bước 2: Tạo Realm "school-forum"

1. Vào **Administration Console** → Click vào dropdown "master" ở góc trên bên trái
2. Chọn **"Create Realm"**
3. Nhập tên realm: `school-forum`
4. Click **"Create"**

---

## Bước 3: Bật User Registration (Đăng ký tự động)

1. Trong realm `school-forum`, vào **Realm Settings** (menu bên trái)
2. Tab **"Login"**
3. Bật toggle **"User registration"** → ON
4. Click **"Save"**

---

## Bước 4: Tạo Client cho Front-end

1. Vào **Clients** (menu bên trái)
2. Click **"Create client"**
3. Điền thông tin:
   - **Client ID**: `forum-frontend`
   - **Client authentication**: OFF (Public client)
   - Click **"Next"**
4. Tab **"Capability config"**:
   - Bật **"Standard flow"** (Authorization Code Flow)
   - Bật **"Direct access grants"** (nếu muốn test với Postman)
   - Click **"Next"**
5. Tab **"Login settings"**:
   - **Root URL**: `http://localhost:8081`
   - **Valid redirect URIs**: `http://localhost:8081/*`
   - **Web origins**: `http://localhost:8081`
   - Click **"Save"**

---

## Bước 5: Tạo Realm Roles

1. Vào **Realm roles** (trong menu **Roles**)
2. Click **"Create role"**
3. Tạo các role sau:
   - `ROLE_STUDENT` - Sinh viên
   - `ROLE_TEACHER` - Giảng viên
   - `ROLE_MODERATOR` - Người điều hành diễn đàn
   - `ROLE_ADMIN` - Quản trị viên

---

## Bước 6: Tạo User Test và gán Role

### Tạo User:
1. Vào **Users** (menu bên trái)
2. Click **"Create new user"**
3. Điền:
   - **Username**: `student1`
   - **Email**: `student1@school.edu`
   - **First name**: `Sinh viên`
   - **Last name**: `Một`
   - Bật **"Email verified"**
   - Click **"Create"**

### Set Password:
1. Vào tab **"Credentials"** của user vừa tạo
2. Click **"Set password"**
3. Nhập password: `password123` (hoặc password bạn muốn)
4. Tắt **"Temporary"** (để không bắt đổi password lần đầu)
5. Click **"Save"**

### Gán Role:
1. Vào tab **"Role mapping"**
2. Click **"Assign role"**
3. Chọn **"Filter by realm roles"**
4. Chọn `ROLE_STUDENT`
5. Click **"Assign"**

---

## Bước 7: Cấu hình Token để include Roles

1. Vào **Clients** → Chọn client `forum-frontend`
2. Tab **"Client scopes"**
3. Vào **"Realm roles"** trong **"Default client scopes"**
4. Đảm bảo **"Add to access token"** = ON
5. Nếu không có, vào **"Client scopes"** → **"roles"** → Tab **"Mappers"** → Kiểm tra mapper **"realm roles"** có **"Add to access token"** = ON

---

## Bước 8: Kiểm tra cấu hình

### Kiểm tra OpenID Configuration:
Truy cập: http://localhost:8080/realms/school-forum/.well-known/openid-configuration

Bạn sẽ thấy JSON với các endpoint, ví dụ:
- `issuer`: `http://localhost:8080/realms/school-forum`
- `authorization_endpoint`: `http://localhost:8080/realms/school-forum/protocol/openid-connect/auth`
- `token_endpoint`: `http://localhost:8080/realms/school-forum/protocol/openid-connect/token`

### Kiểm tra JWKS (Public keys):
Truy cập: http://localhost:8080/realms/school-forum/protocol/openid-connect/certs

---

## Bước 9: Cập nhật application.properties của auth-service

Đảm bảo trong `auth-service/src/main/resources/application.properties` có:

```properties
spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:8080/realms/school-forum
```

---

## Bước 10: Test

1. **Chạy auth-service**:
```bash
cd auth-service
./mvnw spring-boot:run
```

2. **Mở browser**: http://localhost:8081/index.html

3. **Test flow**:
   - Click "Đăng ký" → Tạo user mới trong Keycloak
   - Click "Đăng nhập" → Login với user đã tạo
   - Sau khi login thành công, bạn sẽ thấy token và thông tin user
   - Click "Test với Auth Service" → Gọi API `/auth/me` để verify token

---

## Troubleshooting

### Lỗi "Invalid redirect URI"
- Kiểm tra **Valid redirect URIs** trong client `forum-frontend` phải match với URL bạn đang dùng
- Ví dụ: nếu test ở `http://localhost:8081/index.html` thì redirect URI phải là `http://localhost:8081/*`

### Lỗi "Client not found"
- Kiểm tra Client ID trong HTML (`CLIENT_ID`) phải khớp với client bạn tạo trong Keycloak

### Lỗi "Invalid token" khi test Auth Service
- Kiểm tra `issuer-uri` trong `application.properties` phải khớp với `issuer` trong token
- Decode JWT tại https://jwt.io để xem `iss` claim
- Đảm bảo Keycloak đang chạy và realm `school-forum` tồn tại

### Token không có roles
- Kiểm tra user đã được gán role chưa (tab "Role mapping")
- Kiểm tra client scope "roles" có mapper "realm roles" với "Add to access token" = ON

---

## Tài liệu tham khảo

- Keycloak Documentation: https://www.keycloak.org/documentation
- OpenID Connect: https://openid.net/connect/
- Spring Security OAuth2 Resource Server: https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/index.html

