package com.example.ecom.paymentapp;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {
    "com.example.ecom.paymentapp",
    "com.example.ecom.common",
    "com.example.ecom.payment"
})
@EnableJpaRepositories(basePackages = "com.example.ecom.payment")
@EntityScan(basePackages = "com.example.ecom.payment")
@ConfigurationPropertiesScan(basePackages = {"com.example.ecom.paymentapp", "com.example.ecom.payment"})
@EnableJpaAuditing(auditorAwareRef = "auditorAwareImpl")
public class PaymentApplication {
    public static void main(String[] args) {
        loadDotenvAsSystemProperties();
        SpringApplication.run(PaymentApplication.class, args);
    }

    private static void loadDotenvAsSystemProperties() {
        Dotenv dotenv = Dotenv.configure()
                .ignoreIfMissing()
                .load();
        dotenv.entries().forEach(entry -> System.setProperty(entry.getKey(), entry.getValue()));
    }
}
