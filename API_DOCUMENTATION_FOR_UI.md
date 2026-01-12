# API Documentation for UI Team

## 📋 Tổng Quan

Tài liệu này mô tả tất cả các API endpoints của **Post Service** và **Notification Service** để UI team có thể tích hợp.

**Base URL:** `http://localhost:8088` (qua API Gateway)

**Authentication:** Tất cả endpoints (trừ một số public) yêu cầu JWT token trong header:
```
Authorization: Bearer {access_token}
```

---

## 📌 POST SERVICE APIs

### Base Path: `/posts`

---

### 1. Tạo Bài Viết Mới (với imageUrls)

**Endpoint:** `POST /posts`

**Authentication:** ✅ Required

**Request Body:**
```json
{
  "title": "Tiêu đề bài viết",
  "content": "Nội dung bài viết",
  "imageUrls": [
    "https://res.cloudinary.com/.../image1.jpg",
    "https://res.cloudinary.com/.../image2.jpg"
  ]
}
```

**Request Fields:**
- `title` (string, required, max 200 chars): Tiêu đề bài viết
- `content` (string, required): Nội dung bài viết
- `imageUrls` (array of strings, optional): Danh sách URLs ảnh từ Cloudinary

**Response Codes:**
- `201 Created`: Bài viết được tạo thành công
- `400 Bad Request`: Dữ liệu không hợp lệ (validation errors)
- `401 Unauthorized`: Chưa đăng nhập hoặc token không hợp lệ

**Success Response (201):**
```json
{
  "id": 1,
  "title": "Tiêu đề bài viết",
  "content": "Nội dung bài viết",
  "authorId": "user-uuid-123",
  "authorUsername": "student1",
  "status": "PENDING",
  "imageUrls": [
    "https://res.cloudinary.com/.../image1.jpg",
    "https://res.cloudinary.com/.../image2.jpg"
  ],
  "createdAt": "2026-01-11T10:30:00",
  "updatedAt": "2026-01-11T10:30:00"
}
```

**Error Response (400):**
```json
{
  "timestamp": "2026-01-11T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Title is required",
  "path": "/posts"
}
```

---

### 2. Tạo Bài Viết Mới Kèm Upload Ảnh

**Endpoint:** `POST /posts/with-images`

**Authentication:** ✅ Required

**Content-Type:** `multipart/form-data`

**Request Body (Form Data):**
- `title` (string, required): Tiêu đề bài viết
- `content` (string, required): Nội dung bài viết
- `images` (file[], optional): Danh sách file ảnh cần upload

**Response Codes:**
- `201 Created`: Bài viết được tạo thành công
- `400 Bad Request`: Dữ liệu không hợp lệ
- `401 Unauthorized`: Chưa đăng nhập

**Success Response (201):**
```json
{
  "id": 2,
  "title": "Tiêu đề bài viết",
  "content": "Nội dung bài viết",
  "authorId": "user-uuid-123",
  "authorUsername": "student1",
  "status": "PENDING",
  "imageUrls": [
    "https://res.cloudinary.com/.../uploaded_image1.jpg",
    "https://res.cloudinary.com/.../uploaded_image2.jpg"
  ],
  "createdAt": "2026-01-11T10:35:00",
  "updatedAt": "2026-01-11T10:35:00"
}
```

---

### 3. Upload Ảnh Lên Cloudinary

**Endpoint:** `POST /posts/upload-images`

**Authentication:** ❌ Not Required

**Content-Type:** `multipart/form-data`

**Request Body (Form Data):**
- `images` (file[], required): Danh sách file ảnh cần upload

**Response Codes:**
- `200 OK`: Upload ảnh thành công
- `400 Bad Request`: Lỗi khi upload ảnh

**Success Response (200):**
```json
[
  "https://res.cloudinary.com/dyrksdywm/image/upload/v1234567890/image1.jpg",
  "https://res.cloudinary.com/dyrksdywm/image/upload/v1234567890/image2.jpg"
]
```

---

### 4. Cập Nhật Bài Viết

**Endpoint:** `PUT /posts/{id}`

**Authentication:** ✅ Required (chỉ author mới được cập nhật)

**Path Parameters:**
- `id` (long, required): ID của bài viết cần cập nhật

**Request Body:**
```json
{
  "title": "Tiêu đề đã cập nhật",
  "content": "Nội dung đã cập nhật"
}
```

**Request Fields:**
- `title` (string, required, max 200 chars): Tiêu đề mới
- `content` (string, required): Nội dung mới

