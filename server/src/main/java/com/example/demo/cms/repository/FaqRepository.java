package com.example.demo.cms.repository;

import com.example.demo.common.model.Faq;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FaqRepository extends JpaRepository<Faq, Long> {

    List<Faq> findAllByOrderByDisplayOrderAsc();

    @Query("SELECT MAX(f.displayOrder) FROM Faq f")
    Optional<Integer> findMaxDisplayOrder();

    boolean existsByDisplayOrder(int displayOrder);

    boolean existsByDisplayOrderAndIdNot(int displayOrder, Long id);
}
