package com.example.demo.user.controller;

import com.example.demo.auth.validator.AuthValidator;
import com.example.demo.user.dto.ChangePasswordRequest;
import com.example.demo.user.dto.UpdateProfileRequest;
import com.example.demo.auth.service.AuthService;
import com.example.demo.user.service.UserService;
import com.example.demo.user.validator.ProfileUpdateRequestValidator;
import com.example.demo.common.dto.ApiResponse;
import com.example.demo.common.dto.CustomUserDetails;
import com.example.demo.common.exception.JsrValidationException;
import com.example.demo.common.model.User;
import com.example.demo.common.service.JwtService;
import com.example.demo.common.service.MessageService;
import com.example.demo.common.utils.ResponseUtils;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final AuthValidator authValidator;

    private final ProfileUpdateRequestValidator profileUpdateRequestValidator;

    private final AuthService authService;

    private final UserService userService;

    private final JwtService jwtService;

    private final MessageService messageService;

    @GetMapping
    public ResponseEntity<ApiResponse<User>> getProfile(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseUtils.ok(userDetails.user(), messageService.get("successfully.found", "Profile"));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<String>> updateProfile(@Valid @ModelAttribute UpdateProfileRequest updateProfileRequest,
                                                             BindingResult bindingResult,
                                                             @AuthenticationPrincipal CustomUserDetails userDetails) throws IOException {

        log.info("Multipart file: {}", updateProfileRequest.image());

        profileUpdateRequestValidator.validate(updateProfileRequest, bindingResult);
        if (bindingResult.hasErrors()) {
            throw new JsrValidationException(bindingResult);
        }

        userService.updateProfile(updateProfileRequest, userDetails.getEmail());

        String token = jwtService.generateAccessToken(userDetails.user());

        return ResponseUtils.ok(token, messageService.get("successfully.updated", "Profile"));
    }

    @DeleteMapping
    public ResponseEntity<?> delete(@AuthenticationPrincipal CustomUserDetails userDetails, HttpServletResponse response) {
        userService.delete(userDetails.user().getId());
        authService.logout(response);

        return ResponseUtils.ok(messageService.get("successfully.deleted", "Profile"));
    }

    @PutMapping("/password")
    public ResponseEntity<ApiResponse<Void>> updatePassword(@Valid @RequestBody ChangePasswordRequest changePasswordRequest,
                                                            BindingResult bindingResult,
                                                            @AuthenticationPrincipal CustomUserDetails userDetails) {

        authValidator.validatePasswords(changePasswordRequest.newPassword(), changePasswordRequest.confirmNewPassword(), bindingResult);

        if (bindingResult.hasErrors()) {
            throw new JsrValidationException(bindingResult);
        }

        userService.updatePassword(changePasswordRequest, userDetails.getEmail());
        return ResponseUtils.ok("Password change successful");
    }
}
