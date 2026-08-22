package com.example.ecom.common.aspect;

import com.example.ecom.common.service.IdempotencyService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdempotencyAspectTest {

    @Mock
    private IdempotencyService idempotencyService;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @InjectMocks
    private IdempotencyAspect idempotencyAspect;

    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void enforceIdempotency_missingHeader_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> idempotencyAspect.enforceIdempotency(joinPoint));
    }

    @Test
    void enforceIdempotency_cachedResponseExists_returnsCachedResponse() throws Throwable {
        String key = "test-key-123";
        request.addHeader(IdempotencyService.IDEMPOTENCY_HEADER, key);

        String payload = "payload-data";
        when(joinPoint.getArgs()).thenReturn(new Object[]{payload});
        when(idempotencyService.getCachedResponse(eq(key), eq(payload))).thenReturn("CACHED_RESULT");

        Object result = idempotencyAspect.enforceIdempotency(joinPoint);

        assertEquals("CACHED_RESULT", result);
        verify(joinPoint, never()).proceed();
    }

    @Test
    void enforceIdempotency_noCachedResponse_executesAndSaves() throws Throwable {
        String key = "test-key-123";
        request.addHeader(IdempotencyService.IDEMPOTENCY_HEADER, key);

        String payload = "payload-data";
        when(joinPoint.getArgs()).thenReturn(new Object[]{payload});
        when(idempotencyService.getCachedResponse(eq(key), eq(payload))).thenReturn(null);
        when(joinPoint.proceed()).thenReturn("FRESH_RESULT");

        Object result = idempotencyAspect.enforceIdempotency(joinPoint);

        assertEquals("FRESH_RESULT", result);
        verify(joinPoint, times(1)).proceed();
        verify(idempotencyService, times(1)).save(key, payload, "FRESH_RESULT");
    }
}
