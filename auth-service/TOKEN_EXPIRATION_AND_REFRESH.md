# Token Expiration và Refresh Token

## ⏰ Token Expiration

### Thời gian hết hạn mặc định:
- **Access Token**: **300 giây (5 phút)**
- **Refresh Token**: **1800 giây (30 phút)** - có thể cấu hình trong Keycloak

### Kiểm tra token expiration:
Token JWT có field `exp` (expiration time) - Unix timestamp. Bạn có thể decode JWT để xem:
```javascript
// Decode JWT (không cần verify)
const token = "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJ...";
const payload = JSON.parse(atob(token.split('.')[1]));
console.log("Expires at:", new Date(payload.exp * 1000));
console.log("Expires in:", payload.exp - Math.floor(Date.now() / 1000), "seconds");
```

Hoặc dùng `expiresIn` từ response:
```typescript
// Response từ /auth/login hoặc /auth/register
{
  "expiresIn": 300,  // 300 giây = 5 phút
  ...
}
```

---

## 🔄 Refresh Token Flow

### 1. API Endpoint

**POST** `/auth/refresh`

**Request:**
```json
{
  "refreshToken": "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJ..."
}
```

**Response:**
```json
{
  "accessToken": "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJ...",
  "refreshToken": "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJ...",
  "tokenType": "Bearer",
  "expiresIn": 300,
  "user": {
    "id": "...",
    "username": "...",
    "email": "...",
    "roles": ["ROLE_STUDENT"]
  }
}
```

---

## 💻 Cách sử dụng trong Next.js

### 1. Lưu token khi login/register:

```typescript
// pages/api/auth/login.ts hoặc trong component
const response = await fetch('http://localhost:8088/auth/login', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ username, password })
});

const data = await response.json();

// Lưu vào localStorage hoặc state
localStorage.setItem('accessToken', data.accessToken);
localStorage.setItem('refreshToken', data.refreshToken);
localStorage.setItem('tokenExpiresAt', String(Date.now() + data.expiresIn * 1000));
```

### 2. Kiểm tra token hết hạn trước khi gọi API:

```typescript
function isTokenExpired(): boolean {
  const expiresAt = localStorage.getItem('tokenExpiresAt');
  if (!expiresAt) return true;
  
  // Thêm buffer 30 giây để refresh trước khi hết hạn
  return Date.now() >= (parseInt(expiresAt) - 30000);
}
```

### 3. Refresh token tự động:

```typescript
async function refreshAccessToken(): Promise<string | null> {
  const refreshToken = localStorage.getItem('refreshToken');
  if (!refreshToken) {
    // Redirect to login
    window.location.href = '/login';
    return null;
  }

  try {
    const response = await fetch('http://localhost:8088/auth/refresh', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken })
    });

    if (!response.ok) {
      // Refresh token cũng hết hạn, redirect to login
      localStorage.removeItem('accessToken');
      localStorage.removeItem('refreshToken');
      window.location.href = '/login';
      return null;
    }

    const data = await response.json();
    
    // Cập nhật tokens
    localStorage.setItem('accessToken', data.accessToken);
    localStorage.setItem('refreshToken', data.refreshToken);
    localStorage.setItem('tokenExpiresAt', String(Date.now() + data.expiresIn * 1000));
    
    return data.accessToken;
  } catch (error) {
    console.error('Failed to refresh token:', error);
    window.location.href = '/login';
    return null;
  }
}
```

### 4. Axios interceptor để tự động refresh:

```typescript
import axios from 'axios';

const apiClient = axios.create({
  baseURL: 'http://localhost:8088',
});

// Request interceptor: Thêm token vào header
apiClient.interceptors.request.use(
  async (config) => {
    // Kiểm tra token hết hạn
    if (isTokenExpired()) {
      const newToken = await refreshAccessToken();
      if (newToken) {
        config.headers.Authorization = `Bearer ${newToken}`;
      }
    } else {
      const token = localStorage.getItem('accessToken');
      if (token) {
        config.headers.Authorization = `Bearer ${token}`;
      }
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor: Xử lý 401 (Unauthorized)
apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    // Nếu 401 và chưa retry
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;

      // Thử refresh token
      const newToken = await refreshAccessToken();
      if (newToken) {
        originalRequest.headers.Authorization = `Bearer ${newToken}`;
        return apiClient(originalRequest);
      }
    }

    return Promise.reject(error);
  }
);

export default apiClient;
```

---

## 🔧 Cấu hình Token Expiration trong Keycloak

Nếu muốn thay đổi thời gian hết hạn:

1. **Vào Keycloak Admin Console**: `http://localhost:8080`
2. **Chọn realm**: `school-forum`
3. **Realm Settings** → **Tokens** tab
4. **Access Token Lifespan**: Thay đổi từ 5 phút (300s) sang giá trị khác
5. **SSO Session Idle**: Thời gian idle trước khi logout
6. **SSO Session Max**: Thời gian tối đa của session

**Lưu ý:**
- Access Token ngắn hơn → Bảo mật tốt hơn nhưng cần refresh thường xuyên
- Access Token dài hơn → Ít refresh hơn nhưng kém bảo mật hơn
- **Khuyến nghị**: Giữ 5-15 phút cho Access Token, 30 phút - 1 giờ cho Refresh Token

---

## 📝 Tóm tắt

1. ✅ **Access Token hết hạn sau 5 phút** (300 giây)
2. ✅ **Refresh Token hết hạn sau 30 phút** (1800 giây)
3. ✅ **API `/auth/refresh`** để refresh token
4. ✅ **Tự động refresh** trước khi token hết hạn (dùng interceptor)
5. ✅ **Redirect to login** nếu refresh token cũng hết hạn

---

## 🚨 Xử lý lỗi

### Refresh token hết hạn:
```typescript
if (error.response?.status === 401) {
  // Refresh token cũng hết hạn
  localStorage.clear();
  window.location.href = '/login';
}
```

### Network error:
```typescript
try {
  await refreshAccessToken();
} catch (error) {
  // Retry hoặc show error message
  console.error('Network error:', error);
}
```
