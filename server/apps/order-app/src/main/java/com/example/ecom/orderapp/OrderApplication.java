package com.example.ecom.orderapp;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {
    "com.example.ecom.orderapp",
    "com.example.ecom.common",
    "com.example.ecom.order"
})
@EnableJpaRepositories(basePackages = "com.example.ecom.order")
@EntityScan(basePackages = "com.example.ecom.order")
@ConfigurationPropertiesScan
@EnableJpaAuditing(auditorAwareRef = "auditorAwareImpl")
public class OrderApplication {
    public static void main(String[] args) {
        loadDotenvAsSystemProperties();
        SpringApplication.run(OrderApplication.class, args);
    }

    private static void loadDotenvAsSystemProperties() {
        Dotenv dotenv = Dotenv.configure()
                .ignoreIfMissing()
                .load();
        dotenv.entries().forEach(entry -> System.setProperty(entry.getKey(), entry.getValue()));
    }
}
