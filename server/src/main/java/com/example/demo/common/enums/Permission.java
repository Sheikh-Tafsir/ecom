package com.example.demo.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Permission {

    SUPER_ADMIN_ACCESS("super_admin:access"),
    ADMIN_ACCESS("admin:access");

    private final String value;
}
