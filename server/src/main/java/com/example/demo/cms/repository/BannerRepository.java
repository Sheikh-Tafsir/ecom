package com.example.demo.cms.repository;

import com.example.demo.common.model.Banner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BannerRepository extends JpaRepository<Banner, Long> {

    List<Banner> findAllByActiveTrueOrderByDisplayOrderAsc();

    @Query("SELECT MAX(b.displayOrder) FROM Banner b")
    Optional<Integer> findMaxDisplayOrder();

    long countByActiveTrue();

    boolean existsByDisplayOrder(int displayOrder);

    boolean existsByDisplayOrderAndIdNot(int displayOrder, Long id);
}
