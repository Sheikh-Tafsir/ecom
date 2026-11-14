package com.example.ecom.productapp;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {
    "com.example.ecom.productapp",
    "com.example.ecom.common",
    "com.example.ecom.product"
})
@EnableJpaRepositories(basePackages = "com.example.ecom.product")
@EntityScan(basePackages = "com.example.ecom.product")
@ConfigurationPropertiesScan
@EnableJpaAuditing(auditorAwareRef = "auditorAwareImpl")
public class ProductApplication {
    public static void main(String[] args) {
        loadDotenvAsSystemProperties();
        SpringApplication.run(ProductApplication.class, args);
    }

    private static void loadDotenvAsSystemProperties() {
        Dotenv dotenv = Dotenv.configure()
                .ignoreIfMissing()
                .load();
        dotenv.entries().forEach(entry -> System.setProperty(entry.getKey(), entry.getValue()));
    }
}
