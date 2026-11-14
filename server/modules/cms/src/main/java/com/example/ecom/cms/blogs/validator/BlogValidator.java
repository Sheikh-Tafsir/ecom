package com.example.ecom.cms.blogs.validator;

import com.example.ecom.cms.blogs.dto.BlogRequest;
import com.example.ecom.cms.blogs.repository.BlogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class BlogValidator {

    private final BlogRepository blogRepository;

    public void validateCreate(BlogRequest request, Errors errors) {
        if (blogRepository.existsByTitle(request.getTitle())) {
            errors.rejectValue("title", "error.field.duplicate", new Object[]{"Title"}, "Title already exists");
        }
    }

    public void validateUpdate(UUID id, BlogRequest request, Errors errors) {
        if (blogRepository.existsByTitleAndIdNot(request.getTitle(), id)) {
            errors.rejectValue("title", "error.field.duplicate", new Object[]{"Title"}, "Title already exists");
        }
    }
}
