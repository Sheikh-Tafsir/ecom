package com.example.demo.sale;

import com.example.demo.common.dto.ApiResponse;
import com.example.demo.common.model.Sale;
import com.example.demo.common.service.MessageService;
import com.example.demo.common.utils.ResponseUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequiredArgsConstructor
@RequestMapping("/sales")
public class SaleController {

    private final SaleService saleService;

    private final MessageService messageService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<Sale>>> findAll(@RequestParam(required = false) LocalDateTime fromDate,
                                                           @RequestParam(required = false) LocalDateTime toDate,
                                                           @RequestParam(required = false) Long productId,
                                                           Pageable pageable) {

        Page<Sale> sales = saleService.findAll(fromDate, toDate, productId, pageable);
        return ResponseUtils.ok(sales, messageService.get("successfully.found", "Sales"));
    }
}
