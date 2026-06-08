package com.example.demo.category.controller;

import com.example.demo.common.dto.ApiResponse;
import com.example.demo.common.model.Category;
import com.example.demo.common.service.MessageService;
import com.example.demo.common.utils.ResponseUtils;
import com.example.demo.category.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
        return ResponseUtils.ok(categories, messageService.get("successfully.found", "Product List"));
    }
}
