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
                SELECT s FROM Sale s
                WHERE s.createdAt BETWEEN :fromDate AND :toDate
                     AND (:productId IS NULL OR s.product.id = :productId)
            """)
    Page<Sale> findAllByMonth(
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            @Param("productId") Long productId,
            Pageable pageable
    );
}
