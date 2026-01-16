package com.khoavdse170395.postservice.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.khoavdse170395.postservice.kafka.PostEventProducer;
import com.khoavdse170395.postservice.model.Post;
import com.khoavdse170395.postservice.model.PostStatus;
import com.khoavdse170395.postservice.model.dto.CreatePostRequest;
import com.khoavdse170395.postservice.model.dto.CreatePostWithImagesRequest;
import com.khoavdse170395.postservice.model.dto.PostResponse;
import com.khoavdse170395.postservice.model.dto.UpdatePostRequest;
import com.khoavdse170395.postservice.model.event.PostCreatedEvent;
import com.khoavdse170395.postservice.model.event.PostDeletedEvent;
import com.khoavdse170395.postservice.model.event.PostUpdatedEvent;
import com.khoavdse170395.postservice.repository.PostRepository;
import com.khoavdse170395.postservice.service.CloudinaryService;
import com.khoavdse170395.postservice.service.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;

@Service
@Transactional
public class PostServiceImpl implements PostService {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private CloudinaryService cloudinaryService;

    @Autowired
    private PostEventProducer postEventProducer;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String CACHE_KEY_PREFIX = "post:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(30);

    @Override
    public PostResponse createPost(CreatePostRequest request, String userId, String username) {
        Post post = new Post();
        post.setTitle(request.getTitle());
        post.setContent(request.getContent());
        // Lấy authorId và username từ JWT (user đã đăng nhập)
        post.setAuthorId(userId);
        post.setAuthorUsername(username);
        post.setImageUrls(request.getImageUrls());
        
        Post savedPost = postRepository.save(post);
        
        // Publish PostCreatedEvent lên Kafka
        PostCreatedEvent event = new PostCreatedEvent(
            savedPost.getId(),
            savedPost.getTitle(),
            savedPost.getContent(),
            savedPost.getAuthorId(),
            savedPost.getAuthorUsername(),
            savedPost.getCreatedAt()
        );
        postEventProducer.publishPostCreated(event);
        
        return mapToResponse(savedPost);
    }

    @Override
    public PostResponse createPostWithImages(CreatePostWithImagesRequest request, String userId, String username) {
        // Upload ảnh lên Cloudinary nếu có
        List<String> imageUrls = null;
        if (request.getImages() != null && !request.getImages().isEmpty()) {
            imageUrls = cloudinaryService.uploadImages(request.getImages());
        }

        // Tạo CreatePostRequest từ CreatePostWithImagesRequest
        CreatePostRequest createRequest = new CreatePostRequest();
        createRequest.setTitle(request.getTitle());
        createRequest.setContent(request.getContent());
        createRequest.setImageUrls(imageUrls);
        // Không set authorId và authorUsername ở đây, sẽ lấy từ JWT trong createPost()

        return createPost(createRequest, userId, username);
    }

    @Override
    public PostResponse updatePost(Long id, UpdatePostRequest request, String userId) {
        Post existingPost = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Post not found with id: " + id));

        // Kiểm tra quyền: chỉ author mới được sửa bài viết
        if (!existingPost.getAuthorId().equals(userId)) {
            throw new RuntimeException("You do not have permission to update this post. Only the author can update their own post.");
        }

        existingPost.setTitle(request.getTitle());
        existingPost.setContent(request.getContent());
        existingPost.setUpdatedAt(java.time.LocalDateTime.now());

        Post updatedPost = postRepository.save(existingPost);
        
        // Publish PostUpdatedEvent lên Kafka
        PostUpdatedEvent event = new PostUpdatedEvent(
            updatedPost.getId(),
            updatedPost.getTitle(),
            updatedPost.getContent(),
            updatedPost.getAuthorId(),
            updatedPost.getUpdatedAt()
        );
        postEventProducer.publishPostUpdated(event);
        
        return mapToResponse(updatedPost);
    }

    @Override
    public void deletePost(Long id, String userId) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Post not found with id: " + id));

        // Kiểm tra quyền: chỉ author mới được xóa bài viết
        if (!post.getAuthorId().equals(userId)) {
            throw new RuntimeException("You do not have permission to delete this post. Only the author can delete their own post.");
        }

        // Lưu thông tin trước khi xóa để publish event
        Long postId = post.getId();
        String authorId = post.getAuthorId();
        
        postRepository.deleteById(id);
        
        // Publish PostDeletedEvent lên Kafka
        PostDeletedEvent event = new PostDeletedEvent(postId, authorId);
        postEventProducer.publishPostDeleted(event);
    }

    @Override
    @Transactional(readOnly = true)
    public PostResponse getPostById(Long id) {
        String cacheKey = CACHE_KEY_PREFIX + id;

        // Check cache first
        Object cachedObj = redisTemplate.opsForValue().get(cacheKey);
        if (cachedObj != null) {
            // Redis deserializes thành LinkedHashMap -> convert sang PostResponse
            PostResponse cached = objectMapper.convertValue(cachedObj, PostResponse.class);
            return cached;
        }

        // Cache miss - query database
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Post not found with id: " + id));
        PostResponse response = mapToResponse(post);

        // Update cache
        redisTemplate.opsForValue().set(cacheKey, response, CACHE_TTL);

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PostResponse> getAllPosts(Pageable pageable) {
        return postRepository.findAll(pageable).map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PostResponse> getPostsByAuthor(String authorId, Pageable pageable) {
        return postRepository.findByAuthorId(authorId, pageable).map(this::mapToResponse);
    }

    @Override
    public PostResponse approvePost(Long id, String moderatorId) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Post not found with id: " + id));

        // Kiểm tra trạng thái hiện tại
        if (post.getStatus() == PostStatus.APPROVED) {
            throw new RuntimeException("Post is already approved");
        }

        post.setStatus(PostStatus.APPROVED);
        post.setUpdatedAt(java.time.LocalDateTime.now());

        Post updatedPost = postRepository.save(post);
        return mapToResponse(updatedPost);
    }

    @Override
    public PostResponse rejectPost(Long id, String moderatorId) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Post not found with id: " + id));

        // Kiểm tra trạng thái hiện tại
        if (post.getStatus() == PostStatus.REJECTED) {
            throw new RuntimeException("Post is already rejected");
        }

        post.setStatus(PostStatus.REJECTED);
        post.setUpdatedAt(java.time.LocalDateTime.now());

        Post updatedPost = postRepository.save(post);
        return mapToResponse(updatedPost);
    }

    private PostResponse mapToResponse(Post post) {
        PostResponse response = new PostResponse();
        response.setId(post.getId());
        response.setTitle(post.getTitle());
        response.setContent(post.getContent());
        response.setAuthorId(post.getAuthorId());
        response.setAuthorUsername(post.getAuthorUsername());
        response.setStatus(post.getStatus());
        response.setImageUrls(post.getImageUrls());
        response.setCreatedAt(post.getCreatedAt());
        response.setUpdatedAt(post.getUpdatedAt());
        return response;
    }
}
