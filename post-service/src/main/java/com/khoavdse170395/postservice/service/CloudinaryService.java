package com.khoavdse170395.postservice.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface CloudinaryService {

    String uploadImage(MultipartFile file);

    List<String> uploadImages(List<MultipartFile> files);

    void deleteImage(String publicId);
}
