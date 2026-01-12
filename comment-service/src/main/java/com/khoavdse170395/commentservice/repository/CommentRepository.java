package com.khoavdse170395.commentservice.repository;

import com.khoavdse170395.commentservice.model.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    Page<Comment> findByPostId(Long postId, Pageable pageable);

    Page<Comment> findByAuthorId(String authorId, Pageable pageable);
}
