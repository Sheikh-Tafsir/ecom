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
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    @EntityGraph(attributePaths = {"user", "items", "items.product", "items.product.images"})
    @Query("select o from Order o where o.id = :id")
    Optional<Order> findDetailsById(Long id);

    @EntityGraph(attributePaths = {"user"})
    @Query("""
                SELECT DISTINCT o FROM Order o
                WHERE (:status IS NULL OR o.status = :status)
                    AND o.createdAt BETWEEN :fromDate AND :toDate
            """)
    Page<Order> findAllByStatus(@Param("status") OrderStatus status,
                                @Param("fromDate") LocalDateTime fromDate,
                                @Param("toDate") LocalDateTime toDate,
                                Pageable pageable);

    @EntityGraph(attributePaths = {"user"})
    @Query("""
                SELECT o
                FROM Order o
                WHERE o.user.id = :userId
                  AND o.createdAt BETWEEN :fromDate AND :toDate
            """)
    Page<Order> findAllByUser_Id(@Param("userId") Long userId,
                                 @Param("fromDate") LocalDateTime fromDate,
                                 @Param("toDate") LocalDateTime toDate,
                                 Pageable pageable);
}
