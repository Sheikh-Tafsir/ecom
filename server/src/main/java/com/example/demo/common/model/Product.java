package com.example.demo.common.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Product extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ProductImage> images = new HashSet<>();

    @Column(nullable = false)
    private BigDecimal price = BigDecimal.ZERO;

    private int quantity;

    public void addImage(ProductImage image) {
        images.add(image);
        image.setProduct(this);
    }

    public void clearImages() {
        images.forEach(image -> image.setProduct(null));
        images.clear();
    }
}
