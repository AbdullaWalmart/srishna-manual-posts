package com.srishna.repository;

import com.srishna.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {

    Page<Post> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<Post> findAllByOrderByCreatedAtDesc();

    @Query("SELECT p FROM Post p WHERE LOWER(p.textContent) LIKE LOWER(CONCAT('%', :q, '%')) ORDER BY p.createdAt DESC")
    Page<Post> searchByText(@Param("q") String query, Pageable pageable);
}
