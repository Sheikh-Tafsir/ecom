package com.example.ecom.common.serializer;

import com.example.ecom.common.enums.OrderStatus;
import org.jspecify.annotations.NonNull;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class OrderStatusConverter implements Converter<String, OrderStatus> {

    @Override
    public OrderStatus convert(@NonNull String source) {
        return OrderStatus.fromValue(source);
    }
}
