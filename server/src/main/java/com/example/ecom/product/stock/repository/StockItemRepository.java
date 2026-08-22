package com.example.ecom.product.stock.repository;

import com.example.ecom.common.model.StockItem;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface StockItemRepository extends JpaRepository<StockItem, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT item
            FROM StockItem item
            WHERE item.product.id = :productId
              AND item.remaining > 0
            ORDER BY item.stock.createdAt asc, item.id asc
            """)
    List<StockItem> findAvailableByProductIdOrderByOldest(@Param("productId") Long productId);

    @Query("""
            SELECT item
            FROM StockItem item
            WHERE (:productId IS NULL OR item.product.id = :productId)
              AND (:productName IS NULL OR LOWER(CAST(item.product.name AS string)) 
                LIKE LOWER(CONCAT('%', CAST(:productName AS string), '%')))
              AND item.createdAt BETWEEN :fromDate AND :toDate
            """)
    Page<StockItem> findAll(@Param("fromDate") Instant fromDate,
                            @Param("toDate") Instant toDate,
                            @Param("productId") Long productId,
                            @Param("productName") String productName,
                            Pageable pageable);
}