**Response Codes:**
- `200 OK`: Bài viết được cập nhật thành công
- `400 Bad Request`: Dữ liệu không hợp lệ
- `401 Unauthorized`: Chưa đăng nhập
- `403 Forbidden`: Không có quyền cập nhật bài viết này (không phải author)
- `404 Not Found`: Không tìm thấy bài viết

**Success Response (200):**
```json
{
  "id": 1,
  "title": "Tiêu đề đã cập nhật",
  "content": "Nội dung đã cập nhật",
  "authorId": "user-uuid-123",
  "authorUsername": "student1",
  "status": "PENDING",
  "imageUrls": ["https://..."],
  "createdAt": "2026-01-11T10:30:00",
  "updatedAt": "2026-01-11T11:00:00"
}
```

---

### 5. Xóa Bài Viết

**Endpoint:** `DELETE /posts/{id}`

**Authentication:** ✅ Required (chỉ author mới được xóa)

**Path Parameters:**
- `id` (long, required): ID của bài viết cần xóa

**Response Codes:**
- `204 No Content`: Bài viết được xóa thành công
- `401 Unauthorized`: Chưa đăng nhập
- `403 Forbidden`: Không có quyền xóa bài viết này (không phải author)
- `404 Not Found`: Không tìm thấy bài viết

**Success Response (204):**
```
(No response body)
```

---

### 6. Lấy Bài Viết Theo ID

**Endpoint:** `GET /posts/{id}`

**Authentication:** ❌ Not Required

**Path Parameters:**
- `id` (long, required): ID của bài viết cần lấy

**Response Codes:**
- `200 OK`: Tìm thấy bài viết
- `404 Not Found`: Không tìm thấy bài viết

**Success Response (200):**
```json
{
  "id": 1,
  "title": "Tiêu đề bài viết",
  "content": "Nội dung bài viết",
  "authorId": "user-uuid-123",
  "authorUsername": "student1",
  "status": "APPROVED",
  "imageUrls": ["https://..."],
  "createdAt": "2026-01-11T10:30:00",
  "updatedAt": "2026-01-11T10:30:00"
}
```

**PostStatus Values:**
- `PENDING`: Chờ duyệt
- `APPROVED`: Đã duyệt
- `REJECTED`: Đã từ chối

---

### 7. Lấy Danh Sách Tất Cả Bài Viết

**Endpoint:** `GET /posts`

**Authentication:** ❌ Not Required

**Query Parameters:**
- `page` (int, default: 0): Số trang (bắt đầu từ 0)
- `size` (int, default: 10): Số lượng bài viết mỗi trang
- `sortBy` (string, default: "createdAt"): Field để sắp xếp (ví dụ: "createdAt", "title")
- `sortDir` (string, default: "DESC"): Hướng sắp xếp ("ASC" hoặc "DESC")

**Example Request:**
```
GET /posts?page=0&size=20&sortBy=createdAt&sortDir=DESC
```

**Response Codes:**
- `200 OK`: Lấy danh sách thành công

**Success Response (200):**
```json
{
  "content": [
    {
      "id": 1,
      "title": "Bài viết 1",
      "content": "Nội dung...",
      "authorId": "user-uuid-123",
      "authorUsername": "student1",
      "status": "APPROVED",
      "imageUrls": [],
      "createdAt": "2026-01-11T10:30:00",
      "updatedAt": "2026-01-11T10:30:00"
    },
    {
      "id": 2,
      "title": "Bài viết 2",
      "content": "Nội dung...",
      "authorId": "user-uuid-456",
      "authorUsername": "student2",
      "status": "PENDING",
      "imageUrls": [],
      "createdAt": "2026-01-11T09:00:00",
      "updatedAt": "2026-01-11T09:00:00"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20,
    "sort": {
      "sorted": true,
      "unsorted": false,
      "empty": false
    }
  },
  "totalElements": 50,
  "totalPages": 3,
  "last": false,
  "size": 20,
  "number": 0,
  "sort": {
    "sorted": true,
    "unsorted": false,
    "empty": false
  },
  "first": true,
  "numberOfElements": 20,
  "empty": false
}
```

---

### 8. Lấy Danh Sách Bài Viết Của Tôi

**Endpoint:** `GET /posts/my-posts`

**Authentication:** ✅ Required

