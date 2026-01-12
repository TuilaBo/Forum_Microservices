# CORS Fix Summary - Network Error Resolution

## ✅ Đã Sửa

### 1. **API Gateway CORS Config** (`CorsConfig.java`)
- ✅ Dùng `setAllowedOriginPatterns` với wildcard `http://localhost:*`
- ✅ Cho phép tất cả headers với `*`
- ✅ Đã thêm OPTIONS method
- ✅ `allowCredentials: true`

### 2. **Removed Duplicate CORS Config**
- ✅ Commented out CORS config trong `application.properties` để tránh conflict

### 3. **CORS Preflight Test**
- ✅ OPTIONS request trả về đúng CORS headers
- ✅ `Access-Control-Allow-Origin: http://localhost:3000`
- ✅ `Access-Control-Allow-Credentials: true`

## 🔍 Vấn Đề "Network Error" từ Next.js

"Network Error" từ Axios có thể do:

### 1. **Axios Config Thiếu**
Đảm bảo Axios config đúng:

```typescript
// ✅ Đúng
const axiosInstance = axios.create({
  baseURL: 'http://localhost:8088',
  headers: {
    'Content-Type': 'application/json',
  },
  withCredentials: true, // Quan trọng nếu dùng credentials
});

// Hoặc trong request
axios.post('http://localhost:8088/auth/login', {
  username: 'student1',
  password: 'password123'
}, {
  headers: {
    'Content-Type': 'application/json',
  },
  withCredentials: true,
});
```

### 2. **Browser Console Check**
Mở Browser DevTools → Network tab và kiểm tra:
- Request có được gửi không?
- Status code là gì?
- Response headers có CORS headers không?
- Có error message gì trong Console không?

### 3. **CORS Headers Check**
Response phải có:
```
Access-Control-Allow-Origin: http://localhost:3000
Access-Control-Allow-Credentials: true
Access-Control-Allow-Methods: GET,POST,PUT,DELETE,OPTIONS,PATCH
Access-Control-Allow-Headers: *
```

### 4. **Common Issues**

#### Issue 1: Mixed Content
Nếu Next.js chạy HTTPS nhưng API là HTTP → Browser sẽ block

**Fix:** Đảm bảo cả hai đều HTTP hoặc cả hai đều HTTPS

#### Issue 2: Port Mismatch
Next.js đang chạy ở port khác 3000

**Fix:** Update CORS config để match port thực tế, hoặc dùng wildcard `http://localhost:*`

#### Issue 3: Axios Timeout
Request timeout trước khi nhận được response

**Fix:** Tăng timeout:
```typescript
axios.post(url, data, {
  timeout: 30000, // 30 seconds
});
```

#### Issue 4: Service Not Running
API Gateway hoặc Auth Service không chạy

**Fix:** 
```bash
# Check services
netstat -ano | findstr ":8088"  # API Gateway
netstat -ano | findstr ":8081"  # Auth Service
```

## 🧪 Test Commands

### Test CORS Preflight:
```bash
curl -v -X OPTIONS http://localhost:8088/auth/login \
  -H "Origin: http://localhost:3000" \
  -H "Access-Control-Request-Method: POST" \
  -H "Access-Control-Request-Headers: Content-Type"
```

### Test Actual Request:
```bash
curl -v -X POST http://localhost:8088/auth/login \
  -H "Content-Type: application/json" \
  -H "Origin: http://localhost:3000" \
  -d '{"username":"student1","password":"password123"}'
```

## 📝 Next.js Code Example

```typescript
// authService.ts
import axios from 'axios';

const API_BASE_URL = 'http://localhost:8088';

const axiosInstance = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  withCredentials: true,
  timeout: 30000,
});

export const login = async (username: string, password: string) => {
  try {
    const response = await axiosInstance.post('/auth/login', {
      username,
      password,
    });
    return response.data;
  } catch (error: any) {
    if (error.response) {
      // Server responded with error
      throw new Error(error.response.data?.message || 'Login failed');
    } else if (error.request) {
      // Request was made but no response received
      throw new Error('Network error: No response from server');
    } else {
      // Something else happened
      throw new Error(error.message || 'An error occurred');
    }
  }
};
```

## ✅ Verification Checklist

- [ ] API Gateway đang chạy trên port 8088
- [ ] Auth Service đang chạy trên port 8081
- [ ] CORS preflight (OPTIONS) trả về đúng headers
- [ ] Next.js đang chạy trên port 3000 (hoặc match với CORS config)
- [ ] Axios config có `withCredentials: true` nếu cần
- [ ] Browser Console không có CORS errors
- [ ] Network tab trong DevTools hiển thị request đúng

## 🐛 Debug Steps

1. **Mở Browser DevTools** → Network tab
2. **Thử login** từ Next.js
3. **Kiểm tra request** trong Network tab:
   - Request URL đúng không?
   - Request headers có `Origin: http://localhost:3000` không?
   - Response status code là gì?
   - Response headers có CORS headers không?
4. **Kiểm tra Console** tab:
   - Có CORS errors không?
   - Có network errors không?
   - Error message cụ thể là gì?

## 📞 Nếu Vẫn Lỗi

1. **Copy exact error message** từ Browser Console
2. **Copy request/response** từ Network tab
3. **Kiểm tra**:
   - Services đang chạy?
   - Ports đúng không?
   - CORS config match với Next.js origin không?
