package com.example.gateway.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ResponseUtilsTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private MockServerWebExchange exchange;

    @BeforeEach
    void setUp() {
        exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/test").build()
        );
    }

    @Test
    @DisplayName("error sets the correct HTTP status code on the response")
    void error_validStatus_setsStatusCode() {
        ResponseUtils.error(exchange, HttpStatus.UNAUTHORIZED, "Unauthorized access").block();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    @DisplayName("error sets Content-Type to application/json")
    void error_anyStatus_setsJsonContentType() {
        ResponseUtils.error(exchange, HttpStatus.BAD_REQUEST, "Bad request").block();

        assertEquals(MediaType.APPLICATION_JSON, exchange.getResponse().getHeaders().getContentType());
    }

    @Test
    @DisplayName("error writes JSON body with status and error message")
    void error_withMessage_writesJsonBody() throws Exception {
        ResponseUtils.error(exchange, HttpStatus.FORBIDDEN, "Access denied").block();

        String body = exchange.getResponse().getBodyAsString().block();
        assertNotNull(body);

        @SuppressWarnings("unchecked")
        Map<String, Object> json = OBJECT_MAPPER.readValue(body, Map.class);
        assertEquals(403, json.get("status"));
        assertEquals("Access denied", json.get("error"));
    }

    @Test
    @DisplayName("error with null message writes empty string for error field")
    void error_nullMessage_writesEmptyError() throws Exception {
        ResponseUtils.error(exchange, HttpStatus.INTERNAL_SERVER_ERROR, null).block();

        String body = exchange.getResponse().getBodyAsString().block();
        assertNotNull(body);

        @SuppressWarnings("unchecked")
        Map<String, Object> json = OBJECT_MAPPER.readValue(body, Map.class);
        assertEquals(500, json.get("status"));
        assertEquals("", json.get("error"));
    }

    @Test
    @DisplayName("error with TOO_MANY_REQUESTS sets 429 status")
    void error_tooManyRequests_sets429Status() throws Exception {
        ResponseUtils.error(exchange, HttpStatus.TOO_MANY_REQUESTS, "Rate limit exceeded").block();

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, exchange.getResponse().getStatusCode());

        String body = exchange.getResponse().getBodyAsString().block();
        assertNotNull(body);

        @SuppressWarnings("unchecked")
        Map<String, Object> json = OBJECT_MAPPER.readValue(body, Map.class);
        assertEquals(429, json.get("status"));
        assertEquals("Rate limit exceeded", json.get("error"));
    }

    @Test
    @DisplayName("error with NOT_FOUND sets 404 status")
    void error_notFound_sets404Status() throws Exception {
        ResponseUtils.error(exchange, HttpStatus.NOT_FOUND, "Service Not Found").block();

        assertEquals(HttpStatus.NOT_FOUND, exchange.getResponse().getStatusCode());

        String body = exchange.getResponse().getBodyAsString().block();
        assertNotNull(body);

        @SuppressWarnings("unchecked")
        Map<String, Object> json = OBJECT_MAPPER.readValue(body, Map.class);
        assertEquals(404, json.get("status"));
        assertEquals("Service Not Found", json.get("error"));
    }

    @Test
    @DisplayName("error with SERVICE_UNAVAILABLE sets 503 status")
    void error_serviceUnavailable_sets503Status() {
        ResponseUtils.error(exchange, HttpStatus.SERVICE_UNAVAILABLE, "Service temporarily unavailable").block();

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exchange.getResponse().getStatusCode());
    }
}
