package com.example.ecom.product.sale;

import com.example.ecom.common.dto.DateRangeDto;
import com.example.ecom.product.product.model.Product;
import com.example.ecom.product.sale.model.Sale;
import com.example.ecom.product.sale.dto.SaleResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static com.example.ecom.common.utils.DateUtils.*;
import static com.example.ecom.common.utils.Utils.getValidPageable;

@Service
@RequiredArgsConstructor
public class SaleService {

    private final SaleRepository saleRepository;

    @PreAuthorize("hasAuthority(T(com.example.ecom.common.enums.Permission).SUPER_ADMIN_ACCESS.getValue())")
    public Page<SaleResponse> findAll(LocalDate fromDate, LocalDate toDate, UUID productId, String productName, Pageable pageable) {
        DateRangeDto dateRange = resolveDates(fromDate, toDate);

        return saleRepository.findAllByMonth(dateRange.fromDate(), dateRange.toDate(), productId, productName, getValidPageable(pageable))
                .map(SaleResponse::new);
    }

    @Transactional
    public Sale add(Product product, UUID orderId, int quantity, BigDecimal unitProfit) {
        Sale sale = new Sale();
        sale.setProduct(product);
        sale.setOrderId(orderId);
        sale.setQuantity(quantity);
        sale.setProfit(unitProfit.multiply(BigDecimal.valueOf(quantity)));

        return sale;
    }

    @Transactional
    public void createAll(List<Sale> sales) {
        saleRepository.saveAll(sales);
    }
}
