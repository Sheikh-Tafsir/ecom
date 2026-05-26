package com.example.demo.order.validator;

import com.example.demo.order.dto.CreateOrderRequest;
import com.example.demo.order.dto.CreateOrderItemRequest;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import java.util.HashSet;
import java.util.Set;

@Component
public class OrderValidator implements Validator {

    @Override
    public boolean supports(Class<?> clazz) {
        return clazz.equals(CreateOrderRequest.class) || clazz.equals(CreateOrderItemRequest.class);
    }

    @Override
    public void validate(@NonNull Object target, @NonNull Errors errors) {
        if (target instanceof CreateOrderItemRequest itemRequest) {
            validateItem(itemRequest, errors);
            return;
        }

        CreateOrderRequest request = (CreateOrderRequest) target;

        if (request.items() == null) {
            return;
        }

        Set<Long> productIds = new HashSet<>();
        for (CreateOrderItemRequest item : request.items()) {
            if (item == null) {
                continue;
            }

            if (item.productId() != null && !productIds.add(item.productId())) {
                errors.rejectValue("items", "error.order.product.duplicate", "Duplicate product is not allowed");
                return;
            }
        }
    }

    public void validateItem(CreateOrderItemRequest request, Errors errors) {
        if (request.quantity() <= 0) {
            errors.rejectValue("quantity", "error.field.is.required", "Quantity should be greater than 0");
        }
    }
}
