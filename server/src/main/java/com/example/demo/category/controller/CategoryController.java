package com.example.demo.category.controller;

import com.example.demo.category.dto.CategorySaveRequest;
import com.example.demo.common.dto.ApiResponse;
import com.example.demo.common.model.Category;
import com.example.demo.common.service.MessageService;
import com.example.demo.common.utils.ResponseUtils;
import com.example.demo.category.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    public ResponseEntity<ApiResponse<Long>> create(@Valid CategorySaveRequest request) {
        Long id = categoryService.create(request);
        return ResponseUtils.created(id, messageService.get("successfully.created", "Category"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> update(@PathVariable Long id, @Valid CategorySaveRequest request) {
        categoryService.update(id, request);
        return ResponseUtils.ok(messageService.get("successfully.updated", "Category"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return ResponseUtils.ok(messageService.get("successfully.deleted", "Category"));
    }
}
