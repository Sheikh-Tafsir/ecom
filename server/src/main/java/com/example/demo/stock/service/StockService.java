package com.example.demo.stock.service;

import com.example.demo.common.dto.DateRangeDto;
import com.example.demo.common.model.*;
import com.example.demo.common.service.MessageService;
import com.example.demo.product.service.ProductService;
import com.example.demo.sale.SaleService;
import com.example.demo.stock.dto.*;
import com.example.demo.stock.repository.StockItemRepository;
import com.example.demo.stock.repository.StockRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.example.demo.common.utils.DateUtils.resolveDates;
import static com.example.demo.common.utils.SecurityConstants.HAS_ROLE_ADMIN;
import static com.example.demo.common.utils.Utils.getValidPageable;

@Service
@RequiredArgsConstructor
public class StockService {

    private final StockRepository stockRepository;

    private final StockItemRepository stockItemRepository;

    private final ProductService productService;

    private final SaleService saleService;

    private final MessageService messageService;

    @PreAuthorize(HAS_ROLE_ADMIN)
    public Page<StockListResponse> findAll(LocalDateTime fromDate, LocalDateTime toDate, Pageable pageable) {
        DateRangeDto dateRange = resolveDates(fromDate, toDate);
        return stockRepository.findAll(dateRange.fromDate(), dateRange.toDate(), getValidPageable(pageable)).map(StockListResponse::new);
    }

    @PreAuthorize(HAS_ROLE_ADMIN)
    public StockResponse findById(Long id) {
        Stock stock = stockRepository.findDetailsById(id)
                .orElseThrow(() -> new EntityNotFoundException(messageService.get("error.entity.not.found", "Stock", id)));
        return new StockResponse(stock);
    }

    @PreAuthorize(HAS_ROLE_ADMIN)
    @Transactional
    public long create(CreateStockRequest request) {
        Stock stock = new Stock();

        request.items().forEach(itemRequest -> {
            Product product = productService.updateForStock(itemRequest);

            stock.addItem(product, itemRequest.quantity(), itemRequest.purchasePrice());
        });

        stockRepository.save(stock);
        return stock.getId();
    }

    @PreAuthorize(HAS_ROLE_ADMIN)
    @Transactional
    public StockResponse update(Long id, UpdateStockRequest request) {
        Stock stock = findByIdHelper(id);

        List<StockItem> removedItems = stock.getItems().stream()
                .filter(item -> request.items().stream()
                        .noneMatch(itemRequest ->
                                item.getId().equals(itemRequest.id())
                        )
                )
                .toList();

        removedItems.forEach(item -> {
            decreaseProductQuantity(item.getProduct(), item.getRemaining());
            stock.removeItem(item);
        });

        request.items().forEach(itemRequest ->
                updateItem(stock, itemRequest)
        );

        stock.calculateTotal();

        return new StockResponse(stockRepository.save(stock));
    }

    @PreAuthorize(HAS_ROLE_ADMIN)
    public Page<StockItemResponse> findAllItems(LocalDateTime fromDate, LocalDateTime toDate, Long productId, Pageable pageable) {
        DateRangeDto dateRange = resolveDates(fromDate, toDate);
        return stockItemRepository.findAll(dateRange.fromDate(), dateRange.toDate(), productId, getValidPageable(pageable)).map(StockItemResponse::new);
    }

    @PreAuthorize(HAS_ROLE_ADMIN)
    @Transactional
    public StockResponse addItem(Long stockId, CreateStockItemRequest request) {
        Stock stock = findByIdHelper(stockId);
        ensureProductDoesNotExist(stock, request.productId());
        addItem(stock, request);

        return new StockResponse(stockRepository.save(stock));
    }

    @PreAuthorize(HAS_ROLE_ADMIN)
    @Transactional
    public StockResponse updateItem(Long stockId, Long itemId, UpdateStockItemRequest request) {
        Stock stock = findByIdHelper(stockId);
        updateItem(stock, itemId, request);
        stock.calculateTotal();

        return new StockResponse(stockRepository.save(stock));
    }

