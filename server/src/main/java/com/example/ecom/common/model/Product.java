package com.example.ecom.common.model;

import com.example.ecom.common.enums.ProductStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Formula;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

import static com.example.ecom.common.enums.ProductStatus.DISCONTINUED;

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

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public ProductStatus getStatus() { return status; }
    public void setStatus(ProductStatus status) { this.status = status; }
    public BigDecimal getRating() { return rating; }
    public void setRating(BigDecimal rating) { this.rating = rating; }
    public long getReviewCount() { return reviewCount; }
    public void setReviewCount(long reviewCount) { this.reviewCount = reviewCount; }
    public Set<ProductImage> getImages() { return images; }
    public void setImages(Set<ProductImage> images) { this.images = images; }
    public String getFirstImage() { return firstImage; }
    public void setFirstImage(String firstImage) { this.firstImage = firstImage; }
    public Set<Category> getCategories() { return categories; }
    public void setCategories(Set<Category> categories) { this.categories = categories; }
    public Set<Review> getReviews() { return reviews; }
    public void setReviews(Set<Review> reviews) { this.reviews = reviews; }
    public Boolean getDeleted() { return deleted; }
    public void setDeleted(Boolean deleted) { this.deleted = deleted; }
}
