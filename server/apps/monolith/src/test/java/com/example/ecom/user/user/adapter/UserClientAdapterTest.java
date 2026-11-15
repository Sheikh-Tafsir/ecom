package com.example.ecom.user.user.adapter;

import com.example.ecom.common.client.user.UserBasicDto;
import com.example.ecom.user.user.model.User;
import com.example.ecom.user.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserClientAdapterTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserClientAdapter userClientAdapter;

    @Test
    @DisplayName("findUserById should delegate to userService.findByIdHelper and return UserBasicDto")
    void findUserById_delegatesToUserService() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setName("Jane Doe");
        user.setImage("https://example.com/avatar.png");

        when(userService.findByIdHelper(userId)).thenReturn(user);

        UserBasicDto result = userClientAdapter.findUserById(userId);

        assertNotNull(result);
        assertEquals(userId, result.id());
        assertEquals("Jane Doe", result.name());
        assertEquals("https://example.com/avatar.png", result.image());
        verify(userService).findByIdHelper(userId);
    }
}
