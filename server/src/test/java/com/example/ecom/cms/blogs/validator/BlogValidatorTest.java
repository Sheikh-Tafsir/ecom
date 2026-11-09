package com.example.ecom.cms.blogs.validator;

import com.example.ecom.cms.blogs.dto.BlogRequest;
import com.example.ecom.cms.blogs.repository.BlogRepository;
import com.example.ecom.common.enums.BlogPostStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BlogValidatorTest {

    @Mock
    private BlogRepository blogRepository;

    @InjectMocks
    private BlogValidator blogValidator;

    private BlogRequest blogRequest;
    private Errors errors;

    @BeforeEach
    void setUp() {
        blogRequest = new BlogRequest();
        blogRequest.setTitle("Advanced Java Concurrency Patterns");
        blogRequest.setContent("In-depth coverage of concurrent data structures and patterns.");
        blogRequest.setAuthor("John Smith");
        blogRequest.setStatus(BlogPostStatus.DRAFT);
        errors = new BeanPropertyBindingResult(blogRequest, "blogRequest");
    }

    // ── validateCreate ──────────────────────────────────────────

    @Test
    @DisplayName("validateCreate should pass when title is unique")
    void validateCreate_uniqueTitle_noErrors() {
        when(blogRepository.existsByTitle("Advanced Java Concurrency Patterns")).thenReturn(false);

        blogValidator.validateCreate(blogRequest, errors);

        assertFalse(errors.hasErrors());
        verify(blogRepository).existsByTitle("Advanced Java Concurrency Patterns");
    }

    @Test
    @DisplayName("validateCreate should reject when title already exists")
    void validateCreate_duplicateTitle_rejectsTitleField() {
        when(blogRepository.existsByTitle("Advanced Java Concurrency Patterns")).thenReturn(true);

        blogValidator.validateCreate(blogRequest, errors);

        assertTrue(errors.hasFieldErrors("title"));
        assertEquals("error.field.duplicate", errors.getFieldError("title").getCode());
        verify(blogRepository).existsByTitle("Advanced Java Concurrency Patterns");
    }

    // ── validateUpdate ──────────────────────────────────────────

    @Test
    @DisplayName("validateUpdate should pass when title is unique for different id")
    void validateUpdate_uniqueTitleForDifferentId_noErrors() {
        UUID blogId = UUID.fromString("d4e5f6a7-b8c9-0123-defa-234567890123");
        when(blogRepository.existsByTitleAndIdNot("Advanced Java Concurrency Patterns", blogId)).thenReturn(false);

        blogValidator.validateUpdate(blogId, blogRequest, errors);

        assertFalse(errors.hasErrors());
        verify(blogRepository).existsByTitleAndIdNot("Advanced Java Concurrency Patterns", blogId);
    }

    @Test
    @DisplayName("validateUpdate should reject when title conflicts with another blog")
    void validateUpdate_titleConflictsWithAnotherBlog_rejectsTitleField() {
        UUID blogId = UUID.fromString("d4e5f6a7-b8c9-0123-defa-234567890123");
        when(blogRepository.existsByTitleAndIdNot("Advanced Java Concurrency Patterns", blogId)).thenReturn(true);

        blogValidator.validateUpdate(blogId, blogRequest, errors);

        assertTrue(errors.hasFieldErrors("title"));
        assertEquals("error.field.duplicate", errors.getFieldError("title").getCode());
        verify(blogRepository).existsByTitleAndIdNot("Advanced Java Concurrency Patterns", blogId);
    }
}
