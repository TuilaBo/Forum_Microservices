package com.khoavdse170395.commentservice.service.impl;

import com.khoavdse170395.commentservice.kafka.CommentEventProducer;
import com.khoavdse170395.commentservice.model.Comment;
import com.khoavdse170395.commentservice.model.dto.CommentResponse;
import com.khoavdse170395.commentservice.model.dto.CreateCommentRequest;
import com.khoavdse170395.commentservice.model.dto.UpdateCommentRequest;
import com.khoavdse170395.commentservice.model.event.CommentCreatedEvent;
import com.khoavdse170395.commentservice.repository.CommentRepository;
import com.khoavdse170395.commentservice.service.CommentService;
import com.khoavdse170395.commentservice.service.PostServiceClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CommentServiceImpl implements CommentService {

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private CommentEventProducer commentEventProducer;

    @Autowired
    private PostServiceClient postServiceClient;

    @Override
    public CommentResponse createComment(CreateCommentRequest request, String userId, String username) {
        // Lấy postAuthorId từ post-service
        String postAuthorId = postServiceClient.getPostAuthorId(request.getPostId());
        
        // Tạo comment
        Comment comment = new Comment();
        comment.setPostId(request.getPostId());
        comment.setContent(request.getContent());
        comment.setAuthorId(userId);
        comment.setAuthorUsername(username);
        
        Comment savedComment = commentRepository.save(comment);
        
        // Publish CommentCreatedEvent lên Kafka (chỉ khi tạo comment mới)
        CommentCreatedEvent event = new CommentCreatedEvent(
            savedComment.getId(),
            savedComment.getPostId(),
            savedComment.getContent(),
            savedComment.getAuthorId(),
            savedComment.getAuthorUsername(),
            postAuthorId, // ID của người sở hữu post (để gửi notification)
            savedComment.getCreatedAt()
        );
        commentEventProducer.publishCommentCreated(event);
        
        return mapToResponse(savedComment);
    }

    @Override
    public CommentResponse updateComment(Long id, UpdateCommentRequest request, String userId) {
        Comment existingComment = commentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Comment not found with id: " + id));

        // Kiểm tra quyền: chỉ author mới được sửa comment
        if (!existingComment.getAuthorId().equals(userId)) {
            throw new RuntimeException("You do not have permission to update this comment. Only the author can update their own comment.");
        }

        existingComment.setContent(request.getContent());
        existingComment.setUpdatedAt(java.time.LocalDateTime.now());

        Comment updatedComment = commentRepository.save(existingComment);
        
        // KHÔNG publish event khi update (theo yêu cầu)
        
        return mapToResponse(updatedComment);
    }

    @Override
    public void deleteComment(Long id, String userId) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Comment not found with id: " + id));

        // Kiểm tra quyền: chỉ author mới được xóa comment
        if (!comment.getAuthorId().equals(userId)) {
            throw new RuntimeException("You do not have permission to delete this comment. Only the author can delete their own comment.");
        }

        commentRepository.deleteById(id);
        
        // KHÔNG publish event khi delete (theo yêu cầu)
    }

    @Override
    @Transactional(readOnly = true)
    public CommentResponse getCommentById(Long id) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Comment not found with id: " + id));
        return mapToResponse(comment);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CommentResponse> getCommentsByPost(Long postId, Pageable pageable) {
        return commentRepository.findByPostId(postId, pageable).map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CommentResponse> getCommentsByAuthor(String authorId, Pageable pageable) {
        return commentRepository.findByAuthorId(authorId, pageable).map(this::mapToResponse);
    }

    private CommentResponse mapToResponse(Comment comment) {
        CommentResponse response = new CommentResponse();
        response.setId(comment.getId());
        response.setPostId(comment.getPostId());
        response.setContent(comment.getContent());
        response.setAuthorId(comment.getAuthorId());
        response.setAuthorUsername(comment.getAuthorUsername());
        response.setCreatedAt(comment.getCreatedAt());
        response.setUpdatedAt(comment.getUpdatedAt());
        return response;
    }
}
