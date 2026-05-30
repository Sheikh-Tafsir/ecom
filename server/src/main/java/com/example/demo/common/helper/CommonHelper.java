package com.example.demo.common.helper;

import com.example.demo.common.dto.CustomUserDetails;
import com.example.demo.common.exception.JsrValidationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;

import static com.example.demo.common.utils.SecurityUtil.*;

@Component
public class CommonHelper {

    public void checkErrors(BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            throw new JsrValidationException(bindingResult);
        }
    }

    public void checkOwner(Long userId, CustomUserDetails userDetails) {
        if (!isOwner(userId, userDetails)) {
            throw new AccessDeniedException("User " + userDetails.getId() + "attempted to access resource owned by userId :" + userId);
        }
    }

    public void checkAdmin(CustomUserDetails userDetails) {
        if (!isAdmin(userDetails)) {
            throw new AccessDeniedException("User " + userDetails.getId() + " trying to access admin data");
        }
    }

    public void checkOwnerOrAdmin(Long userId, CustomUserDetails userDetails) {
        if (!isOwnerOrAdmin(userId, userDetails)) {
            throw new AccessDeniedException("User " + userDetails.getId() + "attempted to access resource owned by userId :" + userId);
        }
    }
}
