package com.example.ecom.common.model;

import com.example.ecom.common.enums.BlogPostStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "blogs", uniqueConstraints = @UniqueConstraint(columnNames = "title"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Blogs extends AuditableEntity {

    @Column(nullable = false, unique = true)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    private String author;

    private String imageUrl;

    @Enumerated(EnumType.STRING)
    private BlogPostStatus status = BlogPostStatus.DRAFT;

    private Instant publishedAt;
}
