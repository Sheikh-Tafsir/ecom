package com.example.ecom.common.serializer;

import com.example.ecom.order.enums.OrderStatus;
import com.example.ecom.order.converter.OrderStatusConverter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OrderStatusConverterTest {

    private OrderStatusConverter converter;

    @BeforeEach
    void setUp() {
        converter = new OrderStatusConverter();
    }

    @Test
    @DisplayName("Converts 'Pending' string to OrderStatus.PENDING")
    void convert_validValue_returnsOrderStatus() {
        assertEquals(OrderStatus.PENDING, converter.convert("Pending"));
    }

    @Test
    @DisplayName("Converts enum name 'SHIPPED' to OrderStatus.SHIPPED")
    void convert_enumName_returnsOrderStatus() {
        assertEquals(OrderStatus.SHIPPED, converter.convert("SHIPPED"));
    }

    @Test
    @DisplayName("Converts case-insensitive value 'delivered' to OrderStatus.DELIVERED")
    void convert_caseInsensitive_returnsOrderStatus() {
        assertEquals(OrderStatus.DELIVERED, converter.convert("delivered"));
    }

    @Test
    @DisplayName("Throws IllegalArgumentException for invalid order status string")
    void convert_invalidValue_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> converter.convert("NONEXISTENT"));
    }
}
