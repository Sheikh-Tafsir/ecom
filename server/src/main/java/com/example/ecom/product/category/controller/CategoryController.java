package com.example.ecom.product.category.controller;

import com.example.ecom.product.category.dto.CategorySaveRequest;
import com.example.ecom.common.dto.ApiResponse;
import com.example.ecom.common.model.Category;
import com.example.ecom.common.service.MessageService;
import com.example.ecom.common.utils.ResponseUtils;
import com.example.ecom.product.category.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    private final MessageService messageService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Category>>> findAll() {
        List<Category> categories = categoryService.findAll();
        return ResponseUtils.ok(categories, messageService.get("successfully.found", "Category List"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UUID>> create(@Valid CategorySaveRequest request) {
        UUID id = categoryService.create(request);
        return ResponseUtils.created(id, messageService.get("successfully.created", "Category"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> update(@PathVariable UUID id, @Valid CategorySaveRequest request) {
        categoryService.update(id, request);
        return ResponseUtils.ok(messageService.get("successfully.updated", "Category"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        categoryService.delete(id);
        return ResponseUtils.ok(messageService.get("successfully.deleted", "Category"));
    }
}
