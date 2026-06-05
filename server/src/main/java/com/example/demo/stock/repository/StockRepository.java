package com.example.demo.stock.repository;

import com.example.demo.common.model.Stock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StockRepository extends JpaRepository<Stock, Long> {

    Page<Stock> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"items", "items.product"})
    Optional<Stock> findDetailsById(Long id);
}
