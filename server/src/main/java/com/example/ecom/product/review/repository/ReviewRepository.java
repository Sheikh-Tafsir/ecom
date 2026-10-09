package com.example.ecom.product.review.repository;

import com.example.ecom.common.model.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID> {

    @EntityGraph(attributePaths = {"user"})
    Page<Review> findAllByProduct_Id(UUID productId, Pageable pageable);

    boolean existsByUser_IdAndProduct_Id(UUID userId, UUID productId);
}
