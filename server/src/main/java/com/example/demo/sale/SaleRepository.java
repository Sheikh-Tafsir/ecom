package com.example.demo.sale;

import com.example.demo.common.model.Sale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface SaleRepository extends JpaRepository<Sale, Long> {

    @EntityGraph(attributePaths = {"product", "product.images"})
    @Query("""
                SELECT DISTINCT s
                FROM Sale s
                JOIN s.product p
                WHERE s.createdAt BETWEEN :fromDate AND :toDate
                  AND (:productId IS NULL OR p.id = :productId)
                  AND (:productName IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :productName, '%')))
            """)
    Page<Sale> findAllByMonth(
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            @Param("productId") Long productId,
            @Param("productName") String productName,
            Pageable pageable
    );
}
