package com.khoavdse170395.postservice.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.khoavdse170395.postservice.service.CloudinaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class CloudinaryServiceImpl implements CloudinaryService {

    @Autowired
    private Cloudinary cloudinary;

    @Override
    public String uploadImage(MultipartFile file) {
        try {
            Map<?, ?> uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap("resource_type", "auto")
            );
            return (String) uploadResult.get("secure_url");
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload image to Cloudinary", e);
        }
    }

    @Override
    public List<String> uploadImages(List<MultipartFile> files) {
        List<String> imageUrls = new ArrayList<>();
        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                String url = uploadImage(file);
                imageUrls.add(url);
            }
        }
        return imageUrls;
    }

    @Override
    public void deleteImage(String publicId) {
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete image from Cloudinary", e);
        }
    }
}
