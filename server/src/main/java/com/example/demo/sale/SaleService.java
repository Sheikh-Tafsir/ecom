package com.example.demo.sale;

import com.example.demo.common.model.Product;
import com.example.demo.common.model.Sale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SaleService {

    private final SaleRepository saleRepository;

    public Sale add(Product product, int quantity, BigDecimal unitProfit) {
        Sale sale = new Sale();
        sale.setProduct(product);
        sale.setQuantity(quantity);
        sale.setProfit(unitProfit.multiply(BigDecimal.valueOf(quantity)));

        return sale;
    }

    public void createAll(List<Sale> sales) {
        saleRepository.saveAll(sales);
    }
}
