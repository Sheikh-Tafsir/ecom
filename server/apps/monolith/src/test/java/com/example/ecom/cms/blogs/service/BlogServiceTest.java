package com.example.ecom.cms.blogs.service;

import com.example.ecom.cms.blogs.dto.BlogRequest;
import com.example.ecom.cms.blogs.dto.BlogResponse;
import com.example.ecom.cms.blogs.repository.BlogRepository;
import com.example.ecom.common.dto.CustomUserDetails;
import com.example.ecom.cms.blogs.enums.BlogPostStatus;
import com.example.ecom.cms.blogs.model.Blogs;
import com.example.ecom.common.service.MessageService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
class BlogServiceTest {

    @Mock
    private BlogRepository blogRepository;

    @Mock
    private MessageService messageService;

    @InjectMocks
    private BlogService blogService;

    private UUID blogId;
    private Blogs blog;
    private BlogRequest blogRequest;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        blogId = UUID.fromString("c3d4e5f6-a7b8-9012-cdef-123456789012");
        pageable = PageRequest.of(0, 10);

        blog = new Blogs();
        blog.setId(blogId);
        blog.setTitle("Getting Started with Spring Boot");
        blog.setContent("Spring Boot makes it easy to create production-ready applications.");
        blog.setAuthor("Jane Doe");
        blog.setImageUrl("https://cdn.example.com/blogs/spring.jpg");
        blog.setStatus(BlogPostStatus.PUBLISHED);
        blog.setPublishedAt(Instant.parse("2025-06-15T10:30:00Z"));

