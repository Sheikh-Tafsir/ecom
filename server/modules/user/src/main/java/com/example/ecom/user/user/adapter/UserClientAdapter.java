package com.example.ecom.user.user.adapter;

import com.example.ecom.common.client.user.UserBasicDto;
import com.example.ecom.common.client.user.UserClient;
import com.example.ecom.user.user.model.User;
import com.example.ecom.user.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Monolith adapter: implements {@link UserClient} by delegating to {@link UserService}.
 * On microservice extraction, this class is removed and replaced by an HTTP proxy.
 */
@Primary
@Service
@RequiredArgsConstructor
public class UserClientAdapter implements UserClient {

    private final UserService userService;

    @Override
    public UserBasicDto findUserById(UUID id) {
        User user = userService.findByIdHelper(id);
        return new UserBasicDto(user.getId(), user.getName(), user.getImage());
    }
}
