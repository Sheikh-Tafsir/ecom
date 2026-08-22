package com.example.ecom.user.user.controller;

import com.example.ecom.auth.dto.TokenDto;
import com.example.ecom.auth.validator.AuthValidator;
import com.example.ecom.user.user.dto.ChangePasswordRequest;
import com.example.ecom.user.user.dto.UpdateProfileRequest;
import com.example.ecom.auth.service.AuthService;
import com.example.ecom.user.user.dto.ProfileResponse;
import com.example.ecom.user.user.service.ProfileService;
import com.example.ecom.user.user.validator.ProfileUpdateRequestValidator;
import com.example.ecom.common.dto.ApiResponse;
import com.example.ecom.common.dto.CustomUserDetails;
import com.example.ecom.common.model.User;
import com.example.ecom.common.service.MessageService;
import com.example.ecom.common.utils.ResponseUtils;
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

import static com.example.ecom.common.utils.Utils.checkErrors;

@Slf4j
@RestController
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final AuthValidator authValidator;

    private final ProfileUpdateRequestValidator profileUpdateRequestValidator;

    private final AuthService authService;

    private final ProfileService profileService;

    private final MessageService messageService;

    @GetMapping
    public ResponseEntity<ApiResponse<ProfileResponse>> getProfile(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseUtils.ok(profileService.getProfile(userDetails),
                messageService.get("successfully.found", "Profile"));
    }

    @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ProfileResponse>> updateProfile(@Valid @ModelAttribute UpdateProfileRequest updateProfileRequest,
                                                                      BindingResult bindingResult,
                                                                      @AuthenticationPrincipal CustomUserDetails userDetails,
                                                                      HttpServletResponse response) throws IOException {

        profileUpdateRequestValidator.validate(updateProfileRequest, bindingResult);
        checkErrors(bindingResult);

        User user = profileService.update(updateProfileRequest, userDetails);

        TokenDto tokenDto = authService.getAuthTokens(user);
        authService.addRefreshCookie(response, tokenDto);

        return ResponseUtils.ok(new ProfileResponse(user, tokenDto.getAccessToken()), messageService.get("successfully.updated", "Profile"));
    }

    @DeleteMapping
    public ResponseEntity<?> delete(@AuthenticationPrincipal CustomUserDetails userDetails,
                                    HttpServletRequest request,
                                    HttpServletResponse response) {

        profileService.delete(userDetails);
        authService.logout(request, response);

        return ResponseUtils.ok(messageService.get("successfully.deleted", "Profile"));
    }

    @PutMapping("/password")
    public ResponseEntity<ApiResponse<Void>> updatePassword(@Valid @RequestBody ChangePasswordRequest changePasswordRequest,
                                                            BindingResult bindingResult,
                                                            @AuthenticationPrincipal CustomUserDetails userDetails) {

        authValidator.validatePasswords(changePasswordRequest.newPassword(), changePasswordRequest.confirmNewPassword(), bindingResult);
        checkErrors(bindingResult);

        profileService.updatePassword(changePasswordRequest, userDetails);
        return ResponseUtils.ok("Password change successful");
    }
}
