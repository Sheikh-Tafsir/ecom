package com.example.demo.product.repository;

import com.example.demo.common.enums.ProductStatus;
import com.example.demo.common.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    @EntityGraph(attributePaths = {"images", "categories", "reviews"})
    @Query("select p from Product p where p.id = :id")
    Optional<Product> findDetailsById(Long id);

    @EntityGraph(attributePaths = {"images", "categories"})
    @Query("select p from Product p where p.id = :id")
    Optional<Product> findEditById(Long id);

    @EntityGraph(attributePaths = {"categories"})
    @Query("""
                SELECT DISTINCT p FROM Product p
                LEFT JOIN p.categories c
                WHERE (:name IS NULL OR p.name ILIKE CONCAT('%', CAST(:name AS string), '%'))
                AND (:category IS NULL OR c.name = :category)
                AND (:status IS NULL OR p.status <> :status)
                AND p.createdAt BETWEEN :fromDate AND :toDate
            """)
    Page<Product> findAll(@Param("name") String name,
                          @Param("category") String category,
                          @Param("status") ProductStatus status,
                          @Param("fromDate") LocalDateTime fromDate,
                          @Param("toDate") LocalDateTime toDate,
                          Pageable pageable
    );
}
