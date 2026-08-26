package com.example.ecom.product.category.service;

import com.example.ecom.common.model.Category;
import com.example.ecom.common.service.MessageService;
import com.example.ecom.product.category.dto.CategorySaveRequest;
import com.example.ecom.product.category.repository.CategoryRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.example.ecom.common.utils.CacheConstants.CACHE_CATEGORIES;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    private final MessageService messageService;

    @Cacheable(value = CACHE_CATEGORIES)
    public List<Category> findAll() {
        return categoryRepository.findAll();
    }

    @PreAuthorize("hasAnyAuthority(T(com.example.ecom.common.enums.Permission).ADMIN_ACCESS.getValue()," +
            "T(com.example.ecom.common.enums.Permission).SUPER_ADMIN_ACCESS.getValue())")
    @CacheEvict(value = CACHE_CATEGORIES, allEntries = true)
    @Transactional
    public Long create(CategorySaveRequest request) {
        checkNameExists(request.name(), null);
        Category category = new Category();
        category.setName(request.name());
        categoryRepository.save(category);

        return category.getId();
    }

    @PreAuthorize("hasAnyAuthority(T(com.example.ecom.common.enums.Permission).ADMIN_ACCESS.getValue()," +
            "T(com.example.ecom.common.enums.Permission).SUPER_ADMIN_ACCESS.getValue())")
    @CacheEvict(value = CACHE_CATEGORIES, allEntries = true)
    @Transactional
    public void update(Long id, CategorySaveRequest request) {
        Category category = findByIdHelper(id);

        checkNameExists(request.name(), id);
        category.setName(request.name());

        categoryRepository.save(category);
    }

    @PreAuthorize("hasAnyAuthority(T(com.example.ecom.common.enums.Permission).ADMIN_ACCESS.getValue()," +
            "T(com.example.ecom.common.enums.Permission).SUPER_ADMIN_ACCESS.getValue())")
    @CacheEvict(value = CACHE_CATEGORIES, allEntries = true)
    @Transactional
    public void delete(Long id) {
        Category category = findByIdHelper(id);
        categoryRepository.delete(category);
    }

    private void checkNameExists(String name, Long currentId) {
        Category category = categoryRepository.findByName(name);

        if (category != null && (currentId == null || !category.getId().equals(currentId))) {
            throw new ValidationException("Category Name already exists");
        }
    }

    private Category findByIdHelper(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(messageService.get("error.entity.not.found", "Category", id)));
    }
}
