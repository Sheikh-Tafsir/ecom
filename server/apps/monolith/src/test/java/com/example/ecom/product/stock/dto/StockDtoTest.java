package com.example.ecom.product.stock.dto;

import com.example.ecom.product.product.model.Product;
import com.example.ecom.product.stock.model.Stock;
import com.example.ecom.product.stock.model.StockItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class StockDtoTest {

    private UUID stockId;
    private UUID productId;
    private Instant now;

    @BeforeEach
    void setUp() {
        stockId = UUID.fromString("01927d70-097f-7afa-9ad9-526374859607");
        productId = UUID.fromString("01927d70-1a8f-7bfa-8ada-637485960718");
        now = Instant.parse("2025-09-01T08:00:00Z");
    }

    @Nested
    @DisplayName("CreateStockItemRequest Tests")
    class CreateStockItemRequestTest {

        @Test
        @DisplayName("Record constructor sets all fields")
        void constructor_validArgs_setsFields() {
            CreateStockItemRequest request = new CreateStockItemRequest(productId, 25, new BigDecimal("45.00"));

            assertEquals(productId, request.productId());
            assertEquals(25, request.quantity());
            assertEquals(new BigDecimal("45.00"), request.purchasePrice());
        }

        @Test
        @DisplayName("Two equal records are equal")
        void equals_sameFields_returnsTrue() {
            CreateStockItemRequest r1 = new CreateStockItemRequest(productId, 10, new BigDecimal("20.00"));
            CreateStockItemRequest r2 = new CreateStockItemRequest(productId, 10, new BigDecimal("20.00"));

            assertEquals(r1, r2);
            assertEquals(r1.hashCode(), r2.hashCode());
        }
    }

    @Nested
    @DisplayName("CreateStockRequest Tests")
    class CreateStockRequestTest {

        @Test
        @DisplayName("Record constructor sets items list")
        void constructor_validArgs_setsFields() {
            List<CreateStockItemRequest> items = List.of(
                    new CreateStockItemRequest(productId, 10, new BigDecimal("30.00"))
            );
            CreateStockRequest request = new CreateStockRequest(items);

            assertEquals(1, request.items().size());
        }
    }

    @Nested
    @DisplayName("UpdateStockItemRequest Tests")
    class UpdateStockItemRequestTest {

        @Test
        @DisplayName("Record constructor sets all fields")
        void constructor_validArgs_setsFields() {
            UUID itemId = UUID.fromString("01927d70-2b9f-7cfa-7adb-748596071829");
            UpdateStockItemRequest request = new UpdateStockItemRequest(itemId, 15, new BigDecimal("50.00"));

            assertEquals(itemId, request.id());
            assertEquals(15, request.quantity());
            assertEquals(new BigDecimal("50.00"), request.purchasePrice());
        }

        @Test
        @DisplayName("Record allows null id for new items")
        void constructor_nullId_allowsNull() {
            UpdateStockItemRequest request = new UpdateStockItemRequest(null, 10, new BigDecimal("25.00"));
            assertNull(request.id());
        }
    }

    @Nested
    @DisplayName("UpdateStockRequest Tests")
    class UpdateStockRequestTest {

        @Test
        @DisplayName("Record constructor sets items list")
        void constructor_validArgs_setsFields() {
            List<UpdateStockItemRequest> items = List.of(
                    new UpdateStockItemRequest(null, 5, new BigDecimal("10.00"))
            );
            UpdateStockRequest request = new UpdateStockRequest(items);

            assertEquals(1, request.items().size());
        }
    }

    @Nested
    @DisplayName("StockItemResponse Tests")
    class StockItemResponseTest {

        @Test
        @DisplayName("Construction from StockItem entity maps all fields")
        void constructor_fromEntity_setsAllFields() {
            UUID itemId = UUID.fromString("01927d70-3caf-7dfa-6adc-859607182930");
            Stock stock = new Stock();
            stock.setId(stockId);

            Product product = new Product();
            product.setId(productId);
            product.setName("USB Cable");

            StockItem item = new StockItem();
            item.setId(itemId);
            item.setStock(stock);
            item.setProduct(product);
            item.setQuantity(100);
            item.setPurchasePrice(new BigDecimal("5.00"));
            item.setRemaining(80);

            StockItemResponse response = new StockItemResponse(item);

            assertEquals(itemId, response.getId());
            assertEquals(stockId, response.getStockId());
            assertEquals(productId, response.getProductId());
            assertEquals("USB Cable", response.getProductName());
            assertEquals(100, response.getQuantity());
            assertEquals(new BigDecimal("5.00"), response.getPurchasePrice());
            assertEquals(80, response.getRemaining());
            assertEquals(new BigDecimal("500.00"), response.getSubtotal());
        }

        @Test
        @DisplayName("Construction from StockItem with null stock and product handles nulls")
        void constructor_fromEntityNulls_handlesGracefully() {
            StockItem item = new StockItem();
            item.setId(UUID.fromString("01927d70-4dbf-7efa-5add-960718293041"));
            item.setStock(null);
            item.setProduct(null);
            item.setQuantity(10);
            item.setPurchasePrice(new BigDecimal("10.00"));

            StockItemResponse response = new StockItemResponse(item);

            assertNull(response.getStockId());
            assertNull(response.getProductId());
            assertNull(response.getProductName());
        }
    }

    @Nested
    @DisplayName("StockListResponse Tests")
    class StockListResponseTest {

        @Test
        @DisplayName("Construction from Stock entity maps fields")
        void constructor_fromEntity_setsFields() {
            Stock stock = new Stock();
            stock.setId(stockId);
            stock.setTotalCost(new BigDecimal("5000.00"));
            stock.setCreatedAt(now);
            stock.setUpdatedAt(now);

            StockListResponse response = new StockListResponse(stock);

            assertEquals(stockId, response.getId());
            assertEquals(new BigDecimal("5000.00"), response.getTotalCost());
            assertEquals(now, response.getCreatedAt());
            assertEquals(now, response.getUpdatedAt());
        }
    }

    @Nested
    @DisplayName("StockResponse Tests")
    class StockResponseTest {

        @Test
        @DisplayName("Construction from Stock entity maps items and fields")
        void constructor_fromEntity_setsAllFields() {
            Product product = new Product();
            product.setId(productId);
            product.setName("HDMI Cable");

            Stock stock = new Stock();
            stock.setId(stockId);
            stock.setCreatedAt(now);
            stock.setUpdatedAt(now);
            stock.addItem(product, 20, new BigDecimal("15.00"));

            StockResponse response = new StockResponse(stock);

            assertEquals(stockId, response.getId());
            assertEquals(new BigDecimal("300.00"), response.getTotalCost());
            assertEquals(1, response.getItems().size());
            assertEquals(now, response.getCreatedAt());
            assertEquals(now, response.getUpdatedAt());
        }
    }
}
