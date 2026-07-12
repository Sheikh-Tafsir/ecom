package com.example.demo.category.service;

import com.example.demo.category.dto.CategorySaveRequest;
import com.example.demo.common.model.Category;
import com.example.demo.category.repository.CategoryRepository;
import com.example.demo.common.service.MessageService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    private final MessageService messageService;

    @Cacheable(value = "categories")
    public List<Category> findAll() {
        return categoryRepository.findAll();
    }

    @CacheEvict(value = "categories")
    @Transactional
    public Long create(CategorySaveRequest request) {
        checkNameExists(request.name());
        Category category;

        category = new Category();
        category.setName(request.name());
        categoryRepository.save(category);

        return category.getId();
    }

    @CacheEvict(value = "categories")
    @Transactional
    public void update(Long id, CategorySaveRequest request) {
        Category category = findByIdHelper(id);

        checkNameExists(request.name());
        category.setName(request.name());

        categoryRepository.save(category);
    }

    @CacheEvict(value = "categories")
    @Transactional
    public void delete(Long id) {
        Category category = findByIdHelper(id);
        categoryRepository.delete(category);
    }

    private void checkNameExists(String name) {
        Category category = categoryRepository.findByName(name);

        if (category != null) {
            throw new ValidationException("Category Name already exists");
        }
    }

    private Category findByIdHelper(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(messageService.get("error.entity.not.found", "Category", id)));
    }

}
