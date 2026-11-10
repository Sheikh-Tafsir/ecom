package com.example.ecom.order.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PaymentMethod {

    CASH_ON_DELIVERY("Cash On Delivery"),
    BKASH("Bkash");

    @JsonValue
    private final String value;
}
