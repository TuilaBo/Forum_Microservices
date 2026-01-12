package com.khoavdse170395.commentservice.service;

import com.khoavdse170395.commentservice.model.dto.CommentResponse;
import com.khoavdse170395.commentservice.model.dto.CreateCommentRequest;
import com.khoavdse170395.commentservice.model.dto.UpdateCommentRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CommentService {

    CommentResponse createComment(CreateCommentRequest request, String userId, String username);

    CommentResponse updateComment(Long id, UpdateCommentRequest request, String userId);

    void deleteComment(Long id, String userId);

    CommentResponse getCommentById(Long id);

    Page<CommentResponse> getCommentsByPost(Long postId, Pageable pageable);

    Page<CommentResponse> getCommentsByAuthor(String authorId, Pageable pageable);
}
