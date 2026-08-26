package com.example.ecom.auth.controller;

import com.example.ecom.auth.dto.*;
import com.example.ecom.auth.service.AuthService;
import com.example.ecom.auth.service.AuthTokenService;
import com.example.ecom.auth.service.OAuthService;
import com.example.ecom.auth.validator.AuthValidator;
import com.example.ecom.common.dto.ApiResponse;
import com.example.ecom.common.utils.ResponseUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import static com.example.ecom.common.utils.Utils.checkErrors;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthValidator authValidator;

    private final AuthService authService;

    private final AuthTokenService authTokenService;

    private final OAuthService oAuthService;

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
        authTokenService.addRefreshCookie(response, tokenDto);

        return ResponseUtils.created(tokenDto.getAccessToken(), "Sign up successful!");
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<String>> login(@Valid @RequestBody LoginRequest loginRequest,
                                                     HttpServletResponse response) {

        TokenDto tokenDto = authService.login(loginRequest);
        authTokenService.addRefreshCookie(response, tokenDto);

        return ResponseUtils.ok(tokenDto.getAccessToken(), "Login successful!");
    }

    @PostMapping("/google-login")
    public ResponseEntity<ApiResponse<String>> loginWithGoogle(@Valid @RequestBody GoogleLoginRequest request,
                                                               HttpServletResponse response) {

        TokenDto tokenDto = oAuthService.loginWithGoogle(request);
        authTokenService.addRefreshCookie(response, tokenDto);

        return ResponseUtils.ok(tokenDto.getAccessToken(), "Login with Google successful");
    }

    @PostMapping("/access-token/refresh")
    public ResponseEntity<ApiResponse<String>> refreshAccessToken(HttpServletRequest request,
                                                                 HttpServletResponse response) {
        TokenDto tokenDto = authTokenService.refreshAccessToken(request);
        authTokenService.addRefreshCookie(response, tokenDto);
        return ResponseUtils.ok(tokenDto.getAccessToken(), "Access Token refreshed successfully");
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
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        authTokenService.logout(request, response);
        return ResponseUtils.ok("Logout Successful");
    }
}

