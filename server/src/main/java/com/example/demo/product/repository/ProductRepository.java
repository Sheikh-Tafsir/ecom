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

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    @EntityGraph(attributePaths = {"images", "categorise"})
    @Query("""
                SELECT DISTINCT p FROM Product p
                LEFT JOIN p.categorise c
                WHERE (:name IS NULL OR p.name ILIKE CONCAT('%', CAST(:name AS string), '%'))
                AND (:category IS NULL OR c.name = :category)
                AND (:status IS NULL OR p.status <> :status)
            """)
    Page<Product> findAllByNameAndExcludeStatus(
            @Param("name") String name,
            @Param("category") String category,
            @Param("status") ProductStatus status,
            Pageable pageable
    );
}
