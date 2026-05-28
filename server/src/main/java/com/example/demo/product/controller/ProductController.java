package com.example.demo.product.controller;

import com.example.demo.common.dto.ApiResponse;
import com.example.demo.common.helper.CommonHelper;
import com.example.demo.common.model.Product;
import com.example.demo.common.service.MessageService;
import com.example.demo.common.utils.ResponseUtils;
import com.example.demo.product.dto.CreateProductRequest;
import com.example.demo.product.dto.UpdateProductRequest;
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

    private final ProductValidator productValidator;

    private final CommonHelper commonHelper;

    private final ProductService productService;

    private final MessageService messageService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<Product>>> findAll(Pageable pageable,
                                                                      @RequestParam(required = false) String name) {
        Page<Product> products = productService.findAll(pageable, name);
        return ResponseUtils.ok(products, messageService.get("successfully.found", "Product List"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Product>> findById(@PathVariable Long id) {
        Product product = productService.findById(id);
        return ResponseUtils.ok(product, messageService.get("successfully.found", "Product"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Product>> create(@Valid @ModelAttribute CreateProductRequest productRequest,
                                                               BindingResult bindingResult) throws IOException {

        productValidator.validateCreate(productRequest, bindingResult);
        commonHelper.checkErrors(bindingResult);

        Product product = productService.create(productRequest);
        return ResponseUtils.created(product, messageService.get("entity.creating", "Product"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Product>> update(@PathVariable Long id,
                                                       @Valid @ModelAttribute UpdateProductRequest productRequest,
                                                       BindingResult bindingResult) throws IOException {

        productValidator.validateUpdate(productRequest, bindingResult);
        commonHelper.checkErrors(bindingResult);

        Product product = productService.update(id, productRequest);
        return ResponseUtils.ok(product, messageService.get("successfully.updated", "Product"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        productService.delete(id);
        return ResponseUtils.ok(messageService.get("successfully.deleted", "Product"));
    }
}
