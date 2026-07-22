package com.example.demo.order.repository;

import com.example.demo.common.enums.OrderStatus;
import com.example.demo.common.model.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    @EntityGraph(attributePaths = {"user", "items", "items.product", "items.product.images"})
    @Query("select o from Order o where o.id = :id")
    Optional<Order> findDetailsById(Long id);

    @EntityGraph(attributePaths = {"user"})
    @Query("""
            SELECT o FROM Order o
            LEFT JOIN o.items i
            LEFT JOIN i.product p
            WHERE (:userId IS NULL OR o.user.id = :userId)
              AND (:statuses IS NULL OR o.status IN :statuses)
              AND o.createdAt BETWEEN :fromDate AND :toDate
              AND (:productName IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :productName, '%')))
            ORDER BY o.createdAt ASC
            """)
    Page<Order> findAllByStatus(
            @Param("userId") Long userId,
            @Param("statuses") List<OrderStatus> statuses,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            @Param("productName") String productName,
            Pageable pageable
    );

    List<Order> findAllByStatusAndCreatedAtBefore(OrderStatus status, LocalDateTime dateTime);
}
