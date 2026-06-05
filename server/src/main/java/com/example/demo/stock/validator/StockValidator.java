package com.example.demo.stock.validator;

import com.example.demo.stock.dto.CreateStockItemRequest;
import com.example.demo.stock.dto.CreateStockRequest;
import com.example.demo.stock.dto.UpdateStockItemRequest;
import com.example.demo.stock.dto.UpdateStockRequest;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Component
public class StockValidator {

    public void validateCreate(CreateStockRequest request, Errors errors) {
        if (request.items() == null) {
            return;
        }

        Set<Long> productIds = new HashSet<>();
        for (CreateStockItemRequest item : request.items()) {
            if (item == null) {
                continue;
            }

            validateCreateItem(item, errors);

            if (item.productId() != null && !productIds.add(item.productId())) {
                errors.rejectValue("items", "error.stock.product.duplicate",
                        "Duplicate product is not allowed");
                return;
            }
        }
    }

    public void validateUpdate(UpdateStockRequest request, Errors errors) {
        if (request.items() == null) {
            return;
        }

        Set<Long> itemIds = new HashSet<>();
        for (UpdateStockItemRequest item : request.items()) {
            if (item == null) {
                continue;
            }

            validateUpdateItem(item, errors);

            if (item.id() == null) {
                errors.rejectValue("items", "error.field.is.required",
                        "Stock item id is required");
                return;
            }

            if (item.id() != null && !itemIds.add(item.id())) {
                errors.rejectValue("items", "error.stock.item.duplicate",
                        "Duplicate stock item is not allowed");
                return;
            }
        }
    }

    public void validateCreateItem(CreateStockItemRequest request, Errors errors) {
        validateCost(request.purchasedPrice(), "purchasedPrice", errors);
    }

    public void validateUpdateItem(UpdateStockItemRequest request, Errors errors) {
        validateCost(request.purchasePrice(), "purchasedPrice", errors);
    }

    private void validateCost(BigDecimal cost, String field, Errors errors) {
        if (cost == null) {
            return;
        }

        if (cost.compareTo(BigDecimal.ZERO) < 0) {
            errors.rejectValue(field, "error.field.min", "Cost cannot be negative");
        }
    }
}
