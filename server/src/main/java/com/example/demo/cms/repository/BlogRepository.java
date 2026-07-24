package com.example.demo.cms.repository;

import com.example.demo.common.enums.BlogPostStatus;
import com.example.demo.common.model.Blogs;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BlogRepository extends JpaRepository<Blogs, Long> {
    Optional<Blogs> findByTitle(String title);

    Page<Blogs> findAllByStatus(BlogPostStatus status, Pageable pageable);

    Page<Blogs> findAllByTitleContainingIgnoreCaseOrContentContainingIgnoreCase(String title, String content, Pageable pageable);

    Page<Blogs> findAllByStatusAndTitleContainingIgnoreCaseOrStatusAndContentContainingIgnoreCase(
            BlogPostStatus status1, String title, BlogPostStatus status2, String content, Pageable pageable);

    boolean existsByTitle(String title);

    boolean existsByTitleAndIdNot(String title, Long id);
}