**Query Parameters:**
- `page` (int, default: 0): Số trang (bắt đầu từ 0)
- `size` (int, default: 10): Số lượng bài viết mỗi trang
- `sortBy` (string, default: "createdAt"): Field để sắp xếp
- `sortDir` (string, default: "DESC"): Hướng sắp xếp ("ASC" hoặc "DESC")

**Response Codes:**
- `200 OK`: Lấy danh sách thành công
- `401 Unauthorized`: Chưa đăng nhập

**Success Response (200):**
```json
{
  "content": [
    {
      "id": 1,
      "title": "Bài viết của tôi",
      "content": "Nội dung...",
      "authorId": "user-uuid-123",
      "authorUsername": "student1",
      "status": "APPROVED",
      "imageUrls": [],
      "createdAt": "2026-01-11T10:30:00",
      "updatedAt": "2026-01-11T10:30:00"
    }
  ],
  "pageable": { ... },
  "totalElements": 5,
  "totalPages": 1,
  "last": true,
  "size": 10,
  "number": 0,
  "sort": { ... },
  "first": true,
  "numberOfElements": 5,
  "empty": false
}
```

---

### 9. Duyệt Bài Viết (Moderator Only)

**Endpoint:** `PUT /posts/{id}/approve`

**Authentication:** ✅ Required (chỉ MODERATOR)

**Path Parameters:**
- `id` (long, required): ID của bài viết cần duyệt

**Response Codes:**
- `200 OK`: Bài viết được duyệt thành công
- `400 Bad Request`: Bài viết đã được duyệt trước đó
- `401 Unauthorized`: Chưa đăng nhập
- `403 Forbidden`: Không có quyền duyệt bài viết (chỉ MODERATOR)
- `404 Not Found`: Không tìm thấy bài viết

**Success Response (200):**
```json
{
  "id": 1,
  "title": "Tiêu đề bài viết",
  "content": "Nội dung bài viết",
  "authorId": "user-uuid-123",
  "authorUsername": "student1",
  "status": "APPROVED",
  "imageUrls": [],
  "createdAt": "2026-01-11T10:30:00",
  "updatedAt": "2026-01-11T11:00:00"
}
```

---

### 10. Từ Chối Bài Viết (Moderator Only)

**Endpoint:** `PUT /posts/{id}/reject`

**Authentication:** ✅ Required (chỉ MODERATOR)

**Path Parameters:**
- `id` (long, required): ID của bài viết cần từ chối

**Response Codes:**
- `200 OK`: Bài viết bị từ chối thành công
- `400 Bad Request`: Bài viết đã bị từ chối trước đó
- `401 Unauthorized`: Chưa đăng nhập
- `403 Forbidden`: Không có quyền từ chối bài viết (chỉ MODERATOR)
- `404 Not Found`: Không tìm thấy bài viết

**Success Response (200):**
```json
{
  "id": 1,
  "title": "Tiêu đề bài viết",
  "content": "Nội dung bài viết",
  "authorId": "user-uuid-123",
  "authorUsername": "student1",
  "status": "REJECTED",
  "imageUrls": [],
  "createdAt": "2026-01-11T10:30:00",
  "updatedAt": "2026-01-11T11:00:00"
}
```

---

## 🔔 NOTIFICATION SERVICE APIs

### Base Path: `/notifications`

---

### 1. Lấy Danh Sách Notifications

**Endpoint:** `GET /notifications`

**Authentication:** ✅ Required

**Query Parameters:**
- `page` (int, default: 0): Số trang (bắt đầu từ 0)
- `size` (int, default: 10): Số lượng notifications mỗi trang

**Example Request:**
```
GET /notifications?page=0&size=20
```

**Response Codes:**
- `200 OK`: Lấy danh sách thành công
- `401 Unauthorized`: Chưa đăng nhập

**Success Response (200):**
```json
{
  "content": [
    {
      "id": 1,
      "userId": "user-uuid-123",
      "type": "COMMENT_ON_POST",
      "title": "Có comment mới trên bài viết của bạn",
      "message": "student2 đã comment vào bài viết của bạn: \"Đây là comment...\"",
      "relatedPostId": 5,
      "relatedCommentId": 10,
      "isRead": false,
      "createdAt": "2026-01-11T12:00:00"
    },
    {
      "id": 2,
      "userId": "user-uuid-123",
      "type": "COMMENT_ON_POST",
      "title": "Có comment mới trên bài viết của bạn",
      "message": "student3 đã comment vào bài viết của bạn: \"Comment khác...\"",
      "relatedPostId": 5,
      "relatedCommentId": 11,
      "isRead": true,
      "createdAt": "2026-01-11T11:00:00"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20,
    "sort": {
      "sorted": true,
      "unsorted": false,
      "empty": false
    }
  },
  "totalElements": 15,
  "totalPages": 1,
  "last": true,
  "size": 20,
  "number": 0,
  "sort": { ... },
  "first": true,
  "numberOfElements": 15,
  "empty": false
}
```

