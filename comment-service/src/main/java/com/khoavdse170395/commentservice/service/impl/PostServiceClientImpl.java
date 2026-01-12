package com.khoavdse170395.commentservice.service.impl;

import com.khoavdse170395.commentservice.service.PostServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Implementation của PostServiceClient sử dụng RestTemplate.
 * 
 * Gọi REST API đến post-service để lấy thông tin post.
 */
@Service
public class PostServiceClientImpl implements PostServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(PostServiceClientImpl.class);

    @Value("${post-service.url:http://localhost:8082}")
    private String postServiceUrl;

    private final RestTemplate restTemplate;

    public PostServiceClientImpl() {
        this.restTemplate = new RestTemplate();
    }

    @Override
    @SuppressWarnings("unchecked")
    public String getPostAuthorId(Long postId) {
        try {
            String url = postServiceUrl + "/posts/" + postId;
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> post = (Map<String, Object>) response.getBody();
                return (String) post.get("authorId");
            }
            
            logger.warn("Post not found or invalid response for postId: {}", postId);
            return null;
        } catch (Exception e) {
            logger.error("Error calling post-service for postId: {}", postId, e);
            // Trả về null nếu không lấy được, nhưng vẫn cho phép tạo comment
            // Có thể implement retry mechanism hoặc fallback sau
            return null;
        }
    }
}
