package com.khoavdse170395.postservice.repository;

import com.khoavdse170395.postservice.model.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    Page<Post> findAll(Pageable pageable);

    Page<Post> findByAuthorId(String authorId, Pageable pageable);
}
