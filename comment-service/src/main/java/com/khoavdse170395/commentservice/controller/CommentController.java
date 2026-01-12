package com.khoavdse170395.commentservice.controller;

import com.khoavdse170395.commentservice.model.dto.CommentResponse;
import com.khoavdse170395.commentservice.model.dto.CreateCommentRequest;
import com.khoavdse170395.commentservice.model.dto.UpdateCommentRequest;
import com.khoavdse170395.commentservice.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/comments")
@Tag(name = "Comment Controller", description = "API quản lý comments")
public class CommentController {

    @Autowired
    private CommentService commentService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Tạo comment mới", description = "Tạo một comment mới cho bài viết. Yêu cầu đăng nhập.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Comment được tạo thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ"),
            @ApiResponse(responseCode = "401", description = "Chưa đăng nhập")
    })
    public ResponseEntity<CommentResponse> createComment(
            @Valid @RequestBody CreateCommentRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        String username = jwt.getClaimAsString("preferred_username");
        CommentResponse createdComment = commentService.createComment(request, userId, username);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdComment);
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Cập nhật comment", description = "Cập nhật nội dung comment. Chỉ author của comment mới được cập nhật.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Comment được cập nhật thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ"),
            @ApiResponse(responseCode = "401", description = "Chưa đăng nhập"),
            @ApiResponse(responseCode = "403", description = "Không có quyền cập nhật comment này"),
            @ApiResponse(responseCode = "404", description = "Comment không tồn tại")
    })
    public ResponseEntity<CommentResponse> updateComment(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCommentRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        CommentResponse updatedComment = commentService.updateComment(id, request, userId);
        return ResponseEntity.ok(updatedComment);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Xóa comment", description = "Xóa comment. Chỉ author của comment mới được xóa.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Comment được xóa thành công"),
            @ApiResponse(responseCode = "401", description = "Chưa đăng nhập"),
            @ApiResponse(responseCode = "403", description = "Không có quyền xóa comment này"),
            @ApiResponse(responseCode = "404", description = "Comment không tồn tại")
    })
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        commentService.deleteComment(id, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy comment theo ID", description = "Lấy thông tin chi tiết của một comment theo ID. Public endpoint.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lấy comment thành công"),
            @ApiResponse(responseCode = "404", description = "Comment không tồn tại")
    })
    public ResponseEntity<CommentResponse> getCommentById(@PathVariable Long id) {
        CommentResponse comment = commentService.getCommentById(id);
        return ResponseEntity.ok(comment);
    }

    @GetMapping("/post/{postId}")
    @Operation(summary = "Lấy tất cả comments của post", description = "Lấy danh sách comments của một post với phân trang. Public endpoint.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lấy danh sách comments thành công")
    })
    public ResponseEntity<Page<CommentResponse>> getCommentsByPost(
            @PathVariable Long postId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {
        
        Sort sort = sortDir.equalsIgnoreCase("ASC") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<CommentResponse> comments = commentService.getCommentsByPost(postId, pageable);
        return ResponseEntity.ok(comments);
    }

    @GetMapping("/user/{authorId}")
    @Operation(summary = "Lấy tất cả comments của user", description = "Lấy danh sách comments của một user với phân trang. Public endpoint.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lấy danh sách comments thành công")
    })
    public ResponseEntity<Page<CommentResponse>> getCommentsByAuthor(
            @PathVariable String authorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {
        
        Sort sort = sortDir.equalsIgnoreCase("ASC") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<CommentResponse> comments = commentService.getCommentsByAuthor(authorId, pageable);
        return ResponseEntity.ok(comments);
    }
}
