package com.example.demo.common.serializer;

import com.example.demo.common.enums.UserStatus;
import org.jspecify.annotations.NonNull;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class UserStatusConverter implements Converter<String, UserStatus> {

    @Override
    public UserStatus convert(@NonNull String source) {
        return UserStatus.fromValue(source);
    }
}
