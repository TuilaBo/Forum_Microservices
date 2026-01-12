# API Upload Avatar - Hướng Dẫn Sử Dụng

## 📋 Tổng Quan

API này cho phép user upload avatar từ máy tính của họ. Avatar sẽ được lưu trên Cloudinary và URL sẽ được lưu trong database.

## 🔗 Endpoint

```
POST /users/me/avatar
```

**Base URL:** `http://localhost:8088/users/me/avatar` (qua API Gateway)

## 🔐 Authentication

- **Required:** Có
- **Type:** Bearer Token (JWT)
- **Header:** `Authorization: Bearer {access_token}`

## 📤 Request

### Headers
```
Authorization: Bearer {access_token}
Content-Type: multipart/form-data
```

### Body (Form Data)
- **Field name:** `file`
- **Type:** File (image)
- **Accepted formats:** JPEG, PNG, GIF, WEBP
- **Max size:** 5MB

### Ví dụ với cURL
```bash
curl -X POST http://localhost:8088/users/me/avatar \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -F "file=@/path/to/image.jpg"
```

### Ví dụ với JavaScript (Axios)
```javascript
import axios from 'axios';

const uploadAvatar = async (file, accessToken) => {
  const formData = new FormData();
  formData.append('file', file);

  try {
    const response = await axios.post(
      'http://localhost:8088/users/me/avatar',
      formData,
      {
        headers: {
          'Authorization': `Bearer ${accessToken}`,
          'Content-Type': 'multipart/form-data'
        }
      }
    );
    return response.data;
  } catch (error) {
    console.error('Error uploading avatar:', error.response?.data);
    throw error;
  }
};

// Sử dụng
const fileInput = document.querySelector('input[type="file"]');
fileInput.addEventListener('change', async (e) => {
  const file = e.target.files[0];
  if (file) {
    try {
      const result = await uploadAvatar(file, accessToken);
      console.log('Avatar uploaded:', result);
    } catch (error) {
      alert('Lỗi upload avatar: ' + error.response?.data?.message);
    }
  }
});
```

### Ví dụ với React + Axios
```jsx
import React, { useState } from 'react';
import axios from 'axios';

const AvatarUpload = () => {
  const [file, setFile] = useState(null);
  const [preview, setPreview] = useState(null);
  const [loading, setLoading] = useState(false);
  const accessToken = localStorage.getItem('accessToken');

  const handleFileChange = (e) => {
    const selectedFile = e.target.files[0];
    
    // Validate file type
    if (!selectedFile.type.startsWith('image/')) {
      alert('Vui lòng chọn file ảnh');
      return;
    }
    
    // Validate file size (5MB)
    if (selectedFile.size > 5 * 1024 * 1024) {
      alert('Kích thước file không được vượt quá 5MB');
      return;
    }
    
    setFile(selectedFile);
    
    // Preview image
    const reader = new FileReader();
    reader.onloadend = () => {
      setPreview(reader.result);
    };
    reader.readAsDataURL(selectedFile);
  };

  const handleUpload = async () => {
    if (!file) return;
    
    setLoading(true);
    const formData = new FormData();
    formData.append('file', file);

    try {
      const response = await axios.post(
        'http://localhost:8088/users/me/avatar',
        formData,
        {
          headers: {
            'Authorization': `Bearer ${accessToken}`,
            'Content-Type': 'multipart/form-data'
          }
        }
      );
      
      alert('Upload avatar thành công!');
      console.log('Avatar URL:', response.data.avatarUrl);
      // Có thể update state hoặc reload user info
    } catch (error) {
      const errorMessage = error.response?.data?.message || 'Lỗi upload avatar';
      alert(errorMessage);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      <input
        type="file"
        accept="image/*"
        onChange={handleFileChange}
      />
      {preview && (
        <div>
          <img src={preview} alt="Preview" style={{ maxWidth: '200px' }} />
          <button onClick={handleUpload} disabled={loading}>
            {loading ? 'Đang upload...' : 'Upload Avatar'}
          </button>
        </div>
      )}
    </div>
  );
};

export default AvatarUpload;
```

## 📥 Response

### Success (200 OK)
```json
{
  "id": "c4144f5a-0226-4fd4-a596-e9d0da3959b7",
  "username": "student1",
  "email": "student1@school.edu",
  "firstName": "John",
  "lastName": "Doe",
  "bio": null,
  "avatarUrl": "https://res.cloudinary.com/dyrksdywm/image/upload/v1234567890/user-avatars/abc123.jpg",
  "createdAt": "2026-01-12T10:00:00",
  "updatedAt": "2026-01-12T11:00:00"
}
```

### Error Responses

#### 400 Bad Request - File không hợp lệ
```json
{
  "timestamp": "2026-01-12T11:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "File phải là ảnh (JPEG, PNG, GIF, WEBP)"
}
```

#### 400 Bad Request - File quá lớn
```json
{
  "timestamp": "2026-01-12T11:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Kích thước file không được vượt quá 5MB"
}
```

#### 400 Bad Request - File trống
```json
{
  "timestamp": "2026-01-12T11:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "File không được để trống"
}
```

#### 401 Unauthorized
```json
{
  "timestamp": "2026-01-12T11:00:00",
  "status": 401,
  "error": "Unauthorized",
  "message": "Token không hợp lệ hoặc đã hết hạn"
}
```

#### 500 Internal Server Error
```json
{
  "timestamp": "2026-01-12T11:00:00",
  "status": 500,
  "error": "Internal Server Error",
  "message": "Failed to upload image: ..."
}
```

## ✅ Validation Rules

1. **File Type:** Chỉ chấp nhận file ảnh (JPEG, PNG, GIF, WEBP)
2. **File Size:** Tối đa 5MB
3. **Authentication:** Phải có JWT token hợp lệ
4. **User:** User phải tồn tại trong database (sẽ tự động tạo nếu chưa có)

## 🔄 Flow

1. User chọn file ảnh từ máy tính
2. Frontend validate file (type, size)
3. Frontend gửi POST request với FormData
4. Backend validate lại file
5. Backend upload lên Cloudinary
6. Backend lưu URL vào database
7. Backend trả về thông tin user đã cập nhật

## 📝 Lưu Ý

- Avatar cũ sẽ tự động bị xóa khi upload avatar mới
- Avatar được lưu trong folder `user-avatars` trên Cloudinary
- URL avatar là HTTPS và có thể truy cập công khai
- Nếu upload thất bại, avatar cũ vẫn được giữ nguyên

## 🧪 Test với Postman

1. **Method:** POST
2. **URL:** `http://localhost:8088/users/me/avatar`
3. **Headers:**
   - `Authorization: Bearer {your_token}`
4. **Body:**
   - Chọn tab `form-data`
   - Key: `file` (chọn type là `File`)
   - Value: Chọn file ảnh từ máy tính
5. **Send**

## 🔗 Related APIs

- `GET /users/me` - Lấy thông tin user hiện tại (bao gồm avatarUrl)
- `DELETE /users/me/avatar` - Xóa avatar
- `PUT /users/me` - Cập nhật thông tin user khác
