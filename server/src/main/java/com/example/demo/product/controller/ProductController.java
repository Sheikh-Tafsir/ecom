package com.example.demo.product.controller;

import com.example.demo.common.dto.ApiResponse;
import com.example.demo.common.exception.JsrValidationException;
import com.example.demo.common.service.MessageService;
import com.example.demo.common.utils.ResponseUtils;
import com.example.demo.product.dto.ProductRequest;
import com.example.demo.product.dto.ProductResponse;
import com.example.demo.product.service.ProductService;
import com.example.demo.product.validator.ProductValidator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    private final ProductValidator productValidator;

    private final MessageService messageService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> findAll(Pageable pageable,
                                                                      @RequestParam(required = false) String name) {
        Page<ProductResponse> products = productService.findAll(pageable, name);
        return ResponseUtils.ok(products, messageService.get("successfully.found", "Product List"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> findById(@PathVariable Long id) {
        ProductResponse product = productService.findById(id);
        return ResponseUtils.ok(product, messageService.get("successfully.found", "Product"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponse>> create(@Valid @ModelAttribute ProductRequest productRequest,
                                                               BindingResult bindingResult) throws IOException {
        validate(productRequest, bindingResult);

        ProductResponse product = productService.create(productRequest);
        return ResponseUtils.created(product, messageService.get("entity.creating", "Product"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> update(@PathVariable Long id,
                                                               @Valid @ModelAttribute ProductRequest productRequest,
                                                               BindingResult bindingResult) throws IOException {
        validate(productRequest, bindingResult);

        ProductResponse product = productService.update(id, productRequest);
        return ResponseUtils.ok(product, messageService.get("successfully.updated", "Product"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        productService.delete(id);
        return ResponseUtils.ok(messageService.get("successfully.deleted", "Product"));
    }

    private void validate(ProductRequest productRequest, BindingResult bindingResult) {
        productValidator.validate(productRequest, bindingResult);

        if (bindingResult.hasErrors()) {
            throw new JsrValidationException(bindingResult);
        }
    }
}
