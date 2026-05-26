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

import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    @EntityGraph(attributePaths = {"user", "orderItems", "orderItems.product"})
    @Query("""
                SELECT DISTINCT o FROM Order o
                WHERE (:status IS NULL OR o.status = :status)
            """)
    Page<Order> findAllByStatus(@Param("status") OrderStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "orderItems", "orderItems.product"})
    Page<Order> findByUser_Id(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "orderItems", "orderItems.product"})
    Optional<Order> findById(Long id);
}
