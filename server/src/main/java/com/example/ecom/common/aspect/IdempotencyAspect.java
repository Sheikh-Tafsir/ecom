package com.example.ecom.common.aspect;

import com.example.ecom.common.service.IdempotencyService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;

import static com.example.ecom.common.service.IdempotencyService.IDEMPOTENCY_HEADER;
import static com.example.ecom.common.utils.Utils.isNull;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class IdempotencyAspect {

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

        Object cachedResponse = idempotencyService.getCachedResponse(idempotencyKey, requestPayload);
        if (cachedResponse != null) {
            log.info("Duplicate request detected with Idempotency-Key: {}. Returning cached response.", idempotencyKey);
            return cachedResponse;
        }

        // Atomic Redis SET NX lock to prevent duplicate concurrent processing
        if (stringRedisTemplate != null) {
            String lockKey = "lock:idempotency:" + idempotencyKey;
            Boolean acquired = stringRedisTemplate.opsForValue()
                    .setIfAbsent(lockKey, "LOCKED", Duration.ofSeconds(30));

            if (Boolean.FALSE.equals(acquired)) {
                // Another request with same key is in-flight; return cached result or conflict
                Object cached = idempotencyService.getCachedResponse(idempotencyKey, requestPayload);
                if (cached != null) return cached;
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Request is currently being processed.");
            }

            try {
                Object result = joinPoint.proceed();
                try {
                    idempotencyService.save(idempotencyKey, requestPayload, result);
                } catch (Exception e) {
                    log.error("Failed to save response to idempotency cache", e);
                }
                return result;
            } finally {
                stringRedisTemplate.delete(lockKey);
            }
        }

        // Fallback path (no Redis): proceed without distributed lock
        Object result = joinPoint.proceed();

        try {
            idempotencyService.save(idempotencyKey, requestPayload, result);
        } catch (Exception e) {
            log.error("Failed to save response to idempotency cache", e);
        }

        return result;
    }
}
