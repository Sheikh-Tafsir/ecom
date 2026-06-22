package com.example.demo;

import io.github.cdimascio.dotenv.Dotenv;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
class DemoApplicationTests {

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
