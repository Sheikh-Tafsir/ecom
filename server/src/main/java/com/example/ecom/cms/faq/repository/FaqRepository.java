package com.example.ecom.cms.faq.repository;

import com.example.ecom.common.model.Faq;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FaqRepository extends JpaRepository<Faq, UUID> {

    List<Faq> findAllByOrderByDisplayOrderAsc();

    @Query("SELECT MAX(f.displayOrder) FROM Faq f")
    Optional<Integer> findMaxDisplayOrder();

    boolean existsByDisplayOrder(int displayOrder);

    boolean existsByDisplayOrderAndIdNot(int displayOrder, UUID id);
}
