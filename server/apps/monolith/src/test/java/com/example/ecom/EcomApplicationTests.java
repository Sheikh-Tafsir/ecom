package com.example.ecom;

import io.github.cdimascio.dotenv.Dotenv;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.example.ecom.common.event.EventPublisher;

@SpringBootTest
class EcomApplicationTests {

    /** Provide a mock so the context loads without a real RabbitMQ connection. */
    @MockitoBean
    private EventPublisher domainEventPublisher;

    private static final Dotenv DOTENV = Dotenv.configure()
            .ignoreIfMissing()
            .load();

    @DynamicPropertySource
    static void dotenvProperties(DynamicPropertyRegistry registry) {
        DOTENV.entries().forEach(entry -> registry.add(entry.getKey(), entry::getValue));
    }

    @Test
    void contextLoads() {
    }

}
