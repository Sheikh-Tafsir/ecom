package com.example.demo.auth.controller;

import com.example.demo.auth.dto.*;
import com.example.demo.auth.service.AuthService;
import com.example.demo.auth.validator.AuthValidator;
import com.example.demo.common.dto.ApiResponse;
import com.example.demo.common.helper.CommonHelper;
import com.example.demo.common.utils.ResponseUtils;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static com.example.demo.auth.service.AuthService.REFRESH_TOKEN_COOKIE_NAME;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthValidator authValidator;

    private final CommonHelper commonHelper;

    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<Void>> signup(@Valid @RequestBody SignupRequest signupRequest, BindingResult bindingResult) {
        authValidator.validateSignup(signupRequest, bindingResult);
        commonHelper.checkErrors(bindingResult);

        authService.signup(signupRequest);

        return ResponseUtils.created("Signup OTP send to mail");
    }

    @PostMapping("/signup/resend")
    public ResponseEntity<ApiResponse<Void>> resendSignupOtp(@Valid @RequestBody OtpEmailRequest resetSignupOtpRequest) {
        authService.resendSignupOtp(resetSignupOtpRequest);

        return ResponseUtils.created("Signup OTP again send to mail");
    }

    @PostMapping("/signup/verify")
    public ResponseEntity<ApiResponse<String>> verifySignupOtp(@Valid @RequestBody VerifySignupOtpRequest otpVerificationRequest,
                                                               HttpServletResponse response) {

        TokenDto tokenDto = authService.verifySignupOtp(otpVerificationRequest);
        authService.addRefreshCookie(response, tokenDto);

        return ResponseUtils.created(tokenDto.getAccessToken(), "Signed up successful!");
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<String>> login(@Valid @RequestBody LoginRequest loginRequest,
                                                     HttpServletResponse response) {

        TokenDto tokenDto = authService.login(loginRequest);
        authService.addRefreshCookie(response, tokenDto);

        return ResponseUtils.ok(tokenDto.getAccessToken(), "Login successful!");
    }

    @PostMapping("/google-login")
    public ResponseEntity<ApiResponse<String>> loginWithGoogle(@RequestBody Map<String, String> request, HttpServletResponse response) {
        TokenDto tokenDto = authService.loginWithGoogle(request);
        authService.addRefreshCookie(response, tokenDto);

        return ResponseUtils.ok(tokenDto.getAccessToken(), "Login with Google successful");
    }

    @PostMapping("/access-token/refresh")
    public ResponseEntity<ApiResponse<String>> refreshAccessToken(@CookieValue(name = REFRESH_TOKEN_COOKIE_NAME, required = false) String refreshToken) {
        String accessToken = authService.refreshAccessToken(refreshToken);
        return ResponseUtils.ok(accessToken, "Access Token refreshed successfully");
    }

    @PostMapping("/password-reset")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody OtpEmailRequest request) {
        authService.resetPassword(request);
        return ResponseUtils.ok("Password Reset OTP send to mail");
    }

    @PostMapping("/password-reset/resend")
    public ResponseEntity<ApiResponse<Void>> resendResetPasswordOtp(@Valid @RequestBody OtpEmailRequest request) {
        authService.resetPassword(request);
        return ResponseUtils.ok("Password Reset OTP again send to mail");
    }

    @PostMapping("/password-reset/verify")
    public ResponseEntity<ApiResponse<Void>> verifyResetPasswordOtp(@Valid @RequestBody VerifyResetPasswordOtpRequest resetPasswordOtpVerificationRequest,
                                                                    BindingResult bindingResult) {

        authValidator.validatePasswords(resetPasswordOtpVerificationRequest.password(), resetPasswordOtpVerificationRequest.confirmPassword(), bindingResult);

        commonHelper.checkErrors(bindingResult);

        authService.verifyResetPasswordOtp(resetPasswordOtpVerificationRequest);
        return ResponseUtils.ok("Password Reset successful");
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        authService.logout(response);
        return ResponseUtils.ok("Logout Successful");
    }
}
