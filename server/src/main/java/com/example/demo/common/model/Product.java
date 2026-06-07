package com.example.demo.common.model;

import com.example.demo.common.enums.ProductStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Formula;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

import static com.example.demo.common.enums.ProductStatus.DISCONTINUED;

@Entity
@Table(
        name = "products",
        uniqueConstraints = @UniqueConstraint(columnNames = {"name"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Product extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column
    private String description;

    @Column(nullable = false)
    private BigDecimal price = BigDecimal.ZERO;

    private int quantity;

    @Enumerated(EnumType.STRING)
    private ProductStatus status = ProductStatus.COMING_SOON;

    @Column(nullable = false)
    private BigDecimal rating = BigDecimal.ZERO;

    private long reviewCount;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ProductImage> images = new HashSet<>();

    @Formula("(SELECT pi.image FROM product_images pi WHERE pi.product_id = id ORDER BY pi.id ASC LIMIT 1)")
    private String firstImage;

    @ManyToMany
    @JoinTable(
            name = "product_categories",
            joinColumns = @JoinColumn(name = "product_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    private Set<Category> categories = new HashSet<>();

    @OneToMany(mappedBy = "product")
    private Set<Review> reviews = new HashSet<>();

    private Boolean deleted = false;

    public void addImage(ProductImage image) {
        images.add(image);
        image.setProduct(this);
    }

    public boolean isNotActive() {
        return getDeleted() || getStatus() == DISCONTINUED;
    }
}
