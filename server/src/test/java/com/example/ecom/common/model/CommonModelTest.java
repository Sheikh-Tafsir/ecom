package com.example.ecom.common.model;

import com.example.ecom.cms.banner.model.Banner;
import com.example.ecom.cms.blogs.enums.BlogPostStatus;
import com.example.ecom.cms.blogs.model.Blogs;
import com.example.ecom.cms.faq.model.Faq;
import com.example.ecom.common.enums.Permission;
import com.example.ecom.order.dto.ProductDto;
import com.example.ecom.order.enums.OrderStatus;
import com.example.ecom.order.enums.PaymentMethod;
import com.example.ecom.order.model.Order;
import com.example.ecom.order.model.OrderItem;
import com.example.ecom.order.model.OrderStatusHistory;
import com.example.ecom.payment.enums.PaymentStatus;
import com.example.ecom.payment.model.Payment;
import com.example.ecom.product.category.model.Category;
import com.example.ecom.product.product.enums.ProductStatus;
import com.example.ecom.product.product.model.Product;
import com.example.ecom.product.product.model.ProductImage;
import com.example.ecom.product.review.model.Review;
import com.example.ecom.product.sale.model.Sale;
import com.example.ecom.product.stock.model.Stock;
import com.example.ecom.product.stock.model.StockItem;
import com.example.ecom.user.user.enums.Gender;
import com.example.ecom.user.user.enums.UserRefreshTokenStatus;
import com.example.ecom.common.enums.UserStatus;
import com.example.ecom.user.user.model.Role;
import com.example.ecom.user.user.model.User;
import com.example.ecom.user.user.model.UserRefreshToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CommonModelTest {

    @Nested
    @DisplayName("BaseEntity Tests")
    class BaseEntityTest {

        private ConcreteBaseEntity entity;

        @BeforeEach
        void setUp() {
            entity = new ConcreteBaseEntity();
        }

        @Test
        @DisplayName("ensureId generates a UUID when id is null")
        void ensureId_whenIdNull_generatesUuid() {
            assertNull(entity.getId());
            entity.ensureId();
            assertNotNull(entity.getId());
        }

        @Test
        @DisplayName("ensureId preserves existing id")
        void ensureId_whenIdExists_preservesId() {
            UUID existingId = UUID.fromString("01927d6c-5ecf-7ffa-5ab6-071829304152");
            entity.setId(existingId);
            entity.ensureId();
            assertEquals(existingId, entity.getId());
        }

        @Test
        @DisplayName("Getters and setters work for all fields")
        void gettersSetters_allFields_workCorrectly() {
            UUID id = UUID.fromString("01927d6c-6fdf-70fa-4ab7-182930415263");
            Instant now = Instant.now();

            entity.setId(id);
            entity.setVersion(3);
            entity.setCreatedAt(now);
            entity.setUpdatedAt(now);

            assertEquals(id, entity.getId());
            assertEquals(3, entity.getVersion());
            assertEquals(now, entity.getCreatedAt());
            assertEquals(now, entity.getUpdatedAt());
        }

        static class ConcreteBaseEntity extends BaseEntity {}
    }

    @Nested
    @DisplayName("AuditableEntity Tests")
    class AuditableEntityTest {

        @Test
        @DisplayName("Getters and setters work for createdBy and updatedBy")
        void gettersSetters_auditFields_workCorrectly() {
            ConcreteAuditableEntity entity = new ConcreteAuditableEntity();
            UUID creatorId = UUID.fromString("01927d6c-70ef-71fa-3ab8-293041526374");
            UUID updaterId = UUID.fromString("01927d6c-81ff-72fa-2ab9-304152637485");

            entity.setCreatedBy(creatorId);
            entity.setUpdatedBy(updaterId);

            assertEquals(creatorId, entity.getCreatedBy());
            assertEquals(updaterId, entity.getUpdatedBy());
        }

        @Test
        @DisplayName("AuditableEntity extends BaseEntity and inherits its fields")
        void inheritance_extendsBaseEntity_inheritsFields() {
            ConcreteAuditableEntity entity = new ConcreteAuditableEntity();
            UUID id = UUID.fromString("01927d6c-920f-73fa-1aba-415263748596");
            entity.setId(id);
            entity.setVersion(2);
            assertEquals(id, entity.getId());
            assertEquals(2, entity.getVersion());
        }

        static class ConcreteAuditableEntity extends AuditableEntity {}
    }

    @Nested
    @DisplayName("Order Model Tests")
    class OrderModelTest {

        private Order order;

        @BeforeEach
        void setUp() {
            order = new Order();
            order.setId(UUID.fromString("01927d6c-a31f-74fa-0abb-526374859607"));
        }

        @Test
        @DisplayName("Default values are initialized correctly")
        void defaults_newOrder_hasDefaultValues() {
            assertEquals(BigDecimal.ZERO, order.getTotalPrice());
            assertEquals(OrderStatus.PENDING, order.getStatus());
            assertEquals(PaymentMethod.CASH_ON_DELIVERY, order.getPaymentMethod());
            assertFalse(order.isPaid());
            assertNotNull(order.getItems());
            assertTrue(order.getItems().isEmpty());
            assertNotNull(order.getStatusHistories());
            assertTrue(order.getStatusHistories().isEmpty());
        }

        @Test
        @DisplayName("isCancelledOrRejected returns true for CANCELLED status")
        void isCancelledOrRejected_cancelled_returnsTrue() {
            order.setStatus(OrderStatus.CANCELLED);
            assertTrue(order.isCancelledOrRejected());
        }

        @Test
        @DisplayName("isCancelledOrRejected returns true for REJECTED status")
        void isCancelledOrRejected_rejected_returnsTrue() {
            order.setStatus(OrderStatus.REJECTED);
            assertTrue(order.isCancelledOrRejected());
        }

        @Test
        @DisplayName("isCancelledOrRejected returns false for PENDING status")
        void isCancelledOrRejected_pending_returnsFalse() {
            order.setStatus(OrderStatus.PENDING);
            assertFalse(order.isCancelledOrRejected());
        }

        @Test
        @DisplayName("isCancelledOrRejected returns false for DELIVERED status")
        void isCancelledOrRejected_delivered_returnsFalse() {
            order.setStatus(OrderStatus.DELIVERED);
            assertFalse(order.isCancelledOrRejected());
        }

        @Test
        @DisplayName("addItem adds a new item and calculates total")
        void addItem_newProduct_addsItemAndCalculatesTotal() {
            ProductDto productDto = new ProductDto(
                    UUID.fromString("01927d6c-b42f-75fa-9abc-637485960718"),
                    "Test Product", null, new BigDecimal("150.00"), 2);

            order.addItem(productDto);

            assertEquals(1, order.getItems().size());
            assertEquals(new BigDecimal("300.00"), order.getTotalPrice());
        }

        @Test
        @DisplayName("addItem increases quantity for existing product")
        void addItem_existingProduct_increasesQuantity() {
            UUID productId = UUID.fromString("01927d6c-c53f-76fa-8abd-748596071829");
            ProductDto productDto1 = new ProductDto(productId, "Test Product", null, new BigDecimal("100.00"), 2);
            ProductDto productDto2 = new ProductDto(productId, "Test Product", null, new BigDecimal("100.00"), 3);

            order.addItem(productDto1);
            order.addItem(productDto2);

            assertEquals(1, order.getItems().size());
            OrderItem item = order.getItems().iterator().next();
            assertEquals(5, item.getQuantity());
            assertEquals(new BigDecimal("500.00"), order.getTotalPrice());
        }

        @Test
        @DisplayName("addItem with multiple different products calculates total correctly")
        void addItem_multipleProducts_calculatesCorrectTotal() {
            ProductDto productDto1 = new ProductDto(
                    UUID.fromString("01927d6c-d64f-77fa-7abe-859607182930"),
                    "Product 1", null, new BigDecimal("50.00"), 3);
            ProductDto productDto2 = new ProductDto(
                    UUID.fromString("01927d6c-e75f-78fa-6abf-960718293041"),
                    "Product 2", null, new BigDecimal("200.00"), 1);

            order.addItem(productDto1);
            order.addItem(productDto2);

            assertEquals(2, order.getItems().size());
            assertEquals(new BigDecimal("350.00"), order.getTotalPrice());
        }

        @Test
        @DisplayName("Setters update all order fields")
        void setters_allFields_updateCorrectly() {
            UUID userId = UUID.fromString("01927d6c-f86f-79fa-5ac0-071829304152");

            order.setUserId(userId);
            order.setName("Jane Doe");
            order.setPhone("01712345678");
            order.setAddress("123 Main St, Dhaka");
            order.setStatus(OrderStatus.SHIPPED);
            order.setPaymentMethod(PaymentMethod.BKASH);
            order.setPaid(true);
            order.setIdempotencyKey("idem-key-12345");

            assertEquals(userId, order.getUserId());
            assertEquals("Jane Doe", order.getName());
            assertEquals("01712345678", order.getPhone());
            assertEquals("123 Main St, Dhaka", order.getAddress());
            assertEquals(OrderStatus.SHIPPED, order.getStatus());
            assertEquals(PaymentMethod.BKASH, order.getPaymentMethod());
            assertTrue(order.isPaid());
            assertEquals("idem-key-12345", order.getIdempotencyKey());
        }
    }

    @Nested
    @DisplayName("OrderItem Model Tests")
    class OrderItemModelTest {

        @Test
        @DisplayName("Constructor with product, quantity, order sets unitPrice from product")
        void constructor_withProductAndOrder_setsUnitPrice() {
            ProductDto productDto = new ProductDto(
                    UUID.randomUUID(), "Test Product", "img.jpg", new BigDecimal("99.99"), 3);
            Order order = new Order();

            OrderItem item = new OrderItem(productDto, 3, order);

            assertEquals(productDto.id(), item.getProductId());
            assertEquals(3, item.getQuantity());
            assertEquals(order, item.getOrder());
            assertEquals(new BigDecimal("99.99"), item.getUnitPrice());
        }

        @Test
        @DisplayName("getSubtotal returns unitPrice * quantity")
        void getSubtotal_multipleQuantity_calculatesCorrectly() {
            OrderItem item = new OrderItem();
            item.setUnitPrice(new BigDecimal("25.50"));
            item.setQuantity(4);

            assertEquals(new BigDecimal("102.00"), item.getSubtotal());
        }

        @Test
        @DisplayName("increaseQuantity adds to existing quantity")
        void increaseQuantity_addsFive_increasesCorrectly() {
            OrderItem item = new OrderItem();
            item.setQuantity(3);
            item.increaseQuantity(5);
            assertEquals(8, item.getQuantity());
        }

        @Test
        @DisplayName("decreaseQuantity subtracts from existing quantity")
        void decreaseQuantity_subtractsTwo_decreasesCorrectly() {
            OrderItem item = new OrderItem();
            item.setQuantity(10);
            item.decreaseQuantity(2);
            assertEquals(8, item.getQuantity());
        }
    }

    @Nested
    @DisplayName("OrderStatusHistory Model Tests")
    class OrderStatusHistoryModelTest {

        @Test
        @DisplayName("Custom constructor sets order, fromStatus, toStatus, changedById, comment")
        void constructor_withAllFields_setsCorrectly() {
            Order order = new Order();
            UUID userId = UUID.randomUUID();

            OrderStatusHistory history = new OrderStatusHistory(
                    order, OrderStatus.PENDING, OrderStatus.ACCEPTED, userId, "Order approved"
            );

            assertEquals(order, history.getOrder());
            assertEquals(OrderStatus.PENDING, history.getFromStatus());
            assertEquals(OrderStatus.ACCEPTED, history.getToStatus());
            assertEquals(userId, history.getChangedById());
            assertEquals("Order approved", history.getComment());
        }

        @Test
        @DisplayName("No-args constructor creates empty instance")
        void noArgsConstructor_createsEmptyInstance() {
            OrderStatusHistory history = new OrderStatusHistory();
            assertNull(history.getId());
            assertNull(history.getOrder());
            assertNull(history.getFromStatus());
            assertNull(history.getToStatus());
            assertNull(history.getChangedById());
            assertNull(history.getComment());
        }
    }

    @Nested
    @DisplayName("Product Model Tests")
    class ProductModelTest {

        @Test
        @DisplayName("Default values are initialized correctly")
        void defaults_newProduct_hasDefaultValues() {
            Product product = new Product();
            assertEquals(BigDecimal.ZERO, product.getPrice());
            assertEquals(BigDecimal.ZERO, product.getRating());
            assertEquals(ProductStatus.COMING_SOON, product.getStatus());
            assertEquals(0, product.getQuantity());
            assertEquals(0, product.getReviewCount());
            assertFalse(product.getDeleted());
        }

        @Test
        @DisplayName("addImage adds image and sets back-reference")
        void addImage_addsImageAndSetsProduct() {
            Product product = new Product();
            ProductImage image = new ProductImage();
            image.setImage("https://cdn.example.com/img1.jpg");

            product.addImage(image);

            assertEquals(1, product.getImages().size());
            assertEquals(product, image.getProduct());
        }

        @Test
        @DisplayName("isNotActive returns true when deleted")
        void isNotActive_deleted_returnsTrue() {
            Product product = new Product();
            product.setDeleted(true);
            assertTrue(product.isNotActive());
        }

        @Test
        @DisplayName("isNotActive returns true when DISCONTINUED")
        void isNotActive_discontinued_returnsTrue() {
            Product product = new Product();
            product.setDeleted(false);
            product.setStatus(ProductStatus.DISCONTINUED);
            assertTrue(product.isNotActive());
        }

        @Test
        @DisplayName("isNotActive returns false when active and not deleted")
        void isNotActive_available_returnsFalse() {
            Product product = new Product();
            product.setDeleted(false);
            product.setStatus(ProductStatus.AVAILABLE);
            assertFalse(product.isNotActive());
        }
    }

    @Nested
    @DisplayName("User Model Tests")
    class UserModelTest {

        private User user;

        @BeforeEach
        void setUp() {
            user = new User();
            user.setId(UUID.fromString("01927d6d-097f-7afa-4ac1-182930415263"));
            user.setName("Alice Smith");
            user.setEmail("alice@example.com");
            user.setPassword("hashedPassword123");
            user.setStatus(UserStatus.ACTIVE);
        }

        @Test
        @DisplayName("Default values are initialized correctly")
        void defaults_newUser_hasDefaultValues() {
            User newUser = new User();
            assertEquals(UserStatus.NOT_VERIFIED, newUser.getStatus());
            assertFalse(newUser.getDeleted());
            assertNotNull(newUser.getRoles());
        }

        @Test
        @DisplayName("isNotActive returns true for non-ACTIVE status")
        void isNotActive_suspended_returnsTrue() {
            user.setStatus(UserStatus.SUSPENDED);
            assertTrue(user.isNotActive());
        }

        @Test
        @DisplayName("isNotActive returns false for ACTIVE status")
        void isNotActive_active_returnsFalse() {
            assertTrue(user.getStatus() == UserStatus.ACTIVE);
            assertFalse(user.isNotActive());
        }

        @Test
        @DisplayName("getRoleValues returns set of role names")
        void getRoleValues_withRoles_returnsNames() {
            Role adminRole = new Role();
            adminRole.setName("ADMIN");
            Role userRole = new Role();
            userRole.setName("USER");
            user.setRoles(Set.of(adminRole, userRole));

            Set<String> roleValues = user.getRoleValues();

            assertEquals(2, roleValues.size());
            assertTrue(roleValues.contains("ADMIN"));
            assertTrue(roleValues.contains("USER"));
        }

        @Test
        @DisplayName("getPermissionValues returns flat set of permission values")
        void getPermissionValues_withRoles_returnsPermissions() {
            Role adminRole = new Role();
            adminRole.setName("ADMIN");
            adminRole.setPermissions(Set.of(Permission.ADMIN_ACCESS, Permission.USER));

            Role superAdminRole = new Role();
            superAdminRole.setName("SUPER_ADMIN");
            superAdminRole.setPermissions(Set.of(Permission.SUPER_ADMIN_ACCESS));

            user.setRoles(Set.of(adminRole, superAdminRole));

            Set<String> perms = user.getPermissionValues();

            assertEquals(3, perms.size());
            assertTrue(perms.contains("admin:access"));
            assertTrue(perms.contains("user:access"));
            assertTrue(perms.contains("super_admin:access"));
        }

        @Test
        @DisplayName("Gender can be set and retrieved")
        void gender_setAndGet_workCorrectly() {
            user.setGender(Gender.MALE);
            assertEquals(Gender.MALE, user.getGender());
        }
    }

    @Nested
    @DisplayName("Stock Model Tests")
    class StockModelTest {

        @Test
        @DisplayName("addItem adds item and calculates total cost")
        void addItem_addsItemAndCalculatesTotal() {
            Stock stock = new Stock();
            Product product = new Product();
            product.setId(UUID.fromString("01927d6d-1a8f-7bfa-3ac2-293041526374"));

            stock.addItem(product, 10, new BigDecimal("50.00"));

            assertEquals(1, stock.getItems().size());
            assertEquals(new BigDecimal("500.00"), stock.getTotalCost());
        }

        @Test
        @DisplayName("removeItem removes item and recalculates total")
        void removeItem_removesAndRecalculatesTotal() {
            Stock stock = new Stock();
            Product product = new Product();
            product.setId(UUID.fromString("01927d6d-2b9f-7cfa-2ac3-304152637485"));

            stock.addItem(product, 5, new BigDecimal("100.00"));
            StockItem item = stock.getItems().iterator().next();

            stock.removeItem(item);

            assertTrue(stock.getItems().isEmpty());
            assertEquals(BigDecimal.ZERO, stock.getTotalCost());
            assertNull(item.getStock());
        }

        @Test
        @DisplayName("Default totalCost is ZERO")
        void defaults_newStock_totalCostIsZero() {
            Stock stock = new Stock();
            assertEquals(BigDecimal.ZERO, stock.getTotalCost());
        }
    }

    @Nested
    @DisplayName("StockItem Model Tests")
    class StockItemModelTest {

        @Test
        @DisplayName("Custom constructor sets remaining equal to quantity")
        void constructor_setsRemainingEqualsQuantity() {
            Stock stock = new Stock();
            Product product = new Product();
            StockItem item = new StockItem(stock, product, 20, new BigDecimal("30.00"));

            assertEquals(20, item.getQuantity());
            assertEquals(20, item.getRemaining());
            assertEquals(new BigDecimal("30.00"), item.getPurchasePrice());
        }

        @Test
        @DisplayName("getSubtotal returns purchasePrice * quantity")
        void getSubtotal_calculatesCorrectly() {
            StockItem item = new StockItem();
            item.setPurchasePrice(new BigDecimal("15.00"));
            item.setQuantity(8);

            assertEquals(new BigDecimal("120.00"), item.getSubtotal());
        }
    }

    @Nested
    @DisplayName("Payment Model Tests")
    class PaymentModelTest {

        @Test
        @DisplayName("Default status is PENDING")
        void defaults_newPayment_statusIsPending() {
            Payment payment = new Payment();
            assertEquals(PaymentStatus.PENDING, payment.getStatus());
        }

        @Test
        @DisplayName("Setters update all fields correctly")
        void setters_allFields_updateCorrectly() {
            Payment payment = new Payment();
            UUID orderId = UUID.fromString("01927d6d-3caf-7dfa-1ac4-415263748596");
            payment.setOrderId(orderId);
            payment.setPaymentIntentId("pi_12345");
            payment.setTransactionId("tx_67890");
            payment.setMerchantInvoiceNumber("INV-001");
            payment.setAmount(new BigDecimal("999.99"));
            payment.setStatus(PaymentStatus.COMPLETED);

            assertEquals(orderId, payment.getOrderId());
            assertEquals("pi_12345", payment.getPaymentIntentId());
            assertEquals("tx_67890", payment.getTransactionId());
            assertEquals("INV-001", payment.getMerchantInvoiceNumber());
            assertEquals(new BigDecimal("999.99"), payment.getAmount());
            assertEquals(PaymentStatus.COMPLETED, payment.getStatus());
        }
    }

    @Nested
    @DisplayName("Review Model Tests")
    class ReviewModelTest {

        @Test
        @DisplayName("Setters and getters work for all fields")
        void gettersSetters_allFields_workCorrectly() {
            Review review = new Review();
            UUID userId = UUID.randomUUID();
            Product product = new Product();

            review.setUserId(userId);
            review.setUserName("Test User");
            review.setProduct(product);
            review.setRating(5);
            review.setComment("Excellent product!");

            assertEquals(userId, review.getUserId());
            assertEquals("Test User", review.getUserName());
            assertEquals(product, review.getProduct());
            assertEquals(5, review.getRating());
            assertEquals("Excellent product!", review.getComment());
        }
    }

    @Nested
    @DisplayName("Sale Model Tests")
    class SaleModelTest {

        @Test
        @DisplayName("Setters and getters work for all fields")
        void gettersSetters_allFields_workCorrectly() {
            Sale sale = new Sale();
            UUID id = UUID.fromString("01927d6d-4dbf-7efa-0ac5-526374859607");
            Product product = new Product();
            UUID orderId = UUID.randomUUID();
            Instant now = Instant.now();

            sale.setId(id);
            sale.setProduct(product);
            sale.setOrderId(orderId);
            sale.setQuantity(5);
            sale.setProfit(new BigDecimal("250.00"));
            sale.setCreatedAt(now);

            assertEquals(id, sale.getId());
            assertEquals(product, sale.getProduct());
            assertEquals(orderId, sale.getOrderId());
            assertEquals(5, sale.getQuantity());
            assertEquals(new BigDecimal("250.00"), sale.getProfit());
            assertEquals(now, sale.getCreatedAt());
        }
    }

    @Nested
    @DisplayName("Banner Model Tests")
    class BannerModelTest {

        @Test
        @DisplayName("Default active is true")
        void defaults_newBanner_activeIsTrue() {
            Banner banner = new Banner();
            assertTrue(banner.isActive());
        }

        @Test
        @DisplayName("Setters update all fields correctly")
        void setters_allFields_updateCorrectly() {
            Banner banner = new Banner();
            banner.setTitle("Summer Sale");
            banner.setSubtitle("Up to 50% off");
            banner.setImageUrl("https://cdn.example.com/banner.jpg");
            banner.setLinkUrl("/sale");
            banner.setDisplayOrder(1);
            banner.setActive(false);

            assertEquals("Summer Sale", banner.getTitle());
            assertEquals("Up to 50% off", banner.getSubtitle());
            assertEquals("https://cdn.example.com/banner.jpg", banner.getImageUrl());
            assertEquals("/sale", banner.getLinkUrl());
            assertEquals(1, banner.getDisplayOrder());
            assertFalse(banner.isActive());
        }
    }

    @Nested
    @DisplayName("Blogs Model Tests")
    class BlogsModelTest {

        @Test
        @DisplayName("Default status is DRAFT")
        void defaults_newBlog_statusIsDraft() {
            Blogs blog = new Blogs();
            assertEquals(BlogPostStatus.DRAFT, blog.getStatus());
        }

        @Test
        @DisplayName("Setters update all fields correctly")
        void setters_allFields_updateCorrectly() {
            Blogs blog = new Blogs();
            Instant publishTime = Instant.parse("2025-06-15T12:00:00Z");

            blog.setTitle("Spring Boot Tips");
            blog.setContent("Here are some tips...");
            blog.setAuthor("Jane Dev");
            blog.setImageUrl("https://cdn.example.com/blog.jpg");
            blog.setStatus(BlogPostStatus.PUBLISHED);
            blog.setPublishedAt(publishTime);

            assertEquals("Spring Boot Tips", blog.getTitle());
            assertEquals("Here are some tips...", blog.getContent());
            assertEquals("Jane Dev", blog.getAuthor());
            assertEquals("https://cdn.example.com/blog.jpg", blog.getImageUrl());
            assertEquals(BlogPostStatus.PUBLISHED, blog.getStatus());
            assertEquals(publishTime, blog.getPublishedAt());
        }
    }

    @Nested
    @DisplayName("Faq Model Tests")
    class FaqModelTest {

        @Test
        @DisplayName("Setters and getters work for all fields")
        void gettersSetters_allFields_workCorrectly() {
            Faq faq = new Faq();
            faq.setQuestion("How to return?");
            faq.setAnswer("Visit our returns page.");
            faq.setDisplayOrder(3);

            assertEquals("How to return?", faq.getQuestion());
            assertEquals("Visit our returns page.", faq.getAnswer());
            assertEquals(3, faq.getDisplayOrder());
        }
    }

    @Nested
    @DisplayName("Category Model Tests")
    class CategoryModelTest {

        @Test
        @DisplayName("Setters and getters work for all fields")
        void gettersSetters_allFields_workCorrectly() {
            Category category = new Category();
            UUID id = UUID.fromString("01927d6d-5ecf-7ffa-9ac6-637485960718");
            Instant now = Instant.now();

            category.setId(id);
            category.setName("Electronics");
            category.setCreatedAt(now);

            assertEquals(id, category.getId());
            assertEquals("Electronics", category.getName());
            assertEquals(now, category.getCreatedAt());
        }
    }

    @Nested
    @DisplayName("UserRefreshToken Model Tests")
    class UserRefreshTokenModelTest {

        @Test
        @DisplayName("Default status is ACTIVE")
        void defaults_newToken_statusIsActive() {
            UserRefreshToken token = new UserRefreshToken();
            assertEquals(UserRefreshTokenStatus.ACTIVE, token.getStatus());
        }

        @Test
        @DisplayName("isInvalid returns true for REVOKED status")
        void isInvalid_revoked_returnsTrue() {
            UserRefreshToken token = new UserRefreshToken();
            token.setStatus(UserRefreshTokenStatus.REVOKED);
            assertTrue(token.isInvalid());
        }

        @Test
        @DisplayName("isInvalid returns false for ACTIVE status")
        void isInvalid_active_returnsFalse() {
            UserRefreshToken token = new UserRefreshToken();
            token.setStatus(UserRefreshTokenStatus.ACTIVE);
            assertFalse(token.isInvalid());
        }

        @Test
        @DisplayName("Setters update user and jti")
        void setters_allFields_updateCorrectly() {
            UserRefreshToken token = new UserRefreshToken();
            User user = new User();
            user.setName("Bob");
            token.setUser(user);
            token.setJti("unique-jti-token-abc123");

            assertEquals(user, token.getUser());
            assertEquals("unique-jti-token-abc123", token.getJti());
        }
    }

    @Nested
    @DisplayName("ProductImage Model Tests")
    class ProductImageModelTest {

        @Test
        @DisplayName("Setters and getters work for all fields")
        void gettersSetters_allFields_workCorrectly() {
            ProductImage image = new ProductImage();
            UUID id = UUID.fromString("01927d6d-6fdf-70fa-8ac7-748596071829");
            Product product = new Product();

            image.setId(id);
            image.setImage("https://cdn.example.com/product.png");
            image.setProduct(product);

            assertEquals(id, image.getId());
            assertEquals("https://cdn.example.com/product.png", image.getImage());
            assertEquals(product, image.getProduct());
        }
    }

    @Nested
    @DisplayName("Role Model Tests")
    class RoleModelTest {

        @Test
        @DisplayName("Setters and getters work for all fields")
        void gettersSetters_allFields_workCorrectly() {
            Role role = new Role();
            role.setName("ADMIN");
            role.setPermissions(Set.of(Permission.ADMIN_ACCESS, Permission.USER));

            assertEquals("ADMIN", role.getName());
            assertEquals(2, role.getPermissions().size());
            assertTrue(role.getPermissions().contains(Permission.ADMIN_ACCESS));
        }

        @Test
        @DisplayName("Equals and hashCode based only on name field")
        void equals_sameRoleName_areEqual() {
            Role role1 = new Role();
            role1.setName("ADMIN");
            role1.setPermissions(Set.of(Permission.ADMIN_ACCESS));

            Role role2 = new Role();
            role2.setName("ADMIN");
            role2.setPermissions(Set.of(Permission.USER));

            assertEquals(role1, role2);
            assertEquals(role1.hashCode(), role2.hashCode());
        }

        @Test
        @DisplayName("Different role names are not equal")
        void equals_differentRoleNames_areNotEqual() {
            Role role1 = new Role();
            role1.setName("ADMIN");

            Role role2 = new Role();
            role2.setName("USER");

            assertNotEquals(role1, role2);
        }
    }
}
