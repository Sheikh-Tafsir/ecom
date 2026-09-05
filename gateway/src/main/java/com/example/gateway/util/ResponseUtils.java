package com.example.gateway.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Map;

public final class ResponseUtils {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private ResponseUtils() {
    }

    public static Mono<Void> error(ServerWebExchange exchange, HttpStatus status, String message) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        byte[] bytes;
        try {
            bytes = OBJECT_MAPPER.writeValueAsBytes(Map.of(
                    "status", status.value(),
                    "error", message != null ? message : ""
            ));
        } catch (JsonProcessingException e) {
            String fallback = "{\"status\":" + status.value() + ",\"error\":\"Internal error serializing response\"}";
            bytes = fallback.getBytes(StandardCharsets.UTF_8);
        }

        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }
}
