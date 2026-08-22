package com.example.ecom.product.product.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.Set;

@Data
public class CreateProductRequest {

    @NotBlank
    @Size(min = 2, max = 255)
    private String name;

    @NotBlank
    @Size(min = 5, max = 1023)
    private String description;

    @NotNull
    @DecimalMin(value = "1.0")
    private BigDecimal price;

    @NotEmpty
    private Set<MultipartFile> images;

    @NotEmpty
    private Set<Long> categoryIds;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public Set<MultipartFile> getImages() { return images; }
    public void setImages(Set<MultipartFile> images) { this.images = images; }
    public Set<Long> getCategoryIds() { return categoryIds; }
    public void setCategoryIds(Set<Long> categoryIds) { this.categoryIds = categoryIds; }
}
