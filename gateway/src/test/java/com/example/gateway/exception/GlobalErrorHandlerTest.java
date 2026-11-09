package com.example.gateway.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.*;

class GlobalErrorHandlerTest {

    private GlobalErrorHandler errorHandler;
    private MockServerWebExchange exchange;

    @BeforeEach
    void setUp() {
        errorHandler = new GlobalErrorHandler();
        exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/products").build()
        );
    }

    @Test
    @DisplayName("handle returns Mono.error when response is already committed")
    void handle_committedResponse_returnsMonoError() {
        MockServerWebExchange committedExchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/test").build()
        );
        committedExchange.getResponse().setComplete().block();

        Throwable ex = new RuntimeException("some error");

        Mono<Void> result = errorHandler.handle(committedExchange, ex);
        assertThrows(RuntimeException.class, result::block);
    }

    @Test
    @DisplayName("handle sets 404 NOT_FOUND for NotFoundException")
    void handle_notFoundException_returns404() {
        NotFoundException ex = new NotFoundException("Service not reachable");

        errorHandler.handle(exchange, ex).block();

        assertEquals(HttpStatus.NOT_FOUND, exchange.getResponse().getStatusCode());
    }

    @Test
    @DisplayName("handle sets appropriate status for ResponseStatusException")
    void handle_responseStatusException_returnsMatchingStatus() {
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Downstream failure");

        errorHandler.handle(exchange, ex).block();

        assertEquals(HttpStatus.BAD_GATEWAY, exchange.getResponse().getStatusCode());
    }

    @Test
    @DisplayName("handle uses reason phrase when ResponseStatusException has null reason")
    void handle_responseStatusExceptionNullReason_usesReasonPhrase() {
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.FORBIDDEN);

        errorHandler.handle(exchange, ex).block();

        assertEquals(HttpStatus.FORBIDDEN, exchange.getResponse().getStatusCode());
    }

    @Test
    @DisplayName("handle sets 500 INTERNAL_SERVER_ERROR for generic exceptions")
    void handle_genericException_returns500() {
        RuntimeException ex = new RuntimeException("Unexpected error");

        errorHandler.handle(exchange, ex).block();

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exchange.getResponse().getStatusCode());
    }

    @Test
    @DisplayName("handle sets 500 for NullPointerException")
    void handle_nullPointerException_returns500() {
        NullPointerException ex = new NullPointerException("null ref");

        errorHandler.handle(exchange, ex).block();

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exchange.getResponse().getStatusCode());
    }

    @Test
    @DisplayName("handle sets 500 for IllegalStateException")
    void handle_illegalStateException_returns500() {
        IllegalStateException ex = new IllegalStateException("bad state");

        errorHandler.handle(exchange, ex).block();

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exchange.getResponse().getStatusCode());
    }

    @Test
    @DisplayName("handle ResponseStatusException with SERVICE_UNAVAILABLE sets 503")
    void handle_serviceUnavailableException_returns503() {
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Service down");

        errorHandler.handle(exchange, ex).block();

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exchange.getResponse().getStatusCode());
    }
}
