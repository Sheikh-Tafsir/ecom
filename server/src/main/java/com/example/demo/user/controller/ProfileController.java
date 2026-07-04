package com.example.demo.user.controller;

import com.example.demo.auth.dto.TokenDto;
import com.example.demo.auth.validator.AuthValidator;
import com.example.demo.user.dto.ChangePasswordRequest;
import com.example.demo.user.dto.UpdateProfileRequest;
import com.example.demo.auth.service.AuthService;
import com.example.demo.user.dto.UserResponse;
import com.example.demo.user.service.UserService;
import com.example.demo.user.validator.ProfileUpdateRequestValidator;
import com.example.demo.common.dto.ApiResponse;
import com.example.demo.common.dto.CustomUserDetails;
import com.example.demo.common.model.User;
import com.example.demo.common.service.MessageService;
import com.example.demo.common.utils.ResponseUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

import static com.example.demo.common.utils.Utils.checkErrors;

@Slf4j
@RestController
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final AuthValidator authValidator;

    private final ProfileUpdateRequestValidator profileUpdateRequestValidator;

    private final AuthService authService;

    private final UserService userService;

    private final MessageService messageService;

    @GetMapping
    public ResponseEntity<ApiResponse<UserResponse>> getProfile(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseUtils.ok(new UserResponse(userDetails.user(), null), messageService.get("successfully.found", "Profile"));
    }

    @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(@Valid @ModelAttribute UpdateProfileRequest updateProfileRequest,
                                                                   BindingResult bindingResult,
                                                                   @AuthenticationPrincipal CustomUserDetails userDetails,
                                                                   HttpServletResponse response) throws IOException {

        profileUpdateRequestValidator.validate(updateProfileRequest, bindingResult);
        checkErrors(bindingResult);

        User user = userService.update(updateProfileRequest, userDetails);

        TokenDto tokenDto = authService.getTokens(user);
        authService.addRefreshCookie(response, tokenDto);

        return ResponseUtils.ok(new UserResponse(user, tokenDto.getAccessToken()), messageService.get("successfully.updated", "Profile"));
    }

    @DeleteMapping
    public ResponseEntity<?> delete(@AuthenticationPrincipal CustomUserDetails userDetails,
                                    HttpServletRequest request,
                                    HttpServletResponse response) {

        userService.delete(userDetails.user().getId());
        authService.logout(request, response);

        return ResponseUtils.ok(messageService.get("successfully.deleted", "Profile"));
    }

    @PutMapping("/password")
    public ResponseEntity<ApiResponse<Void>> updatePassword(@Valid @RequestBody ChangePasswordRequest changePasswordRequest,
                                                            BindingResult bindingResult,
                                                            @AuthenticationPrincipal CustomUserDetails userDetails) {

        authValidator.validatePasswords(changePasswordRequest.newPassword(), changePasswordRequest.confirmNewPassword(), bindingResult);
        checkErrors(bindingResult);

        userService.updatePassword(changePasswordRequest, userDetails);
        return ResponseUtils.ok("Password change successful");
    }
}
