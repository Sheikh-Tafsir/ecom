package com.example.demo.stock.service;

import com.example.demo.common.dto.CustomUserDetails;
import com.example.demo.common.model.Product;
import com.example.demo.common.model.Stock;
import com.example.demo.common.model.StockItem;
import com.example.demo.common.service.MessageService;
import com.example.demo.product.service.ProductService;
import com.example.demo.stock.dto.CreateStockItemRequest;
import com.example.demo.stock.dto.CreateStockRequest;
import com.example.demo.stock.dto.StockResponse;
import com.example.demo.stock.dto.UpdateStockItemRequest;
import com.example.demo.stock.dto.UpdateStockRequest;
import com.example.demo.stock.repository.StockRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.example.demo.common.utils.SecurityConstants.HAS_ROLE_ADMIN;
import static com.example.demo.common.utils.Utils.getValidPageable;

@Service
@RequiredArgsConstructor
public class StockService {

    private final StockRepository stockRepository;

    private final ProductService productService;

    private final MessageService messageService;

    @PreAuthorize(HAS_ROLE_ADMIN)
    public Page<StockResponse> findAll(Pageable pageable) {
        return stockRepository.findAll(getValidPageable(pageable)).map(StockResponse::new);
    }

    @PreAuthorize(HAS_ROLE_ADMIN)
    public StockResponse findById(Long id) {
        return new StockResponse(findByIdHelper(id));
    }

    @PreAuthorize(HAS_ROLE_ADMIN)
    @Transactional
    public StockResponse create(CreateStockRequest request, CustomUserDetails userDetails) {
        Stock stock = new Stock();

        request.items().forEach(itemRequest -> {
            Product product = productService.findById(itemRequest.productId(), userDetails);
            product.setQuantity(product.getQuantity() + itemRequest.quantity());

            stock.addItem(product, itemRequest.quantity(), itemRequest.cost());
        });

        return new StockResponse(stockRepository.save(stock));
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

    private Stock findByIdHelper(Long id) {
        return stockRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(messageService.get("error.entity.not.found", "Stock", id)));
    }

    private void addItem(Stock stock, CreateStockItemRequest request) {
        Product product = productService.findByIdHelper(request.productId());
        product.setQuantity(product.getQuantity() + request.quantity());
        stock.addItem(product, request.quantity(), request.cost());
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
        int quantityDelta = request.quantity() - item.getQuantity();
        int updatedRemaining = item.getRemaining() + quantityDelta;

        if (updatedRemaining < 0) {
            throw new IllegalArgumentException("Quantity cannot be less than consumed stock");
        }

        item.setQuantity(request.quantity());
        item.setCost(request.cost());
        item.setRemaining(updatedRemaining);

        applyProductQuantityDelta(item.getProduct(), quantityDelta);
    }

    private StockItem getItem(Stock stock, Long itemId) {
        return stock.getItems().stream()
                .filter(item ->
                        item.getId().equals(itemId)
                )
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException(messageService.get("error.entity.not.found", "StockItem", itemId)));
    }

    private void applyProductQuantityDelta(Product product, int quantityDelta) {
        if (quantityDelta > 0) {
            product.setQuantity(product.getQuantity() + quantityDelta);
            return;
        }

        if (quantityDelta < 0) {
            decreaseProductQuantity(product, quantityDelta * -1);
        }
    }
}
