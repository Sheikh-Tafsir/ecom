package com.example.ecom.order.dto;

import com.example.ecom.common.enums.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateOrderRequest(

        @NotEmpty
        List<@Valid CreateOrderItemRequest> items,

        @Size(min = 2, max = 31)
        String name,

        @NotBlank
        @Size(min = 11, max = 11)
        String phone,

        @NotBlank
        @Size(min = 5, max = 1023)
        String address,

        @NotNull
        PaymentMethod paymentMethod
) {
}
