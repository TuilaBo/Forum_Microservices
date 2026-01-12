package com.khoavdse170395.userservice.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.khoavdse170395.userservice.service.CloudinaryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
public class CloudinaryServiceImpl implements CloudinaryService {

    private static final Logger logger = LoggerFactory.getLogger(CloudinaryServiceImpl.class);

    @Autowired
    private Cloudinary cloudinary;

    @Override
    public String uploadImage(MultipartFile file) {
        try {
            Map<?, ?> uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", "user-avatars", // Folder trong Cloudinary
                            "resource_type", "image"
                    )
            );
            
            String url = (String) uploadResult.get("secure_url");
            logger.info("Image uploaded successfully: {}", url);
            return url;
        } catch (IOException e) {
            logger.error("Error uploading image to Cloudinary", e);
            throw new RuntimeException("Failed to upload image: " + e.getMessage());
        }
    }

    @Override
    public void deleteImage(String publicId) {
        try {
            // Extract public_id from URL if needed
            String publicIdToDelete = extractPublicIdFromUrl(publicId);
            cloudinary.uploader().destroy(publicIdToDelete, ObjectUtils.emptyMap());
            logger.info("Image deleted successfully: {}", publicIdToDelete);
        } catch (IOException e) {
            logger.error("Error deleting image from Cloudinary", e);
            throw new RuntimeException("Failed to delete image: " + e.getMessage());
        }
    }

    private String extractPublicIdFromUrl(String url) {
        // Cloudinary URL format: https://res.cloudinary.com/{cloud_name}/image/upload/{public_id}
        // Extract public_id from URL
        if (url.contains("/upload/")) {
            String[] parts = url.split("/upload/");
            if (parts.length > 1) {
                String publicIdWithExt = parts[1];
                // Remove file extension
                return publicIdWithExt.substring(0, publicIdWithExt.lastIndexOf('.'));
            }
        }
        return url; // Return as-is if can't extract
    }
}