    @PreAuthorize(HAS_ROLE_ADMIN)
    @Transactional
    public StockResponse removeItem(Long stockId, Long itemId) {
        Stock stock = findByIdHelper(stockId);
        StockItem item = getItem(stock, itemId);
        decreaseProductQuantity(item.getProduct(), item.getRemaining());
        stock.removeItem(item);

        return new StockResponse(stockRepository.save(stock));
    }

    @PreAuthorize(HAS_ROLE_ADMIN)
    @Transactional
    public void delete(Long id) {
        Stock stock = findByIdHelper(id);
        stock.getItems().forEach(item ->
                decreaseProductQuantity(item.getProduct(), item.getRemaining())
        );

        stockRepository.delete(stock);
    }

    public void consume(Product product, int quantityToConsume, Order order) {
        List<StockItem> stockItems = stockItemRepository.findAvailableByProductIdOrderByOldest(product.getId());

        List<Sale> sales = new ArrayList<>();

        for (StockItem item : stockItems) {
            if (quantityToConsume <= 0) {
                break;
            }

            int available = item.getRemaining();
            int consumed = Math.min(available, quantityToConsume);

            item.setRemaining(available - consumed);
            quantityToConsume -= consumed;

            sales.add(saleService.add(product, order, consumed, product.getPrice().subtract(item.getPurchasePrice())));
        }

        if (quantityToConsume > 0) {
            throw new ValidationException("Insufficient stock for product id: " + product.getId());
        }

        if (!sales.isEmpty()) {
            saleService.createAll(sales);
        }
    }

    private Stock findByIdHelper(Long id) {
        return stockRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(messageService.get("error.entity.not.found", "Stock", id)));
    }

    private void addItem(Stock stock, CreateStockItemRequest request) {
        Product product = productService.findByIdHelper(request.productId());
        product.setQuantity(product.getQuantity() + request.quantity());
        stock.addItem(product, request.quantity(), request.purchasePrice());
    }

    private void decreaseProductQuantity(Product product, int quantity) {
        if (product.getQuantity() < quantity) {
            throw new ValidationException("Product stock is not available for product id: " + product.getId());
        }

        product.setQuantity(product.getQuantity() - quantity);
    }

    private void updateItem(Stock stock, UpdateStockItemRequest request) {
        updateItem(stock, request.id(), request);
    }

    private void ensureProductDoesNotExist(Stock stock, Long productId) {
        boolean exists = stock.getItems().stream()
                .anyMatch(item ->
                        item.getProduct().getId().equals(productId)
                );

        if (exists) {
            throw new ValidationException("Duplicate product is not allowed");
        }
    }

    private void updateItem(Stock stock, Long itemId, UpdateStockItemRequest request) {
        StockItem item = getItem(stock, itemId);
        int quantityChange = request.quantity() - item.getQuantity();
        int updatedRemaining = item.getRemaining() + quantityChange;

        if (updatedRemaining < 0) {
            throw new IllegalArgumentException("Quantity cannot be less than consumed stock");
        }

        item.setQuantity(request.quantity());
        item.setPurchasePrice(request.purchasePrice());
        item.setRemaining(updatedRemaining);

        applyProductQuantityChange(item.getProduct(), quantityChange);
    }

    private StockItem getItem(Stock stock, Long itemId) {
        return stock.getItems().stream()
                .filter(item ->
                        item.getId().equals(itemId)
                )
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException(messageService.get("error.entity.not.found", "StockItem", itemId)));
    }

    private void applyProductQuantityChange(Product product, int quantityChange) {
        if (quantityChange > 0) {
            product.setQuantity(product.getQuantity() + quantityChange);
            return;
        }

        if (quantityChange < 0) {
            decreaseProductQuantity(product, quantityChange * -1);
        }
    }
}
