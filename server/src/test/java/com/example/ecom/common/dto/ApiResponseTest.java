package com.example.ecom.common.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ApiResponseTest {

    @Test
    @DisplayName("Success constructor sets data and message, errors remains null")
    void constructor_successResponse_setsDataAndMessage() {
        String data = "test-payload";
        String message = "Operation successful";

        ApiResponse<String> response = new ApiResponse<>(data, message);

        assertEquals(data, response.getData());
        assertEquals(message, response.getMessage());
        assertNull(response.getErrors());
    }

    @Test
    @DisplayName("Success constructor with null data sets data to null")
    void constructor_successResponseNullData_setsNullData() {
        ApiResponse<Object> response = new ApiResponse<>(null, "No content");

        assertNull(response.getData());
        assertEquals("No content", response.getMessage());
        assertNull(response.getErrors());
    }

    @Test
    @DisplayName("Error constructor sets errors map, data and message remain null")
    void constructor_errorResponse_setsErrors() {
        Map<String, List<String>> errors = Map.of(
                "email", List.of("Email is required"),
                "password", List.of("Password must be at least 8 characters", "Password is required")
        );

        ApiResponse<Object> response = new ApiResponse<>(errors);

        assertNull(response.getData());
        assertNull(response.getMessage());
        assertNotNull(response.getErrors());
        assertEquals(2, response.getErrors().size());
        assertEquals(List.of("Email is required"), response.getErrors().get("email"));
        assertEquals(2, response.getErrors().get("password").size());
    }

    @Test
    @DisplayName("Error constructor with empty map sets empty errors")
    void constructor_errorResponseEmptyMap_setsEmptyErrors() {
        ApiResponse<Object> response = new ApiResponse<>(Map.of());

        assertNull(response.getData());
        assertNull(response.getMessage());
        assertNotNull(response.getErrors());
        assertTrue(response.getErrors().isEmpty());
    }

    @Test
    @DisplayName("Success constructor with complex data type works correctly")
    void constructor_successResponseComplexType_setsData() {
        List<Integer> data = List.of(1, 2, 3);
        ApiResponse<List<Integer>> response = new ApiResponse<>(data, "Fetched list");

        assertEquals(data, response.getData());
        assertEquals(3, response.getData().size());
    }

    @Test
    @DisplayName("Setters allow modifying all fields after construction")
    void setters_modifyFields_updatesCorrectly() {
        ApiResponse<String> response = new ApiResponse<>("original", "Original message");

        response.setData("updated");
        response.setMessage("Updated message");
        response.setErrors(Map.of("field", List.of("error")));

        assertEquals("updated", response.getData());
        assertEquals("Updated message", response.getMessage());
        assertNotNull(response.getErrors());
        assertEquals(List.of("error"), response.getErrors().get("field"));
    }

    @Test
    @DisplayName("Two ApiResponse objects with same fields are equal via Lombok @Data")
    void equals_sameFields_returnsTrue() {
        ApiResponse<String> r1 = new ApiResponse<>("data", "msg");
        ApiResponse<String> r2 = new ApiResponse<>("data", "msg");

        assertEquals(r1, r2);
        assertEquals(r1.hashCode(), r2.hashCode());
    }

    @Test
    @DisplayName("Two ApiResponse objects with different fields are not equal")
    void equals_differentFields_returnsFalse() {
        ApiResponse<String> r1 = new ApiResponse<>("data1", "msg");
        ApiResponse<String> r2 = new ApiResponse<>("data2", "msg");

        assertNotEquals(r1, r2);
    }

    @Test
    @DisplayName("toString includes all fields")
    void toString_includesAllFields() {
        ApiResponse<String> response = new ApiResponse<>("test", "message");
        String str = response.toString();

        assertTrue(str.contains("test"));
        assertTrue(str.contains("message"));
    }
}
