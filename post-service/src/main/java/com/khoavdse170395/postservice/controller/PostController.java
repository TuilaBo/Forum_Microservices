package com.khoavdse170395.postservice.controller;

import com.khoavdse170395.postservice.model.dto.CreatePostRequest;
import com.khoavdse170395.postservice.model.dto.CreatePostWithImagesRequest;
import com.khoavdse170395.postservice.model.dto.PostResponse;
import com.khoavdse170395.postservice.model.dto.UpdatePostRequest;
import com.khoavdse170395.postservice.service.CloudinaryService;
import com.khoavdse170395.postservice.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/posts")
@Tag(name = "Post Controller", description = "API quản lý bài viết")
public class PostController {

    @Autowired
    private PostService postService;

    @Autowired
    private CloudinaryService cloudinaryService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Tạo bài viết mới", description = "Tạo một bài viết mới trong diễn đàn. Yêu cầu đăng nhập. ID sẽ tự động được tạo. Có thể gửi imageUrls (URLs từ Cloudinary) trong request body.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Bài viết được tạo thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ"),
            @ApiResponse(responseCode = "401", description = "Chưa đăng nhập")
    })
    public ResponseEntity<PostResponse> createPost(
            @Valid @RequestBody CreatePostRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        String username = jwt.getClaimAsString("preferred_username");
        PostResponse createdPost = postService.createPost(request, userId, username);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdPost);
    }

    @PostMapping(value = "/with-images", consumes = "multipart/form-data")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Tạo bài viết mới kèm ảnh", description = "Tạo bài viết mới và upload ảnh cùng lúc. Yêu cầu đăng nhập. Ảnh sẽ được upload lên Cloudinary tự động.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Bài viết được tạo thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ"),
            @ApiResponse(responseCode = "401", description = "Chưa đăng nhập")
    })
    public ResponseEntity<PostResponse> createPostWithImages(
            @Valid @RequestPart("title") String title,
            @RequestPart("content") String content,
            @RequestPart(value = "images", required = false) List<MultipartFile> images,
            @AuthenticationPrincipal Jwt jwt) {
        
        String userId = jwt.getSubject();
        String username = jwt.getClaimAsString("preferred_username");
        
        CreatePostWithImagesRequest request = new CreatePostWithImagesRequest();
        request.setTitle(title);
        request.setContent(content);
        request.setImages(images);
        
        PostResponse createdPost = postService.createPostWithImages(request, userId, username);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdPost);
    }

    @PostMapping("/upload-images")
    @Operation(summary = "Upload ảnh lên Cloudinary", description = "Upload một hoặc nhiều ảnh lên Cloudinary và trả về URLs")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Upload ảnh thành công"),
            @ApiResponse(responseCode = "400", description = "Lỗi khi upload ảnh")
    })
    public ResponseEntity<List<String>> uploadImages(
            @Parameter(description = "Danh sách file ảnh cần upload") 
            @RequestParam("images") List<MultipartFile> images) {
        List<String> imageUrls = cloudinaryService.uploadImages(images);
        return ResponseEntity.ok(imageUrls);
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Cập nhật bài viết", description = "Cập nhật thông tin của một bài viết theo ID. Chỉ author của bài viết mới được cập nhật.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Bài viết được cập nhật thành công"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy bài viết"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ"),
            @ApiResponse(responseCode = "403", description = "Không có quyền cập nhật bài viết này"),
            @ApiResponse(responseCode = "401", description = "Chưa đăng nhập")
    })
    public ResponseEntity<PostResponse> updatePost(
            @Parameter(description = "ID của bài viết cần cập nhật") @PathVariable Long id,
            @Valid @RequestBody UpdatePostRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        PostResponse updatedPost = postService.updatePost(id, request, userId);
        return ResponseEntity.ok(updatedPost);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Xóa bài viết", description = "Xóa một bài viết theo ID. Chỉ author của bài viết mới được xóa.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Bài viết được xóa thành công"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy bài viết"),
            @ApiResponse(responseCode = "403", description = "Không có quyền xóa bài viết này"),
            @ApiResponse(responseCode = "401", description = "Chưa đăng nhập")
    })
    public ResponseEntity<Void> deletePost(
            @Parameter(description = "ID của bài viết cần xóa") @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        postService.deletePost(id, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy bài viết theo ID", description = "Lấy thông tin chi tiết của một bài viết theo ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tìm thấy bài viết"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy bài viết")
    })
    public ResponseEntity<PostResponse> getPostById(
            @Parameter(description = "ID của bài viết cần lấy") @PathVariable Long id) {
        PostResponse post = postService.getPostById(id);
        return ResponseEntity.ok(post);
    }

    @GetMapping
    @Operation(summary = "Lấy danh sách bài viết", description = "Lấy danh sách tất cả bài viết có phân trang và sắp xếp")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lấy danh sách thành công")
    })
    public ResponseEntity<Page<PostResponse>> getAllPosts(
            @Parameter(description = "Số trang (bắt đầu từ 0)", example = "0") 
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Số lượng bài viết mỗi trang", example = "10") 
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Field để sắp xếp", example = "createdAt") 
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Hướng sắp xếp (ASC hoặc DESC)", example = "DESC") 
            @RequestParam(defaultValue = "DESC") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("ASC") 
                ? Sort.by(sortBy).ascending() 
                : Sort.by(sortBy).descending();
        
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<PostResponse> posts = postService.getAllPosts(pageable);
        
        return ResponseEntity.ok(posts);
    }

    @GetMapping("/my-posts")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Lấy danh sách bài viết của tôi", description = "Lấy danh sách tất cả bài viết của user đang đăng nhập, có phân trang và sắp xếp. Yêu cầu đăng nhập.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lấy danh sách thành công"),
            @ApiResponse(responseCode = "401", description = "Chưa đăng nhập")
    })
    public ResponseEntity<Page<PostResponse>> getMyPosts(
            @Parameter(description = "Số trang (bắt đầu từ 0)", example = "0") 
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Số lượng bài viết mỗi trang", example = "10") 
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Field để sắp xếp", example = "createdAt") 
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Hướng sắp xếp (ASC hoặc DESC)", example = "DESC") 
            @RequestParam(defaultValue = "DESC") String sortDir,
            @AuthenticationPrincipal Jwt jwt) {

        String userId = jwt.getSubject();
        
        Sort sort = sortDir.equalsIgnoreCase("ASC") 
                ? Sort.by(sortBy).ascending() 
                : Sort.by(sortBy).descending();
        
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<PostResponse> posts = postService.getPostsByAuthor(userId, pageable);
        
        return ResponseEntity.ok(posts);
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasRole('MODERATOR')")
    @Operation(summary = "Duyệt bài viết", description = "Duyệt một bài viết. Chỉ moderator mới có quyền này.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Bài viết được duyệt thành công"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy bài viết"),
            @ApiResponse(responseCode = "400", description = "Bài viết đã được duyệt trước đó"),
            @ApiResponse(responseCode = "403", description = "Không có quyền duyệt bài viết (chỉ MODERATOR)"),
            @ApiResponse(responseCode = "401", description = "Chưa đăng nhập")
    })
    public ResponseEntity<PostResponse> approvePost(
            @Parameter(description = "ID của bài viết cần duyệt") @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {
        String moderatorId = jwt.getSubject();
        PostResponse approvedPost = postService.approvePost(id, moderatorId);
        return ResponseEntity.ok(approvedPost);
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasRole('MODERATOR')")
    @Operation(summary = "Từ chối bài viết", description = "Từ chối một bài viết. Chỉ moderator mới có quyền này.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Bài viết bị từ chối thành công"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy bài viết"),
            @ApiResponse(responseCode = "400", description = "Bài viết đã bị từ chối trước đó"),
            @ApiResponse(responseCode = "403", description = "Không có quyền từ chối bài viết (chỉ MODERATOR)"),
            @ApiResponse(responseCode = "401", description = "Chưa đăng nhập")
    })
    public ResponseEntity<PostResponse> rejectPost(
            @Parameter(description = "ID của bài viết cần từ chối") @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {
        String moderatorId = jwt.getSubject();
        PostResponse rejectedPost = postService.rejectPost(id, moderatorId);
        return ResponseEntity.ok(rejectedPost);
    }
}
