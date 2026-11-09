package com.example.ecom.common.client.user;

import java.util.UUID;

/**
 * Basic user data exposed across domain boundaries.
 */
public record UserBasicDto(UUID id, String name, String image) {
}
