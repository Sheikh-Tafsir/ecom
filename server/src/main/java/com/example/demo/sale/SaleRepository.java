package com.example.demo.sale;

import com.example.demo.common.model.Sale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface SaleRepository extends JpaRepository<Sale, Long> {

    @Query("""
                SELECT DISTINCT s
                FROM Sale s
                JOIN FETCH s.product p
                LEFT JOIN FETCH p.images
                WHERE s.createdAt BETWEEN :fromDate AND :toDate
                  AND (:productId IS NULL OR p.id = :productId)
            """)
    Page<Sale> findAllByMonth(
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            @Param("productId") Long productId,
            Pageable pageable
    );
}
