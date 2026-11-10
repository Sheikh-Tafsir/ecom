package com.example.ecom.cms.blogs.dto;

import com.example.ecom.cms.blogs.enums.BlogPostStatus;
import com.example.ecom.cms.blogs.model.Blogs;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class BlogDtoTest {

    @Nested
    @DisplayName("BlogRequest Tests")
    class BlogRequestTest {

        @Test
        @DisplayName("Setters and getters work correctly")
        void settersGetters_allFields_workCorrectly() {
            BlogRequest request = new BlogRequest();
            request.setTitle("10 Tips for Online Shopping");
            request.setContent("Here are the top tips...");
            request.setAuthor("Sarah Writer");
            request.setImageUrl("https://cdn.example.com/blog/tips.jpg");
            request.setStatus(BlogPostStatus.PUBLISHED);

            assertEquals("10 Tips for Online Shopping", request.getTitle());
            assertEquals("Here are the top tips...", request.getContent());
            assertEquals("Sarah Writer", request.getAuthor());
            assertEquals("https://cdn.example.com/blog/tips.jpg", request.getImageUrl());
            assertEquals(BlogPostStatus.PUBLISHED, request.getStatus());
        }
    }

    @Nested
    @DisplayName("BlogResponse Tests")
    class BlogResponseTest {

        @Test
        @DisplayName("Construction from Blogs entity maps all fields")
        void constructor_fromEntity_setsAllFields() {
            UUID id = UUID.fromString("01927d79-097f-7afa-9af5-415263748596");
            Instant publishedAt = Instant.parse("2025-08-01T10:00:00Z");
            Instant createdAt = Instant.parse("2025-07-28T08:00:00Z");

            Blogs blog = new Blogs();
            blog.setId(id);
            blog.setTitle("Understanding E-Commerce");
            blog.setContent("This article explores...");
            blog.setAuthor("Tech Blogger");
            blog.setImageUrl("https://cdn.example.com/blog/ecommerce.jpg");
            blog.setStatus(BlogPostStatus.PUBLISHED);
            blog.setPublishedAt(publishedAt);
            blog.setCreatedAt(createdAt);

            BlogResponse response = new BlogResponse(blog);

            assertEquals(id, response.getId());
            assertEquals("Understanding E-Commerce", response.getTitle());
            assertEquals("This article explores...", response.getContent());
            assertEquals("Tech Blogger", response.getAuthor());
            assertEquals("https://cdn.example.com/blog/ecommerce.jpg", response.getImageUrl());
            assertEquals(BlogPostStatus.PUBLISHED, response.getStatus());
            assertEquals(publishedAt, response.getPublishedAt());
            assertEquals(createdAt, response.getCreatedAt());
        }

        @Test
        @DisplayName("Construction from draft blog has null publishedAt")
        void constructor_fromDraftEntity_nullPublishedAt() {
            Blogs blog = new Blogs();
            blog.setId(UUID.fromString("01927d79-1a8f-7bfa-8af6-526374859607"));
            blog.setTitle("Draft Post");
            blog.setContent("Work in progress...");
            blog.setStatus(BlogPostStatus.DRAFT);
            blog.setPublishedAt(null);

            BlogResponse response = new BlogResponse(blog);

            assertNull(response.getPublishedAt());
            assertEquals(BlogPostStatus.DRAFT, response.getStatus());
        }
    }
}
