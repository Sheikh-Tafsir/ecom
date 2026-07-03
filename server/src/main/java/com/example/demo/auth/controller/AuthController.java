package com.example.demo.auth.controller;

import com.example.demo.auth.dto.*;
import com.example.demo.auth.service.AuthService;
import com.example.demo.auth.validator.AuthValidator;
import com.example.demo.common.dto.ApiResponse;
import com.example.demo.common.utils.ResponseUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static com.example.demo.common.utils.Utils.checkErrors;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthValidator authValidator;

    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<Void>> signup(@Valid @RequestBody SignupRequest signupRequest,
                                                    BindingResult bindingResult) {

        authValidator.validateSignup(signupRequest, bindingResult);
        checkErrors(bindingResult);

        authService.signup(signupRequest);

        return ResponseUtils.created("Signup OTP send to mail");
    }

    @PostMapping("/signup/resend")
    public ResponseEntity<ApiResponse<Void>> resendSignupOtp(@Valid @RequestBody OtpRequest request) {
        authService.resendSignupOtp(request);

        return ResponseUtils.created("Signup OTP again send to mail");
    }

    @PostMapping("/signup/verify")
    public ResponseEntity<ApiResponse<String>> verifySignupOtp(@Valid @RequestBody VerifySignupOtpRequest verifySignupOtpRequest,
                                                               HttpServletResponse response) {

        TokenDto tokenDto = authService.verifySignupOtp(verifySignupOtpRequest);
        authService.addRefreshCookie(response, tokenDto);

        return ResponseUtils.created(tokenDto.getAccessToken(), "Sign up successful!");
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<String>> login(@Valid @RequestBody LoginRequest loginRequest,
                                                     HttpServletResponse response) {

        TokenDto tokenDto = authService.login(loginRequest);
        authService.addRefreshCookie(response, tokenDto);

        return ResponseUtils.ok(tokenDto.getAccessToken(), "Login successful!");
    }

    @PostMapping("/google-login")
    public ResponseEntity<ApiResponse<String>> loginWithGoogle(@RequestBody Map<String, String> request,
                                                               HttpServletResponse response) {

        TokenDto tokenDto = authService.loginWithGoogle(request);
        authService.addRefreshCookie(response, tokenDto);

        return ResponseUtils.ok(tokenDto.getAccessToken(), "Login with Google successful");
    }

    @PostMapping("/access-token/refresh")
    public ResponseEntity<ApiResponse<String>> refreshAccessToken(HttpServletRequest request) {
        String accessToken = authService.refreshAccessToken(request);
        return ResponseUtils.ok(accessToken, "Access Token refreshed successfully");
    }

    @PostMapping("/forget-password")
    public ResponseEntity<ApiResponse<Void>> forgetPassword(@Valid @RequestBody OtpRequest request) {
        authService.forgetPassword(request);
        return ResponseUtils.ok("Password Reset OTP send to mail");
    }

    @PostMapping("/forget-password/resend")
    public ResponseEntity<ApiResponse<Void>> forgetPasswordOtp(@Valid @RequestBody OtpRequest request) {
        authService.forgetPassword(request);
        return ResponseUtils.ok("Password Reset OTP again send to mail");
    }

    @PostMapping("/forget-password/verify")
    public ResponseEntity<ApiResponse<Void>> verifyForgetPasswordOtp(@Valid @RequestBody VerifyForgetPasswordOtpRequest request,
                                                                    BindingResult bindingResult) {

        authValidator.validatePasswords(request.password(), request.confirmPassword(), bindingResult);
        checkErrors(bindingResult);

        authService.verifyForgetPasswordOtp(request);
        return ResponseUtils.ok("Password Reset successful");
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        authService.logout(response);
        return ResponseUtils.ok("Logout Successful");
    }
}
