package com.example.ecom.common.aspect;

import com.example.ecom.common.service.IdempotencyService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;

import static com.example.ecom.common.service.IdempotencyService.IDEMPOTENCY_HEADER;
import static com.example.ecom.common.utils.Utils.isNull;
import static com.example.ecom.common.utils.Utils.objectHash;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class IdempotencyAspect {

    public static final String IDEMPOTENCY_LOCK_KEY_PREFIX = "lock:idempotency:";

    private final IdempotencyService idempotencyService;

    @Autowired(required = false)
    private StringRedisTemplate stringRedisTemplate;

    @Around("@annotation(com.example.ecom.common.annotation.Idempotent)")
    public Object enforceIdempotency(ProceedingJoinPoint joinPoint) throws Throwable {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return joinPoint.proceed();
        }

        HttpServletRequest request = attributes.getRequest();
        String idempotencyKey = request.getHeader(IDEMPOTENCY_HEADER);

        if (isNull(idempotencyKey)) {
            throw new IllegalArgumentException("Missing required Idempotency-Key header for this request.");
        }

        // Get method arguments to construct a request hash representation
        Object[] args = joinPoint.getArgs();
        Object requestPayload = null;
        for (Object arg : args) {
            if (arg != null
                    && !(arg instanceof HttpServletRequest)
                    && !(arg instanceof jakarta.servlet.http.HttpServletResponse)
                    && !(arg instanceof org.springframework.validation.BindingResult)) {
                requestPayload = arg;
                break;
            }
        }
        if (requestPayload == null) {
            requestPayload = "EMPTY_PAYLOAD";
        }
        
        String existingHash = idempotencyService.getStoredPayloadHash(idempotencyKey);
        if (existingHash != null && !existingHash.equals(objectHash(requestPayload))) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Idempotency-Key reused with a different request payload");
        }

        Object cachedResponse = idempotencyService.getCachedResponse(idempotencyKey, requestPayload);
        if (cachedResponse != null) {
            log.info("Duplicate request detected with Idempotency-Key: {}. Returning cached response.", idempotencyKey);
            return cachedResponse;
        }

        boolean redisAvailable = true;
        boolean redisLockAcquired = false;
        String lockKey = null;

        if (stringRedisTemplate != null) {
            lockKey = IDEMPOTENCY_LOCK_KEY_PREFIX + idempotencyKey;

            try {
                Boolean acquired = stringRedisTemplate.opsForValue()
                        .setIfAbsent(lockKey, "LOCKED", Duration.ofSeconds(30));

                if (Boolean.FALSE.equals(acquired)) {
                    // Another request with same key is in-flight; return cached result or conflict
                    Object cached = idempotencyService.getCachedResponse(idempotencyKey, requestPayload);
                    if (cached != null) return cached;
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Request is currently being processed.");
                }

                redisLockAcquired = true;
            } catch (RedisConnectionFailureException e) {
                log.warn("Redis unavailable for idempotency lock; falling back to DB constraint: {}", e.getMessage());
                redisAvailable = false;
            }
        }

        // --- Execution + cleanup (flat) ---
        try {
            Object result = joinPoint.proceed();
            if (redisAvailable) {
                try {
                    idempotencyService.save(idempotencyKey, requestPayload, result);
                    idempotencyService.savePayloadHash(idempotencyKey, requestPayload);
                } catch (Exception e) {
                    log.error("Failed to save response to idempotency cache", e);
                }
            }
            return result;
        } finally {
            if (redisLockAcquired) {
                try {
                    stringRedisTemplate.delete(lockKey);
                } catch (RedisConnectionFailureException ex) {
                    log.warn("Redis unavailable for lock cleanup; lock will expire via TTL: {}", ex.getMessage());
                }
            }
        }
    }
}
