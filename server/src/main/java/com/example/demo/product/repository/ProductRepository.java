package com.example.demo.product.repository;

import com.example.demo.common.enums.ProductStatus;
import com.example.demo.common.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    @Query("""
                SELECT p FROM Product p
                WHERE (:name IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%')))
                AND (:status IS NULL OR p.status <> :status)
            """)
    Page<Product> findAllByNameAndExcludeStatus(
            @Param("name") String name,
            @Param("status") ProductStatus status,
            Pageable pageable
    );
}
