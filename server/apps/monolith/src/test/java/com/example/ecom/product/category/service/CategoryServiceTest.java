package com.example.ecom.product.category.service;

import com.example.ecom.product.category.model.Category;
import com.example.ecom.common.service.MessageService;
import com.example.ecom.product.category.dto.CategorySaveRequest;
import com.example.ecom.product.category.repository.CategoryRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private MessageService messageService;

    @InjectMocks
    private CategoryService categoryService;

    private static final UUID CATEGORY_ID = UUID.fromString("f47ac10b-58cc-4372-a567-0e02b2c3d479");
    private static final UUID CATEGORY_ID_2 = UUID.fromString("e57bd20c-69dd-5483-b678-1f13c3d4e580");

    private Category category;

    @BeforeEach
    void setUp() {
        category = new Category();
        category.setId(CATEGORY_ID);
        category.setName("Electronics");
    }

    @Test
    @DisplayName("findAll should return all categories from repository")
    void findAll_categoriesExist_returnsAllCategories() {
        Category cat2 = new Category();
        cat2.setId(CATEGORY_ID_2);
        cat2.setName("Clothing");

        when(categoryRepository.findAll()).thenReturn(List.of(category, cat2));

        List<Category> result = categoryService.findAll();

        assertEquals(2, result.size());
        verify(categoryRepository).findAll();
    }

    @Test
    @DisplayName("findAll should return empty list when no categories exist")
    void findAll_noCategories_returnsEmptyList() {
        when(categoryRepository.findAll()).thenReturn(List.of());

        List<Category> result = categoryService.findAll();

        assertTrue(result.isEmpty());
        verify(categoryRepository).findAll();
    }

    @Test
    @DisplayName("create should save new category and return its ID")
    void create_uniqueName_savesAndReturnsId() {
        CategorySaveRequest request = new CategorySaveRequest("Books");
        when(categoryRepository.findByName("Books")).thenReturn(null);
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> {
            Category saved = invocation.getArgument(0);
            saved.setId(CATEGORY_ID);
            return saved;
        });

        UUID result = categoryService.create(request);

        assertNotNull(result);
        verify(categoryRepository).findByName("Books");
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    @DisplayName("create should throw ValidationException when category name already exists")
    void create_duplicateName_throwsValidationException() {
        CategorySaveRequest request = new CategorySaveRequest("Electronics");
        when(categoryRepository.findByName("Electronics")).thenReturn(category);

        assertThrows(ValidationException.class, () -> categoryService.create(request));
        verify(categoryRepository).findByName("Electronics");
        verify(categoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("update should update existing category name")
    void update_validRequest_updatesCategory() {
        CategorySaveRequest request = new CategorySaveRequest("Updated Electronics");
        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));
        when(categoryRepository.findByName("Updated Electronics")).thenReturn(null);
        when(categoryRepository.save(any(Category.class))).thenReturn(category);

        categoryService.update(CATEGORY_ID, request);

        assertEquals("Updated Electronics", category.getName());
        verify(categoryRepository).findById(CATEGORY_ID);
        verify(categoryRepository).findByName("Updated Electronics");
        verify(categoryRepository).save(category);
    }

    @Test
    @DisplayName("update should throw EntityNotFoundException when category not found")
    void update_categoryNotFound_throwsEntityNotFoundException() {
        CategorySaveRequest request = new CategorySaveRequest("Updated");
        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.empty());
        when(messageService.get(eq("error.entity.not.found"), eq("Category"), eq(CATEGORY_ID)))
                .thenReturn("Category not found");

        assertThrows(EntityNotFoundException.class, () -> categoryService.update(CATEGORY_ID, request));
        verify(categoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("update should throw ValidationException when new name already exists for another category")
    void update_duplicateNameDifferentCategory_throwsValidationException() {
        Category existingOther = new Category();
        existingOther.setId(CATEGORY_ID_2);
        existingOther.setName("Clothing");

        CategorySaveRequest request = new CategorySaveRequest("Clothing");
        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));
        when(categoryRepository.findByName("Clothing")).thenReturn(existingOther);

        assertThrows(ValidationException.class, () -> categoryService.update(CATEGORY_ID, request));
        verify(categoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("update should allow same name when it belongs to the same category being updated")
    void update_sameNameSameCategory_updatesSuccessfully() {
        CategorySaveRequest request = new CategorySaveRequest("Electronics");
        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));
        when(categoryRepository.findByName("Electronics")).thenReturn(category);
        when(categoryRepository.save(any(Category.class))).thenReturn(category);

        categoryService.update(CATEGORY_ID, request);

        verify(categoryRepository).save(category);
    }

    @Test
    @DisplayName("delete should remove existing category")
    void delete_existingCategory_deletesFromRepository() {
        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));

        categoryService.delete(CATEGORY_ID);

        verify(categoryRepository).findById(CATEGORY_ID);
        verify(categoryRepository).delete(category);
    }

    @Test
    @DisplayName("delete should throw EntityNotFoundException when category not found")
    void delete_categoryNotFound_throwsEntityNotFoundException() {
        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.empty());
        when(messageService.get(eq("error.entity.not.found"), eq("Category"), eq(CATEGORY_ID)))
                .thenReturn("Category not found");

        assertThrows(EntityNotFoundException.class, () -> categoryService.delete(CATEGORY_ID));
        verify(categoryRepository, never()).delete(any());
    }
}
