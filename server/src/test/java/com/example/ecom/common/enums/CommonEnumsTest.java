package com.example.ecom.common.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class CommonEnumsTest {

    // ===== AppModule =====

    @ParameterizedTest
    @EnumSource(AppModule.class)
    @DisplayName("AppModule.fromValue round-trips for every constant via getValue()")
    void appModule_fromValue_roundTrips(AppModule module) {
        assertEquals(module, AppModule.fromValue(module.getValue()));
    }

    @ParameterizedTest
    @EnumSource(AppModule.class)
    @DisplayName("AppModule.fromValue works with enum name()")
    void appModule_fromName_works(AppModule module) {
        assertEquals(module, AppModule.fromValue(module.name()));
    }

    @Test
    @DisplayName("AppModule.fromValue returns null for null input")
    void appModule_fromValue_null_returnsNull() {
        assertNull(AppModule.fromValue(null));
    }

    @Test
    @DisplayName("AppModule.fromValue throws for invalid value")
    void appModule_fromValue_invalid_throws() {
        assertThrows(IllegalArgumentException.class, () -> AppModule.fromValue("NONEXISTENT"));
    }

    @Test
    @DisplayName("AppModule.valueOf works for all constants")
    void appModule_valueOf_allConstants() {
        assertNotNull(AppModule.valueOf("USER"));
        assertNotNull(AppModule.valueOf("ORDER"));
        assertNotNull(AppModule.valueOf("SALE"));
    }

    // ===== BlogPostStatus =====

    @ParameterizedTest
    @EnumSource(BlogPostStatus.class)
    @DisplayName("BlogPostStatus.fromValue round-trips for every constant")
    void blogPostStatus_fromValue_roundTrips(BlogPostStatus status) {
        assertEquals(status, BlogPostStatus.fromValue(status.getValue()));
    }

    @Test
    @DisplayName("BlogPostStatus.fromValue returns null for invalid input")
    void blogPostStatus_fromValue_invalid_returnsNull() {
        assertNull(BlogPostStatus.fromValue("NONEXISTENT"));
    }

    @Test
    @DisplayName("BlogPostStatus has DRAFT and PUBLISHED")
    void blogPostStatus_allConstants() {
        assertEquals(2, BlogPostStatus.values().length);
        assertNotNull(BlogPostStatus.valueOf("DRAFT"));
        assertNotNull(BlogPostStatus.valueOf("PUBLISHED"));
    }

    // ===== Gender =====

    @ParameterizedTest
    @EnumSource(Gender.class)
    @DisplayName("Gender.fromValue round-trips for every constant")
    void gender_fromValue_roundTrips(Gender gender) {
        assertEquals(gender, Gender.fromValue(gender.getValue()));
    }

    @Test
    @DisplayName("Gender.fromValue returns null for null input")
    void gender_fromValue_null_returnsNull() {
        assertNull(Gender.fromValue(null));
    }

    @Test
    @DisplayName("Gender.fromValue throws for invalid value")
    void gender_fromValue_invalid_throws() {
        assertThrows(IllegalArgumentException.class, () -> Gender.fromValue("OTHER"));
    }

    // ===== NotificationType =====

    @ParameterizedTest
    @EnumSource(NotificationType.class)
    @DisplayName("NotificationType.fromValue round-trips for every constant")
    void notificationType_fromValue_roundTrips(NotificationType type) {
        assertEquals(type, NotificationType.fromValue(type.getValue()));
    }

    @Test
    @DisplayName("NotificationType.fromValue returns null for null input")
    void notificationType_fromValue_null_returnsNull() {
        assertNull(NotificationType.fromValue(null));
    }

    @Test
    @DisplayName("NotificationType.fromValue throws for invalid value")
    void notificationType_fromValue_invalid_throws() {
        assertThrows(IllegalArgumentException.class, () -> NotificationType.fromValue("CRITICAL"));
    }

    @Test
    @DisplayName("NotificationType has INFO, SUCCESS, ERROR, WARNING")
    void notificationType_allConstants() {
        assertEquals(4, NotificationType.values().length);
    }

    // ===== OrderStatus =====

    @ParameterizedTest
    @EnumSource(OrderStatus.class)
    @DisplayName("OrderStatus.fromValue round-trips for every constant")
    void orderStatus_fromValue_roundTrips(OrderStatus status) {
        assertEquals(status, OrderStatus.fromValue(status.getValue()));
    }

    @ParameterizedTest
    @EnumSource(OrderStatus.class)
    @DisplayName("OrderStatus.fromValue works with enum name")
    void orderStatus_fromName_works(OrderStatus status) {
        assertEquals(status, OrderStatus.fromValue(status.name()));
    }

    @Test
    @DisplayName("OrderStatus.fromValue returns null for null input")
    void orderStatus_fromValue_null_returnsNull() {
        assertNull(OrderStatus.fromValue(null));
    }

    @Test
    @DisplayName("OrderStatus.fromValue throws for invalid value")
    void orderStatus_fromValue_invalid_throws() {
        assertThrows(IllegalArgumentException.class, () -> OrderStatus.fromValue("UNKNOWN_STATUS"));
    }

    @Test
    @DisplayName("OrderStatus.isCancellationOrRejection returns true for CANCELLED and REJECTED")
    void orderStatus_isCancellationOrRejection_cancelled() {
        assertTrue(OrderStatus.CANCELLED.isCancellationOrRejection());
        assertTrue(OrderStatus.REJECTED.isCancellationOrRejection());
    }

    @Test
    @DisplayName("OrderStatus.isCancellationOrRejection returns false for non-cancellation statuses")
    void orderStatus_isCancellationOrRejection_others() {
        assertFalse(OrderStatus.PENDING.isCancellationOrRejection());
        assertFalse(OrderStatus.SHIPPED.isCancellationOrRejection());
        assertFalse(OrderStatus.DELIVERED.isCancellationOrRejection());
        assertFalse(OrderStatus.COMPLETED.isCancellationOrRejection());
    }

    @Test
    @DisplayName("OrderStatus.canTransitionTo returns false for null target")
    void orderStatus_canTransitionTo_null_returnsFalse() {
        assertFalse(OrderStatus.PENDING.canTransitionTo(null));
    }

    @Test
    @DisplayName("PENDING can transition to ACCEPTED, CANCELLED, REJECTED only")
    void orderStatus_pending_validTransitions() {
        assertTrue(OrderStatus.PENDING.canTransitionTo(OrderStatus.ACCEPTED));
        assertTrue(OrderStatus.PENDING.canTransitionTo(OrderStatus.CANCELLED));
        assertTrue(OrderStatus.PENDING.canTransitionTo(OrderStatus.REJECTED));
        assertFalse(OrderStatus.PENDING.canTransitionTo(OrderStatus.SHIPPED));
        assertFalse(OrderStatus.PENDING.canTransitionTo(OrderStatus.DELIVERED));
    }

    @Test
    @DisplayName("ACCEPTED can transition to SHIPPED, REJECTED, CANCELLED, REFUNDED only")
    void orderStatus_accepted_validTransitions() {
        assertTrue(OrderStatus.ACCEPTED.canTransitionTo(OrderStatus.SHIPPED));
        assertTrue(OrderStatus.ACCEPTED.canTransitionTo(OrderStatus.REJECTED));
        assertTrue(OrderStatus.ACCEPTED.canTransitionTo(OrderStatus.CANCELLED));
        assertTrue(OrderStatus.ACCEPTED.canTransitionTo(OrderStatus.REFUNDED));
        assertFalse(OrderStatus.ACCEPTED.canTransitionTo(OrderStatus.DELIVERED));
        assertFalse(OrderStatus.ACCEPTED.canTransitionTo(OrderStatus.PENDING));
    }

    @Test
    @DisplayName("SHIPPED can transition to DELIVERED, LOST, RETURNED only")
    void orderStatus_shipped_validTransitions() {
        assertTrue(OrderStatus.SHIPPED.canTransitionTo(OrderStatus.DELIVERED));
        assertTrue(OrderStatus.SHIPPED.canTransitionTo(OrderStatus.LOST));
        assertTrue(OrderStatus.SHIPPED.canTransitionTo(OrderStatus.RETURNED));
        assertFalse(OrderStatus.SHIPPED.canTransitionTo(OrderStatus.ACCEPTED));
        assertFalse(OrderStatus.SHIPPED.canTransitionTo(OrderStatus.COMPLETED));
    }

    @Test
    @DisplayName("DELIVERED can transition to COMPLETED only")
    void orderStatus_delivered_validTransitions() {
        assertTrue(OrderStatus.DELIVERED.canTransitionTo(OrderStatus.COMPLETED));
        assertFalse(OrderStatus.DELIVERED.canTransitionTo(OrderStatus.REFUNDED));
    }

    @Test
    @DisplayName("LOST and RETURNED can transition to REFUNDED only")
    void orderStatus_lostReturned_validTransitions() {
        assertTrue(OrderStatus.LOST.canTransitionTo(OrderStatus.REFUNDED));
        assertTrue(OrderStatus.RETURNED.canTransitionTo(OrderStatus.REFUNDED));
        assertFalse(OrderStatus.LOST.canTransitionTo(OrderStatus.COMPLETED));
        assertFalse(OrderStatus.RETURNED.canTransitionTo(OrderStatus.SHIPPED));
    }

    @Test
    @DisplayName("Terminal statuses cannot transition to any status")
    void orderStatus_terminalStatuses_cannotTransition() {
        assertFalse(OrderStatus.CANCELLED.canTransitionTo(OrderStatus.PENDING));
        assertFalse(OrderStatus.REJECTED.canTransitionTo(OrderStatus.PENDING));
        assertFalse(OrderStatus.COMPLETED.canTransitionTo(OrderStatus.PENDING));
        assertFalse(OrderStatus.REFUNDED.canTransitionTo(OrderStatus.PENDING));
    }

    @Test
    @DisplayName("OrderStatus has all 10 expected constants")
    void orderStatus_allConstants() {
        assertEquals(10, OrderStatus.values().length);
    }

    // ===== PaymentMethod =====

    @Test
    @DisplayName("PaymentMethod has CASH_ON_DELIVERY and BKASH")
    void paymentMethod_allConstants() {
        assertEquals(2, PaymentMethod.values().length);
        assertNotNull(PaymentMethod.valueOf("CASH_ON_DELIVERY"));
        assertNotNull(PaymentMethod.valueOf("BKASH"));
    }

    @Test
    @DisplayName("PaymentMethod getValue returns expected values")
    void paymentMethod_getValue() {
        assertEquals("Cash On Delivery", PaymentMethod.CASH_ON_DELIVERY.getValue());
        assertEquals("Bkash", PaymentMethod.BKASH.getValue());
    }

    // ===== PaymentStatus =====

    @Test
    @DisplayName("PaymentStatus has PENDING, COMPLETED, FAILED")
    void paymentStatus_allConstants() {
        assertEquals(3, PaymentStatus.values().length);
        assertEquals("Pending", PaymentStatus.PENDING.getValue());
        assertEquals("Completed", PaymentStatus.COMPLETED.getValue());
        assertEquals("Failed", PaymentStatus.FAILED.getValue());
    }

    // ===== Permission =====

    @ParameterizedTest
    @EnumSource(Permission.class)
    @DisplayName("Permission.fromValue round-trips for every constant")
    void permission_fromValue_roundTrips(Permission perm) {
        assertEquals(perm, Permission.fromValue(perm.getValue()));
    }

    @Test
    @DisplayName("Permission.fromValue returns null for null input")
    void permission_fromValue_null_returnsNull() {
        assertNull(Permission.fromValue(null));
    }

    @Test
    @DisplayName("Permission.fromValue throws for invalid value")
    void permission_fromValue_invalid_throws() {
        assertThrows(IllegalArgumentException.class, () -> Permission.fromValue("unknown:perm"));
    }

    // ===== ProductStatus =====

    @ParameterizedTest
    @EnumSource(ProductStatus.class)
    @DisplayName("ProductStatus.fromValue round-trips for every constant")
    void productStatus_fromValue_roundTrips(ProductStatus status) {
        assertEquals(status, ProductStatus.fromValue(status.getValue()));
    }

    @Test
    @DisplayName("ProductStatus.fromValue returns null for null input")
    void productStatus_fromValue_null_returnsNull() {
        assertNull(ProductStatus.fromValue(null));
    }

    @Test
    @DisplayName("ProductStatus.fromValue throws for invalid value")
    void productStatus_fromValue_invalid_throws() {
        assertThrows(IllegalArgumentException.class, () -> ProductStatus.fromValue("ARCHIVED"));
    }

    @Test
    @DisplayName("ProductStatus has COMING_SOON, AVAILABLE, STOCK_OUT, DISCONTINUED")
    void productStatus_allConstants() {
        assertEquals(4, ProductStatus.values().length);
    }

    // ===== RoleName =====

    @ParameterizedTest
    @EnumSource(RoleName.class)
    @DisplayName("RoleName.fromValue round-trips for every constant")
    void roleName_fromValue_roundTrips(RoleName role) {
        assertEquals(role, RoleName.fromValue(role.getValue()));
    }

    @Test
    @DisplayName("RoleName.fromValue returns null for null input")
    void roleName_fromValue_null_returnsNull() {
        assertNull(RoleName.fromValue(null));
    }

    @Test
    @DisplayName("RoleName.fromValue throws for invalid value")
    void roleName_fromValue_invalid_throws() {
        assertThrows(IllegalArgumentException.class, () -> RoleName.fromValue("MODERATOR"));
    }

    // ===== UserRefreshTokenStatus =====

    @ParameterizedTest
    @EnumSource(UserRefreshTokenStatus.class)
    @DisplayName("UserRefreshTokenStatus.fromValue round-trips for every constant")
    void userRefreshTokenStatus_fromValue_roundTrips(UserRefreshTokenStatus status) {
        assertEquals(status, UserRefreshTokenStatus.fromValue(status.getValue()));
    }

    @Test
    @DisplayName("UserRefreshTokenStatus.fromValue returns null for null input")
    void userRefreshTokenStatus_fromValue_null_returnsNull() {
        assertNull(UserRefreshTokenStatus.fromValue(null));
    }

    @Test
    @DisplayName("UserRefreshTokenStatus.fromValue throws for invalid value")
    void userRefreshTokenStatus_fromValue_invalid_throws() {
        assertThrows(IllegalArgumentException.class, () -> UserRefreshTokenStatus.fromValue("EXPIRED"));
    }

    // ===== UserStatus =====

    @ParameterizedTest
    @EnumSource(UserStatus.class)
    @DisplayName("UserStatus.fromValue round-trips for every constant")
    void userStatus_fromValue_roundTrips(UserStatus status) {
        assertEquals(status, UserStatus.fromValue(status.getValue()));
    }

    @ParameterizedTest
    @EnumSource(UserStatus.class)
    @DisplayName("UserStatus.fromValue works with enum name")
    void userStatus_fromName_works(UserStatus status) {
        assertEquals(status, UserStatus.fromValue(status.name()));
    }

    @Test
    @DisplayName("UserStatus.fromValue returns null for null input")
    void userStatus_fromValue_null_returnsNull() {
        assertNull(UserStatus.fromValue(null));
    }

    @Test
    @DisplayName("UserStatus.fromValue throws for invalid value")
    void userStatus_fromValue_invalid_throws() {
        assertThrows(IllegalArgumentException.class, () -> UserStatus.fromValue("UNKNOWN_STATUS"));
    }

    @Test
    @DisplayName("UserStatus has 6 expected constants")
    void userStatus_allConstants() {
        assertEquals(6, UserStatus.values().length);
    }
}
