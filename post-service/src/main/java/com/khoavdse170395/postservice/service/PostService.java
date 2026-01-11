package com.khoavdse170395.postservice.service;

import com.khoavdse170395.postservice.model.dto.CreatePostRequest;
import com.khoavdse170395.postservice.model.dto.CreatePostWithImagesRequest;
import com.khoavdse170395.postservice.model.dto.PostResponse;
import com.khoavdse170395.postservice.model.dto.UpdatePostRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PostService {

    PostResponse createPost(CreatePostRequest request, String userId, String username);

    PostResponse createPostWithImages(CreatePostWithImagesRequest request, String userId, String username);

    PostResponse updatePost(Long id, UpdatePostRequest request, String userId);

    void deletePost(Long id, String userId);

    PostResponse getPostById(Long id);

    Page<PostResponse> getAllPosts(Pageable pageable);

    Page<PostResponse> getPostsByAuthor(String authorId, Pageable pageable);

    PostResponse approvePost(Long id, String moderatorId);

    PostResponse rejectPost(Long id, String moderatorId);
}
