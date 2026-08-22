package com.example.ecom.product.stock.repository;

import com.example.ecom.common.model.Stock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface StockRepository extends JpaRepository<Stock, Long> {

    @EntityGraph(attributePaths = {"items", "items.product"})
    Optional<Stock> findDetailsById(Long id);

    @Query("""
            SELECT DISTINCT s FROM Stock s
            LEFT JOIN s.items i
            LEFT JOIN i.product p
            WHERE s.createdAt BETWEEN :fromDate AND :toDate
              AND (:productName IS NULL OR LOWER(CAST(p.name AS string)) 
                LIKE LOWER(CONCAT('%', CAST(:productName AS string), '%')))
            """)
    Page<Stock> findAll(@Param("fromDate") Instant fromDate,
                        @Param("toDate") Instant toDate,
                        @Param("productName") String productName,
                        Pageable pageable);

}