**Notification Types:**
- `COMMENT_ON_POST`: Có comment mới trên bài viết của bạn

---

### 2. Đánh Dấu Notification Đã Đọc

**Endpoint:** `PUT /notifications/{id}/read`

**Authentication:** ✅ Required

**Path Parameters:**
- `id` (long, required): ID của notification cần đánh dấu đã đọc

**Response Codes:**
- `204 No Content`: Đánh dấu thành công
- `401 Unauthorized`: Chưa đăng nhập
- `404 Not Found`: Không tìm thấy notification

**Success Response (204):**
```
(No response body)
```

---

### 3. Đếm Số Notifications Chưa Đọc

**Endpoint:** `GET /notifications/unread-count`

**Authentication:** ✅ Required

**Response Codes:**
- `200 OK`: Lấy số lượng thành công
- `401 Unauthorized`: Chưa đăng nhập

**Success Response (200):**
```json
5
```

---

### 4. Health Check

**Endpoint:** `GET /notifications/health`

**Authentication:** ❌ Not Required

**Response Codes:**
- `200 OK`: Service đang chạy

**Success Response (200):**
```
"Notification Service is running"
```

---

## 📊 Response Codes Summary

### Success Codes
- `200 OK`: Request thành công
- `201 Created`: Resource được tạo thành công
- `204 No Content`: Request thành công nhưng không có response body

### Client Error Codes
- `400 Bad Request`: Dữ liệu không hợp lệ hoặc validation errors
- `401 Unauthorized`: Chưa đăng nhập hoặc token không hợp lệ
- `403 Forbidden`: Không có quyền thực hiện action này
- `404 Not Found`: Không tìm thấy resource

### Server Error Codes
- `500 Internal Server Error`: Lỗi server

---

## 🔐 Authentication Flow

1. **Login** qua `/auth/login` để nhận `accessToken`
2. **Gửi token** trong header mỗi request:
   ```
   Authorization: Bearer {accessToken}
   ```
3. **Token expires** sau một khoảng thời gian (thường 5 phút), cần login lại hoặc refresh token

---

## 📝 Notes cho UI Team

1. **Base URL:** Luôn sử dụng `http://localhost:8088` (API Gateway) thay vì gọi trực tiếp đến các service
2. **Pagination:** Tất cả endpoints trả về danh sách đều hỗ trợ phân trang với `page` và `size`
3. **Sorting:** Endpoints danh sách bài viết hỗ trợ sắp xếp với `sortBy` và `sortDir`
4. **Image Upload:** Có 2 cách:
   - Upload trước qua `/posts/upload-images` → lấy URLs → tạo post với `imageUrls`
   - Upload cùng lúc khi tạo post qua `/posts/with-images`
5. **Error Handling:** Luôn check `status` code và handle errors phù hợp
6. **Date Format:** Tất cả dates đều ở format ISO 8601: `"2026-01-11T10:30:00"`
7. **Post Status:** 
   - `PENDING`: Chờ duyệt (chỉ author và moderator thấy)
   - `APPROVED`: Đã duyệt (mọi người thấy)
   - `REJECTED`: Đã từ chối (chỉ author và moderator thấy)

---

## 🧪 Example Requests

### Tạo bài viết với imageUrls:
```javascript
fetch('http://localhost:8088/posts', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': 'Bearer ' + accessToken
  },
  body: JSON.stringify({
    title: 'Tiêu đề bài viết',
    content: 'Nội dung bài viết',
    imageUrls: ['https://res.cloudinary.com/.../image1.jpg']
  })
})
```

### Lấy danh sách notifications:
```javascript
fetch('http://localhost:8088/notifications?page=0&size=20', {
  method: 'GET',
  headers: {
    'Authorization': 'Bearer ' + accessToken
  }
})
```

### Đánh dấu notification đã đọc:
```javascript
fetch('http://localhost:8088/notifications/1/read', {
  method: 'PUT',
  headers: {
    'Authorization': 'Bearer ' + accessToken
  }
})
```

---

**Last Updated:** 2026-01-11
