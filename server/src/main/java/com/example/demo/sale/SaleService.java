package com.example.demo.sale;

import com.example.demo.common.dto.DateRangeDto;
import com.example.demo.common.model.Order;
import com.example.demo.common.model.Product;
import com.example.demo.common.model.Sale;
import com.example.demo.sale.dto.SaleResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static com.example.demo.common.utils.DateUtils.*;
import static com.example.demo.common.utils.Utils.getValidPageable;

@Service
@RequiredArgsConstructor
public class SaleService {

    private final SaleRepository saleRepository;

    public Sale add(Product product, Order order, int quantity, BigDecimal unitProfit) {
        Sale sale = new Sale();
        sale.setProduct(product);
        sale.setOrder(order);
        sale.setQuantity(quantity);
        sale.setProfit(unitProfit.multiply(BigDecimal.valueOf(quantity)));

        return sale;
    }

    public void createAll(List<Sale> sales) {
        saleRepository.saveAll(sales);
    }

    @PreAuthorize("hasAuthority(T(com.example.demo.common.enums.Permission).SUPER_ADMIN_ACCESS.getValue())")
    public Page<SaleResponse> findAll(LocalDateTime fromDate, LocalDateTime toDate, Long productId, Pageable pageable) {
        DateRangeDto dateRange = resolveDates(fromDate, toDate);

        return saleRepository.findAllByMonth(dateRange.fromDate(), dateRange.toDate(), productId, getValidPageable(pageable))
                .map(SaleResponse::new);
    }
}
