package com.example.demo.common.config;

import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${spring.rabbitmq.mail.queue}")
    private String mailQueue;

    @Value("${spring.rabbitmq.mail.exchange}")
    private String mailExchange;

    @Value("${spring.rabbitmq.mail.routing-key}")
    private String mailRoutingKey;

    @Value("${spring.rabbitmq.service.queue}")
    private String serviceQueue;

    @Value("${spring.rabbitmq.service.exchange}")
    private String serviceExchange;

    @Value("${spring.rabbitmq.service.routing-key}")
    private String serviceRoutingKey;

    // For mail
    @Bean
    public Queue mailQueue() {
        return QueueBuilder.durable(mailQueue)
                .build();
    }

    @Bean
    public DirectExchange mailExchange() {
        return new DirectExchange(mailExchange);
    }

    @Bean
    public Binding mailBinding() {
        return BindingBuilder
                .bind(mailQueue())
                .to(mailExchange())
                .with(mailRoutingKey);
    }

    //For services
    @Bean
    public Queue serviceQueue() {
        return QueueBuilder.durable(serviceQueue).build();
    }

    @Bean
    public TopicExchange topicExchange() {
        return new TopicExchange(serviceExchange);
    }

    @Bean
    public Binding serviceBinding() {
        return BindingBuilder.bind(serviceQueue()).to(topicExchange()).with(serviceRoutingKey);
    }
}

