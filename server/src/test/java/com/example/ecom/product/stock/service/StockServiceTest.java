package com.example.ecom.product.stock.service;

import com.example.ecom.common.model.*;
import com.example.ecom.common.service.IdempotencyService;
import com.example.ecom.common.service.MessageService;
import com.example.ecom.product.product.service.ProductService;
import com.example.ecom.product.stock.dto.*;
import com.example.ecom.sale.SaleService;
import com.example.ecom.product.stock.repository.StockItemRepository;
import com.example.ecom.product.stock.repository.StockRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockServiceTest {

    @Mock
    private StockRepository stockRepository;

    @Mock
    private StockItemRepository stockItemRepository;

    @Mock
    private ProductService productService;

    @Mock
    private SaleService saleService;

    @Mock
    private MessageService messageService;

    @Mock
    private IdempotencyService idempotencyService;

    @InjectMocks
    private StockService stockService;

    private static final UUID STOCK_ID = UUID.fromString("a1a1a1a1-b2b2-c3c3-d4d4-e5e5e5e5e5e5");
    private static final UUID PRODUCT_ID = UUID.fromString("b2b2b2b2-c3c3-d4d4-e5e5-f6f6f6f6f6f6");
    private static final UUID PRODUCT_ID_2 = UUID.fromString("c3c3c3c3-d4d4-e5e5-f6f6-a7a7a7a7a7a7");
    private static final UUID ITEM_ID = UUID.fromString("d4d4d4d4-e5e5-f6f6-a7a7-b8b8b8b8b8b8");
    private static final UUID ITEM_ID_2 = UUID.fromString("e5e5e5e5-f6f6-a7a7-b8b8-c9c9c9c9c9c9");
    private static final UUID ORDER_ID = UUID.fromString("f6f6f6f6-a7a7-b8b8-c9c9-d0d0d0d0d0d0");

    private Product product;
    private Stock stock;
    private StockItem stockItem;

    @BeforeEach
    void setUp() {
        product = new Product();
        product.setId(PRODUCT_ID);
        product.setName("Wireless Mouse");
        product.setPrice(new BigDecimal("29.99"));
        product.setQuantity(100);

        stock = new Stock();
        stock.setId(STOCK_ID);
        stock.setItems(new HashSet<>());
        stock.setTotalCost(BigDecimal.ZERO);

        stockItem = new StockItem();
        stockItem.setId(ITEM_ID);
        stockItem.setStock(stock);
        stockItem.setProduct(product);
        stockItem.setQuantity(20);
        stockItem.setPurchasePrice(new BigDecimal("15.00"));
        stockItem.setRemaining(20);

        stock.getItems().add(stockItem);
    }

    @Test
    @DisplayName("findById should return StockResponse when stock exists")
    void findById_stockExists_returnsStockResponse() {
        when(stockRepository.findDetailsById(STOCK_ID)).thenReturn(Optional.of(stock));

        StockResponse result = stockService.findById(STOCK_ID);

        assertNotNull(result);
        assertEquals(STOCK_ID, result.getId());
        verify(stockRepository).findDetailsById(STOCK_ID);
    }

    @Test
    @DisplayName("findById should throw EntityNotFoundException when stock not found")
    void findById_stockNotFound_throwsEntityNotFoundException() {
        when(stockRepository.findDetailsById(STOCK_ID)).thenReturn(Optional.empty());
        when(messageService.get(eq("error.entity.not.found"), eq("Stock"), eq(STOCK_ID)))
                .thenReturn("Stock not found");

        assertThrows(EntityNotFoundException.class, () -> stockService.findById(STOCK_ID));
        verify(stockRepository).findDetailsById(STOCK_ID);
    }

    @Test
    @DisplayName("create should save stock and update products when no idempotency cache hit")
    void create_noCacheHit_savesStockAndUpdatesProducts() {
        CreateStockItemRequest itemReq = new CreateStockItemRequest(PRODUCT_ID, 10, new BigDecimal("12.00"));
        CreateStockRequest request = new CreateStockRequest(List.of(itemReq));

        when(idempotencyService.getCachedResponse("idem-key-1", request)).thenReturn(null);
        when(productService.updateForStock(itemReq)).thenReturn(product);
        when(stockRepository.save(any(Stock.class))).thenAnswer(invocation -> {
            Stock s = invocation.getArgument(0);
            s.setId(STOCK_ID);
            return s;
        });

        UUID result = stockService.create(request, "idem-key-1");

        assertNotNull(result);
        verify(idempotencyService).getCachedResponse("idem-key-1", request);
        verify(productService).updateForStock(itemReq);
        verify(stockRepository).save(any(Stock.class));
    }

    @Test
    @DisplayName("create should return cached response when idempotency key matches")
    void create_cacheHit_returnsCachedId() {
        CreateStockItemRequest itemReq = new CreateStockItemRequest(PRODUCT_ID, 10, new BigDecimal("12.00"));
        CreateStockRequest request = new CreateStockRequest(List.of(itemReq));

        when(idempotencyService.getCachedResponse("idem-key-2", request)).thenReturn(STOCK_ID);

        UUID result = stockService.create(request, "idem-key-2");

        assertEquals(STOCK_ID, result);
        verify(productService, never()).updateForStock(any());
        verify(stockRepository, never()).save(any());
    }

    @Test
    @DisplayName("create should save without idempotency caching when key is null")
    void create_nullIdempotencyKey_savesWithoutCaching() {
        CreateStockItemRequest itemReq = new CreateStockItemRequest(PRODUCT_ID, 5, new BigDecimal("10.00"));
        CreateStockRequest request = new CreateStockRequest(List.of(itemReq));

        when(idempotencyService.getCachedResponse(null, request)).thenReturn(null);
        when(productService.updateForStock(itemReq)).thenReturn(product);
        when(stockRepository.save(any(Stock.class))).thenAnswer(invocation -> {
            Stock s = invocation.getArgument(0);
            s.setId(STOCK_ID);
            return s;
        });

        UUID result = stockService.create(request, null);

        assertNotNull(result);
        verify(idempotencyService, never()).save(any(), any(), any());
    }

    @Test
    @DisplayName("delete should decrease product quantities and delete stock")
    void delete_existingStock_decreasesQuantitiesAndDeletes() {
        when(stockRepository.findById(STOCK_ID)).thenReturn(Optional.of(stock));

        stockService.delete(STOCK_ID);

        verify(productService).increaseQuantity(product, -20);
        verify(stockRepository).delete(stock);
    }

    @Test
    @DisplayName("delete should throw EntityNotFoundException when stock not found")
    void delete_stockNotFound_throwsEntityNotFoundException() {
        when(stockRepository.findById(STOCK_ID)).thenReturn(Optional.empty());
        when(messageService.get(eq("error.entity.not.found"), eq("Stock"), eq(STOCK_ID)))
                .thenReturn("Stock not found");

        assertThrows(EntityNotFoundException.class, () -> stockService.delete(STOCK_ID));
        verify(stockRepository, never()).delete(any());
    }

    @Test
    @DisplayName("addItem should add new item to stock and update product quantity")
    void addItem_newProduct_addsItemAndUpdatesProduct() {
        Product product2 = new Product();
        product2.setId(PRODUCT_ID_2);
        product2.setName("Keyboard");
        product2.setQuantity(50);

        CreateStockItemRequest itemReq = new CreateStockItemRequest(PRODUCT_ID_2, 15, new BigDecimal("20.00"));

        when(stockRepository.findById(STOCK_ID)).thenReturn(Optional.of(stock));
        when(productService.findByIdHelper(PRODUCT_ID_2)).thenReturn(product2);
        when(stockRepository.save(any(Stock.class))).thenReturn(stock);

        StockResponse result = stockService.addItem(STOCK_ID, itemReq);

        assertNotNull(result);
        verify(productService).findByIdHelper(PRODUCT_ID_2);
        verify(productService).increaseQuantity(product2, 15);
        verify(stockRepository).save(stock);
    }

    @Test
    @DisplayName("addItem should throw ValidationException when product already exists in stock")
    void addItem_duplicateProduct_throwsValidationException() {
        CreateStockItemRequest itemReq = new CreateStockItemRequest(PRODUCT_ID, 10, new BigDecimal("12.00"));

        when(stockRepository.findById(STOCK_ID)).thenReturn(Optional.of(stock));

        assertThrows(ValidationException.class, () -> stockService.addItem(STOCK_ID, itemReq));
        verify(stockRepository, never()).save(any());
    }

    @Test
    @DisplayName("removeItem should remove item from stock and decrease product quantity")
    void removeItem_existingItem_removesAndDecreasesQuantity() {
        when(stockRepository.findById(STOCK_ID)).thenReturn(Optional.of(stock));
        when(stockRepository.save(any(Stock.class))).thenReturn(stock);

        StockResponse result = stockService.removeItem(STOCK_ID, ITEM_ID);

        assertNotNull(result);
        verify(productService).increaseQuantity(product, -20);
        verify(stockRepository).save(stock);
    }

    @Test
    @DisplayName("removeItem should throw EntityNotFoundException when item not found in stock")
    void removeItem_itemNotFound_throwsEntityNotFoundException() {
        UUID nonExistentItemId = UUID.fromString("99999999-9999-9999-9999-999999999999");
        when(stockRepository.findById(STOCK_ID)).thenReturn(Optional.of(stock));
        when(messageService.get(eq("error.entity.not.found"), eq("StockItem"), eq(nonExistentItemId)))
                .thenReturn("StockItem not found");

        assertThrows(EntityNotFoundException.class, () -> stockService.removeItem(STOCK_ID, nonExistentItemId));
    }

    @Test
    @DisplayName("updateItem should update quantity and purchase price of existing item")
    void updateItem_validRequest_updatesItemFields() {
        UpdateStockItemRequest updateReq = new UpdateStockItemRequest(ITEM_ID, 25, new BigDecimal("18.00"));

        when(stockRepository.findById(STOCK_ID)).thenReturn(Optional.of(stock));
        when(stockRepository.save(any(Stock.class))).thenReturn(stock);

        StockResponse result = stockService.updateItem(STOCK_ID, ITEM_ID, updateReq);

        assertNotNull(result);
        assertEquals(25, stockItem.getQuantity());
        assertEquals(new BigDecimal("18.00"), stockItem.getPurchasePrice());
        assertEquals(25, stockItem.getRemaining());
        verify(productService).increaseQuantity(product, 5);
        verify(stockRepository).save(stock);
    }

    @Test
    @DisplayName("updateItem should throw IllegalArgumentException when new quantity would make remaining negative")
    void updateItem_quantityTooLow_throwsIllegalArgumentException() {
        stockItem.setRemaining(5);
        UpdateStockItemRequest updateReq = new UpdateStockItemRequest(ITEM_ID, 2, new BigDecimal("15.00"));

        when(stockRepository.findById(STOCK_ID)).thenReturn(Optional.of(stock));

        assertThrows(IllegalArgumentException.class, () -> stockService.updateItem(STOCK_ID, ITEM_ID, updateReq));
        verify(stockRepository, never()).save(any());
    }

    @Test
    @DisplayName("consume should consume stock from oldest items first and create sales")
    void consume_sufficientStock_consumesAndCreatesSales() {
        Order order = new Order();
        order.setId(ORDER_ID);

        StockItem item1 = new StockItem();
        item1.setId(ITEM_ID);
        item1.setProduct(product);
        item1.setRemaining(5);
        item1.setPurchasePrice(new BigDecimal("15.00"));

        StockItem item2 = new StockItem();
        item2.setId(ITEM_ID_2);
        item2.setProduct(product);
        item2.setRemaining(10);
        item2.setPurchasePrice(new BigDecimal("14.00"));

        when(stockItemRepository.findAvailableByProductIdOrderByOldest(PRODUCT_ID))
                .thenReturn(List.of(item1, item2));

        Sale sale1 = new Sale();
        Sale sale2 = new Sale();
        when(saleService.add(eq(product), eq(order), eq(5), any(BigDecimal.class))).thenReturn(sale1);
        when(saleService.add(eq(product), eq(order), eq(3), any(BigDecimal.class))).thenReturn(sale2);

        stockService.consume(product, 8, order);

        assertEquals(0, item1.getRemaining());
        assertEquals(7, item2.getRemaining());
        verify(saleService).createAll(List.of(sale1, sale2));
    }

    @Test
    @DisplayName("consume should throw ValidationException when stock is insufficient")
    void consume_insufficientStock_throwsValidationException() {
        Order order = new Order();
        order.setId(ORDER_ID);

        StockItem item1 = new StockItem();
        item1.setId(ITEM_ID);
        item1.setProduct(product);
        item1.setRemaining(3);
        item1.setPurchasePrice(new BigDecimal("15.00"));

        when(stockItemRepository.findAvailableByProductIdOrderByOldest(PRODUCT_ID))
                .thenReturn(List.of(item1));
        when(saleService.add(eq(product), eq(order), eq(3), any(BigDecimal.class))).thenReturn(new Sale());

        assertThrows(ValidationException.class, () -> stockService.consume(product, 10, order));
        verify(stockItemRepository).findAvailableByProductIdOrderByOldest(PRODUCT_ID);
    }

    @Test
    @DisplayName("consume should not create sales when no items are consumed")
    void consume_zeroQuantity_noSalesCreated() {
        Order order = new Order();
        order.setId(ORDER_ID);

        when(stockItemRepository.findAvailableByProductIdOrderByOldest(PRODUCT_ID))
                .thenReturn(List.of());

        stockService.consume(product, 0, order);

        verify(saleService, never()).createAll(any());
    }

    @Test
    @DisplayName("update should remove items not in request and update existing ones")
    void update_removesAbsentItemsAndUpdatesPresent() {
        StockItem item2 = new StockItem();
        item2.setId(ITEM_ID_2);
        item2.setStock(stock);

        Product product2 = new Product();
        product2.setId(PRODUCT_ID_2);
        product2.setQuantity(30);
        item2.setProduct(product2);
        item2.setQuantity(10);
        item2.setPurchasePrice(new BigDecimal("12.00"));
        item2.setRemaining(10);

        stock.getItems().add(item2);

        UpdateStockItemRequest updateItem1 = new UpdateStockItemRequest(ITEM_ID, 25, new BigDecimal("16.00"));
        UpdateStockRequest request = new UpdateStockRequest(List.of(updateItem1));

        when(stockRepository.findById(STOCK_ID)).thenReturn(Optional.of(stock));
        when(stockRepository.save(any(Stock.class))).thenReturn(stock);

        StockResponse result = stockService.update(STOCK_ID, request);

        assertNotNull(result);
        verify(productService).increaseQuantity(product2, -10);
        verify(productService).increaseQuantity(product, 5);
    }

    @Test
    @DisplayName("delete should throw ValidationException when product quantity is less than stock remaining")
    void delete_productQuantityInsufficient_throwsValidationException() {
        product.setQuantity(5);
        stockItem.setRemaining(20);

        when(stockRepository.findById(STOCK_ID)).thenReturn(Optional.of(stock));

        assertThrows(ValidationException.class, () -> stockService.delete(STOCK_ID));
    }
}
