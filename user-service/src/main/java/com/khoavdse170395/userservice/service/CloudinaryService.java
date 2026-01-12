package com.khoavdse170395.userservice.service;

import org.springframework.web.multipart.MultipartFile;

public interface CloudinaryService {

    String uploadImage(MultipartFile file);

    void deleteImage(String publicId);
}
