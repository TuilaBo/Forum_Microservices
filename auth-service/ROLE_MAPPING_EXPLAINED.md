# Role Mapping - Giải thích chi tiết

## Vấn đề: Keycloak vs Spring Security Role Format

### Keycloak trả về:
```json
{
  "realm_access": {
    "roles": ["STUDENT", "TEACHER", "MODERATOR"]
  }
}
```

### Spring Security cần:
```java
// Spring Security yêu cầu roles phải có prefix "ROLE_"
hasRole("STUDENT")  // ❌ KHÔNG hoạt động
hasRole("ROLE_STUDENT")  // ✅ Hoạt động
```

---

## Tại sao cần JwtAuthenticationConverter?

**Spring Security mặc định:**
- Chỉ đọc `scope` claim từ JWT
- Không đọc `realm_access.roles` từ Keycloak
- Không tự động thêm prefix `ROLE_`

**Keycloak:**
- Trả về roles trong `realm_access.roles`
- Không có prefix `ROLE_` (theo chuẩn OIDC)

**→ Cần custom converter để bridge giữa 2 bên**

---

## Giải pháp: JwtAuthConverter

### File: `security/JwtAuthConverter.java`

**Chức năng:**
1. Extract roles từ `realm_access.roles` trong JWT
2. Extract roles từ `resource_access.<client_id>.roles` (nếu có)
3. Thêm prefix `ROLE_` vào mỗi role
4. Convert thành `GrantedAuthority` cho Spring Security

### Code flow:

```java
JWT từ Keycloak
    ↓
JwtAuthConverter.convert(Jwt jwt)
    ↓
extractKeycloakRoles(jwt)
    ↓
Đọc realm_access.roles: ["STUDENT", "TEACHER"]
    ↓
ensureRolePrefix("STUDENT") → "ROLE_STUDENT"
ensureRolePrefix("TEACHER") → "ROLE_TEACHER"
    ↓
Tạo SimpleGrantedAuthority("ROLE_STUDENT")
Tạo SimpleGrantedAuthority("ROLE_TEACHER")
    ↓
JwtAuthenticationToken với authorities
```

---

## Cách sử dụng trong SecurityConfig

### Trước (KHÔNG hoạt động):
```java
.oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
```

**Vấn đề:** Spring Security không đọc roles từ Keycloak, chỉ đọc scope.

### Sau (HOẠT ĐỘNG):
```java
.oauth2ResourceServer(oauth2 -> oauth2
    .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthConverter))
);
```

**Kết quả:** Spring Security đọc roles từ Keycloak và convert đúng format.

---

## Test Role Mapping

### 1. Kiểm tra roles trong token

Decode JWT tại https://jwt.io, xem payload:
```json
{
  "realm_access": {
    "roles": ["STUDENT", "TEACHER"]
  }
}
```

### 2. Test với @PreAuthorize

```java
@GetMapping("/admin-only")
@PreAuthorize("hasRole('ADMIN')")
public String adminOnly() {
    return "Admin only";
}
```

**Nếu role trong Keycloak là `ADMIN` (không có prefix):**
- `JwtAuthConverter` sẽ convert thành `ROLE_ADMIN`
- `hasRole('ADMIN')` sẽ match với `ROLE_ADMIN` → ✅ Hoạt động

### 3. Test với SecurityConfig

```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/admin/**").hasRole("ADMIN")
    .requestMatchers("/student/**").hasRole("STUDENT")
)
```

**Nếu role trong Keycloak là `STUDENT`:**
- `JwtAuthConverter` convert thành `ROLE_STUDENT`
- `hasRole("STUDENT")` match với `ROLE_STUDENT` → ✅ Hoạt động

---

## Các trường hợp xử lý

### 1. Role đã có prefix ROLE_
```java
ensureRolePrefix("ROLE_STUDENT") → "ROLE_STUDENT" (giữ nguyên)
```

### 2. Role chưa có prefix
```java
ensureRolePrefix("STUDENT") → "ROLE_STUDENT" (thêm prefix)
```

### 3. Realm roles
```json
"realm_access": {
  "roles": ["STUDENT", "TEACHER"]
}
```
→ Convert thành: `ROLE_STUDENT`, `ROLE_TEACHER`

### 4. Client roles
```json
"resource_access": {
  "forum-service": {
    "roles": ["POST_CREATE", "POST_DELETE"]
  }
}
```
→ Convert thành: `ROLE_POST_CREATE`, `ROLE_POST_DELETE`

---

## Tóm tắt

| Aspect | Trước | Sau |
|--------|-------|-----|
| **Spring Security đọc roles?** | ❌ Không (chỉ đọc scope) | ✅ Có (đọc realm_access.roles) |
| **Role format** | `STUDENT` | `ROLE_STUDENT` |
| **hasRole() hoạt động?** | ❌ Không | ✅ Có |
| **@PreAuthorize hoạt động?** | ❌ Không | ✅ Có |

---

## Checklist

- [x] Tạo `JwtAuthConverter` class
- [x] Extract roles từ `realm_access.roles`
- [x] Extract roles từ `resource_access.<client_id>.roles`
- [x] Thêm prefix `ROLE_` vào mỗi role
- [x] Convert thành `GrantedAuthority`
- [x] Inject vào `SecurityConfig`
- [x] Test với `hasRole()` và `@PreAuthorize`

---

## Kết luận

**JwtAuthenticationConverter là BẮT BUỘC** khi dùng Keycloak với Spring Security.

Không có nó:
- Spring Security không đọc roles từ Keycloak
- `hasRole()`, `@PreAuthorize` không hoạt động
- Phân quyền không work

Có nó:
- Spring Security đọc roles từ Keycloak
- `hasRole()`, `@PreAuthorize` hoạt động đúng
- Phân quyền work hoàn hảo

**Đây là phần 90% người học bị fail - nhưng bạn đã làm đúng rồi! ✅**