        blogRequest = new BlogRequest();
        blogRequest.setTitle("Getting Started with Spring Boot");
        blogRequest.setContent("Spring Boot makes it easy to create production-ready applications.");
        blogRequest.setAuthor("Jane Doe");
        blogRequest.setImageUrl("https://cdn.example.com/blogs/spring.jpg");
        blogRequest.setStatus(BlogPostStatus.PUBLISHED);
    }

    // ── findAllPublished ────────────────────────────────────────

    @Test
    @DisplayName("findAllPublished should return published blogs without search filter")
    void findAllPublished_noSearch_returnsPublishedBlogs() {
        Page<Blogs> blogsPage = new PageImpl<>(List.of(blog));
        when(blogRepository.findAllByStatus(BlogPostStatus.PUBLISHED, pageable)).thenReturn(blogsPage);

        Page<BlogResponse> result = blogService.findAllPublished(null, pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals("Getting Started with Spring Boot", result.getContent().get(0).getTitle());
        verify(blogRepository).findAllByStatus(BlogPostStatus.PUBLISHED, pageable);
    }

    @Test
    @DisplayName("findAllPublished should return published blogs without search filter when search is empty")
    void findAllPublished_emptySearch_returnsPublishedBlogs() {
        Page<Blogs> blogsPage = new PageImpl<>(List.of(blog));
        when(blogRepository.findAllByStatus(BlogPostStatus.PUBLISHED, pageable)).thenReturn(blogsPage);

        Page<BlogResponse> result = blogService.findAllPublished("", pageable);

        assertEquals(1, result.getTotalElements());
        verify(blogRepository).findAllByStatus(BlogPostStatus.PUBLISHED, pageable);
    }

    @Test
    @DisplayName("findAllPublished should search published blogs by title and content")
    void findAllPublished_withSearch_searchesByTitleAndContent() {
        Page<Blogs> blogsPage = new PageImpl<>(List.of(blog));
        when(blogRepository.findAllByStatusAndTitleContainingIgnoreCaseOrStatusAndContentContainingIgnoreCase(
                BlogPostStatus.PUBLISHED, "Spring", BlogPostStatus.PUBLISHED, "Spring", pageable))
                .thenReturn(blogsPage);

        Page<BlogResponse> result = blogService.findAllPublished("Spring", pageable);

        assertEquals(1, result.getTotalElements());
        verify(blogRepository).findAllByStatusAndTitleContainingIgnoreCaseOrStatusAndContentContainingIgnoreCase(
                BlogPostStatus.PUBLISHED, "Spring", BlogPostStatus.PUBLISHED, "Spring", pageable);
    }

    @Test
    @DisplayName("findAllPublished should return empty page when no matching blogs")
    void findAllPublished_noMatches_returnsEmptyPage() {
        when(blogRepository.findAllByStatus(BlogPostStatus.PUBLISHED, pageable))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        Page<BlogResponse> result = blogService.findAllPublished(null, pageable);

        assertTrue(result.isEmpty());
        verify(blogRepository).findAllByStatus(BlogPostStatus.PUBLISHED, pageable);
    }

    // ── findByTitle ─────────────────────────────────────────────

    @Test
    @DisplayName("findByTitle should return published blog when user is null")
    void findByTitle_publishedBlogNoUser_returnsBlogResponse() {
        when(blogRepository.findByTitle("Getting Started with Spring Boot")).thenReturn(Optional.of(blog));

        BlogResponse result = blogService.findByTitle("Getting Started with Spring Boot", null);

        assertNotNull(result);
        assertEquals("Getting Started with Spring Boot", result.getTitle());
        assertEquals(BlogPostStatus.PUBLISHED, result.getStatus());
        verify(blogRepository).findByTitle("Getting Started with Spring Boot");
    }

    @Test
    @DisplayName("findByTitle should throw EntityNotFoundException when blog not found")
    void findByTitle_nonExistentTitle_throwsEntityNotFoundException() {
        when(blogRepository.findByTitle("Nonexistent")).thenReturn(Optional.empty());
        when(messageService.get(eq("error.entity.not.found"), eq("BlogPost"), eq("Nonexistent")))
                .thenReturn("BlogPost not found");

        assertThrows(EntityNotFoundException.class, () -> blogService.findByTitle("Nonexistent", null));

        verify(blogRepository).findByTitle("Nonexistent");
    }

    @Test
    @DisplayName("findByTitle should throw EntityNotFoundException when draft blog accessed by non-admin")
    void findByTitle_draftBlogNonAdmin_throwsEntityNotFoundException() {
        blog.setStatus(BlogPostStatus.DRAFT);
        when(blogRepository.findByTitle("Getting Started with Spring Boot")).thenReturn(Optional.of(blog));
        when(messageService.get(eq("error.entity.not.found"), eq("BlogPost"), eq("Getting Started with Spring Boot")))
                .thenReturn("BlogPost not found");

        assertThrows(EntityNotFoundException.class,
                () -> blogService.findByTitle("Getting Started with Spring Boot", null));
    }

    @Test
    @DisplayName("findByTitle should return draft blog when accessed by admin user")
    void findByTitle_draftBlogAdminUser_returnsBlogResponse() {
        blog.setStatus(BlogPostStatus.DRAFT);
        CustomUserDetails adminUser = mock(CustomUserDetails.class);
        doReturn(List.of(new SimpleGrantedAuthority("admin:access")))
                .when(adminUser).getAuthorities();

        when(blogRepository.findByTitle("Getting Started with Spring Boot")).thenReturn(Optional.of(blog));

        BlogResponse result = blogService.findByTitle("Getting Started with Spring Boot", adminUser);

        assertNotNull(result);
        assertEquals(BlogPostStatus.DRAFT, result.getStatus());
    }

    @Test
    @DisplayName("findByTitle should return draft blog when accessed by super admin user")
    void findByTitle_draftBlogSuperAdmin_returnsBlogResponse() {
        blog.setStatus(BlogPostStatus.DRAFT);
        CustomUserDetails superAdmin = mock(CustomUserDetails.class);
        doReturn(List.of(new SimpleGrantedAuthority("super_admin:access")))
                .when(superAdmin).getAuthorities();

        when(blogRepository.findByTitle("Getting Started with Spring Boot")).thenReturn(Optional.of(blog));

        BlogResponse result = blogService.findByTitle("Getting Started with Spring Boot", superAdmin);

        assertNotNull(result);
        assertEquals(BlogPostStatus.DRAFT, result.getStatus());
    }

    // ── findAll ─────────────────────────────────────────────────

    @Test
    @DisplayName("findAll should return all blogs without search filter")
    void findAll_noSearch_returnsAllBlogs() {
        Page<Blogs> blogsPage = new PageImpl<>(List.of(blog));
        when(blogRepository.findAll(pageable)).thenReturn(blogsPage);

        Page<BlogResponse> result = blogService.findAll(null, pageable);

        assertEquals(1, result.getTotalElements());
        verify(blogRepository).findAll(pageable);
    }

    @Test
    @DisplayName("findAll should search blogs by title and content")
    void findAll_withSearch_searchesByTitleAndContent() {
        Page<Blogs> blogsPage = new PageImpl<>(List.of(blog));
        when(blogRepository.findAllByTitleContainingIgnoreCaseOrContentContainingIgnoreCase(
                "Spring", "Spring", pageable)).thenReturn(blogsPage);

        Page<BlogResponse> result = blogService.findAll("Spring", pageable);

        assertEquals(1, result.getTotalElements());
        verify(blogRepository).findAllByTitleContainingIgnoreCaseOrContentContainingIgnoreCase(
                "Spring", "Spring", pageable);
    }

    @Test
    @DisplayName("findAll should return empty page for empty search")
    void findAll_emptySearch_returnsAll() {
        Page<Blogs> blogsPage = new PageImpl<>(List.of(blog));
        when(blogRepository.findAll(pageable)).thenReturn(blogsPage);

        Page<BlogResponse> result = blogService.findAll("", pageable);

        assertEquals(1, result.getTotalElements());
        verify(blogRepository).findAll(pageable);
    }

    // ── create ──────────────────────────────────────────────────

    @Test
    @DisplayName("create should persist blog and return response")
    void create_validRequest_savesAndReturnsResponse() {
        when(blogRepository.save(any(Blogs.class))).thenReturn(blog);

        BlogResponse result = blogService.create(blogRequest);

        assertNotNull(result);
        assertEquals("Getting Started with Spring Boot", result.getTitle());
        assertEquals("Jane Doe", result.getAuthor());
        verify(blogRepository).save(any(Blogs.class));
    }

    @Test
    @DisplayName("create should set publishedAt when status changes to PUBLISHED")
    void create_publishedStatus_setsPublishedAt() {
        blogRequest.setStatus(BlogPostStatus.PUBLISHED);
        when(blogRepository.save(any(Blogs.class))).thenAnswer(invocation -> {
            Blogs saved = invocation.getArgument(0);
            assertNotNull(saved.getPublishedAt());
            saved.setId(blogId);
            return saved;
        });

        blogService.create(blogRequest);

        verify(blogRepository).save(any(Blogs.class));
    }

    @Test
    @DisplayName("create should not set publishedAt for draft status")
    void create_draftStatus_doesNotSetPublishedAt() {
        blogRequest.setStatus(BlogPostStatus.DRAFT);
        when(blogRepository.save(any(Blogs.class))).thenAnswer(invocation -> {
            Blogs saved = invocation.getArgument(0);
            saved.setId(blogId);
            return saved;
        });

        blogService.create(blogRequest);

        verify(blogRepository).save(any(Blogs.class));
    }

    // ── update ──────────────────────────────────────────────────

    @Test
    @DisplayName("update should modify existing blog and return updated response")
    void update_existingBlog_updatesAndReturnsResponse() {
        blogRequest.setTitle("Updated Spring Boot Guide");
        when(blogRepository.findById(blogId)).thenReturn(Optional.of(blog));
        when(blogRepository.save(any(Blogs.class))).thenReturn(blog);

        BlogResponse result = blogService.update(blogId, blogRequest);

        assertNotNull(result);
        verify(blogRepository).findById(blogId);
        verify(blogRepository).save(any(Blogs.class));
    }

    @Test
    @DisplayName("update should throw EntityNotFoundException when blog not found")
    void update_nonExistentBlog_throwsEntityNotFoundException() {
        UUID nonExistentId = UUID.fromString("00000000-0000-0000-0000-000000000003");
        when(blogRepository.findById(nonExistentId)).thenReturn(Optional.empty());
        when(messageService.get(eq("error.entity.not.found"), eq("BlogPost"), eq(nonExistentId)))
                .thenReturn("BlogPost not found");

        assertThrows(EntityNotFoundException.class, () -> blogService.update(nonExistentId, blogRequest));

        verify(blogRepository).findById(nonExistentId);
        verify(blogRepository, never()).save(any());
    }

    @Test
    @DisplayName("update should set publishedAt when changing draft to published")
    void update_draftToPublished_setsPublishedAt() {
        blog.setStatus(BlogPostStatus.DRAFT);
        blog.setPublishedAt(null);
        blogRequest.setStatus(BlogPostStatus.PUBLISHED);

        when(blogRepository.findById(blogId)).thenReturn(Optional.of(blog));
        when(blogRepository.save(any(Blogs.class))).thenAnswer(invocation -> {
            Blogs saved = invocation.getArgument(0);
            assertNotNull(saved.getPublishedAt());
            return saved;
        });

        blogService.update(blogId, blogRequest);

        verify(blogRepository).save(any(Blogs.class));
    }

    // ── delete ──────────────────────────────────────────────────

    @Test
    @DisplayName("delete should remove existing blog")
    void delete_existingBlog_deletesBlog() {
        when(blogRepository.findById(blogId)).thenReturn(Optional.of(blog));

        blogService.delete(blogId);

        verify(blogRepository).findById(blogId);
        verify(blogRepository).delete(blog);
    }

    @Test
    @DisplayName("delete should throw EntityNotFoundException when blog not found")
    void delete_nonExistentBlog_throwsEntityNotFoundException() {
        UUID nonExistentId = UUID.fromString("00000000-0000-0000-0000-000000000004");
        when(blogRepository.findById(nonExistentId)).thenReturn(Optional.empty());
        when(messageService.get(eq("error.entity.not.found"), eq("BlogPost"), eq(nonExistentId)))
                .thenReturn("BlogPost not found");

        assertThrows(EntityNotFoundException.class, () -> blogService.delete(nonExistentId));

        verify(blogRepository).findById(nonExistentId);
        verify(blogRepository, never()).delete(any());
    }
}
