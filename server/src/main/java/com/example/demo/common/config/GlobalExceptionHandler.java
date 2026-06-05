package com.example.demo.common.config;

import com.example.demo.common.exception.InvalidAccessTokenException;
import com.example.demo.common.exception.InvalidRefreshTokenException;
import com.example.demo.common.exception.MultipleValidationException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.NoResultException;
import jakarta.validation.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import static com.example.demo.common.utils.ResponseUtils.error;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String ACCESS_TOKEN_INVALID = "Access Token is invalid or expired";

    private static final String REFRESH_TOKEN_INVALID = "Refresh Token is invalid or expired";

    private static final String PERMISSION_DENIED = "Permission Denied";

    private static final String NOT_FOUND = "No result found";

    private static final String SOMETHING_WENT_WRONG = "Something went wrong";

//    @ExceptionHandler(HttpMessageNotReadableException.class)
//    public ResponseEntity<?> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
//        log.error("Request body is missing or unreadable", ex);
//        return error("Request body is missing or malformed", HttpStatusCode.valueOf(400));
//    }

    // 401
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<?> handleInvalidCredentialsExceptions(BadCredentialsException ex) {
        log.error("Authentication credentials invalid: {}", ex.getMessage());
        return error(ex.getMessage(), HttpStatusCode.valueOf(401));
    }

    @ExceptionHandler(InvalidAccessTokenException.class)
    public ResponseEntity<?> handleAccessTokenInvalidExceptions(InvalidAccessTokenException ex) {
        log.error("Access token invalid: {}", ex.getMessage());
        return error(ACCESS_TOKEN_INVALID, HttpStatusCode.valueOf(401));
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<?> handleRefreshTokenInvalidException(InvalidRefreshTokenException ex) {
        log.error("Refresh token invalid: {}", ex.getMessage());
        return error(REFRESH_TOKEN_INVALID, HttpStatusCode.valueOf(401));
    }

    // 403
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<?> handleAccessDeniedException(AccessDeniedException ex) {
        log.error("Access Denied: {}", ex.getMessage());
        return error(PERMISSION_DENIED,HttpStatusCode.valueOf(403));
    }

    // 404
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<?> handleEntityNotFoundException(EntityNotFoundException ex) {
        log.error("Entity not found: {}", ex.getMessage());
        return error(ex.getMessage(), HttpStatusCode.valueOf(404));
    }

    @ExceptionHandler(NoResultException.class)
    public ResponseEntity<?> handleNoResultException(NoResultException ex) {
        log.error("No Result found: {}", ex.getMessage());
        return error(NOT_FOUND, HttpStatusCode.valueOf(404));
    }

    // 422
    @ExceptionHandler(MultipleValidationException.class)
    public ResponseEntity<?> handleMultipleValidationExceptions(MultipleValidationException ex) {
        log.error("Multiple Validation: {}", ex.getBindingResult().toString());
        return error(ex.getBindingResult());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        log.error("Multiple Validation: {}", ex.getBindingResult().toString());
        return error(ex.getBindingResult());
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<?> handleValidationException(ValidationException ex) {
        log.error("Validation: {}", ex.getMessage());
        return error(ex.getMessage(), HttpStatusCode.valueOf(422));
    }

    // 500
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> handleRuntimeException(RuntimeException ex) {
        log.error("Runtime exception caught: {}", ex.getMessage());
        return error(SOMETHING_WENT_WRONG, HttpStatusCode.valueOf(500));
    }

    // Handle all other exceptions
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleAllExceptions(Exception ex) {
        log.error("Unhandled exception caught {}", ex.getMessage());
        return error(SOMETHING_WENT_WRONG, HttpStatusCode.valueOf(500));
    }
}
