# Auth Service API - Next.js Integration Guide

## 📋 Tổng Quan

API format cho Next.js frontend với response bao gồm token và user info.

---

## 🔐 API Endpoints

### 1. Register (Đăng Ký)

**POST** `/auth/register`

**Request Body:**
```json
{
  "username": "student1",
  "email": "student1@school.edu",
  "password": "password123",
  "firstName": "John",
  "lastName": "Doe"
}
```

**Response (200 OK):**
```json
{
  "accessToken": "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJ...",
  "refreshToken": "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJ...",
  "tokenType": "Bearer",
  "expiresIn": 300,
  "user": {
    "id": "b125eb37-6726-45e0-8391-b1e502d24260",
    "username": "student1",
    "email": "student1@school.edu",
    "firstName": "John",
    "lastName": "Doe",
    "roles": ["ROLE_STUDENT"]
  }
}
```

**Lưu ý:**
- User mới được tạo với role **ROLE_STUDENT** mặc định
- Response bao gồm token và user info để frontend có thể lưu ngay

---

### 2. Login (Đăng Nhập)

**POST** `/auth/login`

**Request Body:**
```json
{
  "username": "student1",
  "password": "password123"
}
```

**Response (200 OK):**
```json
{
  "accessToken": "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJ...",
  "refreshToken": "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJ...",
  "tokenType": "Bearer",
  "expiresIn": 300,
  "user": {
    "id": "b125eb37-6726-45e0-8391-b1e502d24260",
    "username": "student1",
    "email": "student1@school.edu",
    "firstName": "John",
    "lastName": "Doe",
    "roles": ["ROLE_STUDENT"]
  }
}
```

---

### 3. Get Current User

**GET** `/auth/me`

**Headers:**
```
Authorization: Bearer {accessToken}
```

**Response (200 OK):**
```json
{
  "id": "b125eb37-6726-45e0-8391-b1e502d24260",
  "username": "student1",
  "email": "student1@school.edu",
  "realmRoles": {
    "roles": ["ROLE_STUDENT"]
  },
  "resourceAccess": {},
  "issuedAt": "2026-01-10T08:05:57Z",
  "expiresAt": "2026-01-10T08:10:57Z"
}
```

---

## 📝 Next.js Integration Example

### 1. Register Function

```typescript
// lib/auth.ts
interface RegisterRequest {
  username: string;
  email: string;
  password: string;
  firstName?: string;
  lastName?: string;
}

interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  user: {
    id: string;
    username: string;
    email: string;
    firstName?: string;
    lastName?: string;
    roles: string[];
  };
}

export async function register(data: RegisterRequest): Promise<AuthResponse> {
  const response = await fetch('http://localhost:8081/auth/register', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(data),
  });

  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.message || 'Registration failed');
  }

  return response.json();
}
```

### 2. Login Function

```typescript
interface LoginRequest {
  username: string;
  password: string;
}

export async function login(data: LoginRequest): Promise<AuthResponse> {
  const response = await fetch('http://localhost:8081/auth/login', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(data),
  });

  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.message || 'Login failed');
  }

  return response.json();
}
```

### 3. Store Token và User Info

```typescript
// app/register/page.tsx hoặc components/RegisterForm.tsx
'use client';

import { useState } from 'react';
import { register } from '@/lib/auth';
import { useRouter } from 'next/navigation';

export default function RegisterForm() {
  const router = useRouter();
  const [formData, setFormData] = useState({
    username: '',
    email: '',
    password: '',
    firstName: '',
    lastName: '',
  });

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      const response = await register(formData);
      
      // Lưu token vào localStorage hoặc cookie
      localStorage.setItem('accessToken', response.accessToken);
      localStorage.setItem('refreshToken', response.refreshToken);
      localStorage.setItem('user', JSON.stringify(response.user));
      
      // Redirect về trang chủ
      router.push('/');
    } catch (error) {
      console.error('Registration failed:', error);
      alert('Registration failed: ' + error.message);
    }
  };

  return (
    <form onSubmit={handleSubmit}>
      {/* Form fields */}
    </form>
  );
}
```

### 4. Protected API Call

```typescript
// lib/api.ts
export async function fetchWithAuth(url: string, options: RequestInit = {}) {
  const token = localStorage.getItem('accessToken');
  
  return fetch(url, {
    ...options,
    headers: {
      ...options.headers,
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json',
    },
  });
}

// Usage
const response = await fetchWithAuth('http://localhost:8082/posts');
const posts = await response.json();
```

---

## 🔒 Security Notes

1. **Token Storage:**
   - **LocalStorage**: Dễ bị XSS attack
   - **HttpOnly Cookie**: An toàn hơn (cần config CORS)
   - **Next.js**: Có thể dùng `next-auth` hoặc custom cookie handling

2. **Token Expiration:**
   - `expiresIn`: 300 giây (5 phút)
   - Cần implement refresh token logic

3. **CORS:**
   - Cần config CORS trong auth-service để cho phép Next.js frontend

---

## ✅ Validation Rules

### Register Request:
- `username`: Required, 3-50 characters
- `email`: Required, valid email format
- `password`: Required, minimum 6 characters
- `firstName`: Optional, max 100 characters
- `lastName`: Optional, max 100 characters

### Login Request:
- `username`: Required
- `password`: Required

---

## 🎯 Response Format Summary

**AuthResponse:**
```typescript
{
  accessToken: string;      // JWT token để gọi API
  refreshToken: string;    // Token để refresh access token
  tokenType: "Bearer";     // Token type
  expiresIn: number;        // Thời gian hết hạn (seconds)
  user: {
    id: string;             // Keycloak User ID
    username: string;       // Username
    email: string;          // Email
    firstName?: string;     // First name (optional)
    lastName?: string;      // Last name (optional)
    roles: string[];        // User roles (mặc định: ["ROLE_STUDENT"])
  }
}
```

---

## 📌 Key Points

1. ✅ **Register tự động gán role ROLE_STUDENT**
2. ✅ **Response bao gồm token + user info** (không cần gọi `/auth/me` sau khi login/register)
3. ✅ **Format phù hợp với Next.js** (có thể lưu trực tiếp vào state/store)
4. ✅ **Validation đầy đủ** (username, email, password)
