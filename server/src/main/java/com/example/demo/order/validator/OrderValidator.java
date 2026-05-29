package com.example.demo.order.validator;

import com.example.demo.order.dto.CreateOrderRequest;
import com.example.demo.order.dto.CreateOrderItemRequest;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;

import java.util.HashSet;
import java.util.Set;

import static com.example.demo.common.utils.Utils.isEmpty;

@Component
public class OrderValidator {

    public void validate(CreateOrderRequest request, Errors errors) {
        if (isEmpty(request.items())) {
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
}
