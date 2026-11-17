package com.example.ecom.common.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.interceptor.RetryInterceptorBuilder;

import static com.example.ecom.common.event.EventConstants.*;

/**
 * RabbitMQ infrastructure for domain events.
 * <p>
 * Sets up:
 * <ul>
 *     <li>Topic exchanges per bounded context ({@code ecom.orders}, {@code ecom.payments})</li>
 *     <li>Durable queues per consuming service with dead-letter routing</li>
 *     <li>A shared dead-letter exchange ({@code ecom.dlx}) and per-queue DLQs</li>
 *     <li>Jackson JSON message converter for all RabbitMQ messages</li>
 * </ul>
 */
@Configuration
@ConditionalOnBean(ConnectionFactory.class)
public class DomainEventConfig {

    // ── Message converter (JSON instead of Java serialization) ──────

    @Bean
    public MessageConverter jacksonMessageConverter(ObjectMapper objectMapper) {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(objectMapper);

        // Configure type mapper so Jackson can deserialize generic DomainEvent<T>
        // by embedding the full class name in __TypeId__ message headers.
        DefaultJackson2JavaTypeMapper typeMapper = new DefaultJackson2JavaTypeMapper();
        typeMapper.setTrustedPackages("com.example.ecom.*");
        converter.setJavaTypeMapper(typeMapper);

        return converter;
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         MessageConverter jacksonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jacksonMessageConverter);
        return template;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter jacksonMessageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jacksonMessageConverter);
        factory.setDefaultRequeueRejected(false); // send to DLQ on failure, don't requeue infinitely
        factory.setAdviceChain(RetryInterceptorBuilder.stateless()
                .maxAttempts(3)
                .backOffOptions(1000, 2.0, 10000)
                .build());
        return factory;
    }

    // ── Dead-letter exchange ────────────────────────────────────────

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DLX);
    }

    // ── Order events (published by order-service) ───────────────────

    @Bean
    public TopicExchange orderExchange() {
        return ExchangeBuilder.topicExchange(ORDER_EXCHANGE).durable(true).build();
    }

    /** Queue for product-service to consume order events (e.g., stock release). */
    @Bean
    public Queue productOrderEventsQueue() {
        return QueueBuilder.durable(PRODUCT_ORDER_EVENTS_QUEUE)
                .deadLetterExchange(DLX)
                .deadLetterRoutingKey(PRODUCT_ORDER_EVENTS_DLQ)
                .build();
    }

    @Bean
    public Queue productOrderEventsDlq() {
        return QueueBuilder.durable(PRODUCT_ORDER_EVENTS_DLQ).build();
    }

    @Bean
    public Binding productOrderEventsBinding() {
        return BindingBuilder
                .bind(productOrderEventsQueue())
                .to(orderExchange())
                .with("order.#");
    }

    @Bean
    public Binding productOrderEventsDlqBinding() {
        return BindingBuilder
                .bind(productOrderEventsDlq())
                .to(deadLetterExchange())
                .with(PRODUCT_ORDER_EVENTS_DLQ);
    }

    // ── Payment events (published by payment-service) ───────────────

    @Bean
    public TopicExchange paymentExchange() {
        return ExchangeBuilder.topicExchange(PAYMENT_EXCHANGE).durable(true).build();
    }

    /** Queue for order-service to consume payment events (e.g., payment completed). */
    @Bean
    public Queue orderPaymentEventsQueue() {
        return QueueBuilder.durable(ORDER_PAYMENT_EVENTS_QUEUE)
                .deadLetterExchange(DLX)
                .deadLetterRoutingKey(ORDER_PAYMENT_EVENTS_DLQ)
                .build();
    }

    @Bean
    public Queue orderPaymentEventsDlq() {
        return QueueBuilder.durable(ORDER_PAYMENT_EVENTS_DLQ).build();
    }

    @Bean
    public Binding orderPaymentEventsBinding() {
        return BindingBuilder
                .bind(orderPaymentEventsQueue())
                .to(paymentExchange())
                .with("payment.#");
    }

    @Bean
    public Binding orderPaymentEventsDlqBinding() {
        return BindingBuilder
                .bind(orderPaymentEventsDlq())
                .to(deadLetterExchange())
                .with(ORDER_PAYMENT_EVENTS_DLQ);
    }

    // ── Mail events ─────────────────────────────────────────────────

    @Bean
    public DirectExchange mailExchange() {
        return new DirectExchange(MAIL_EXCHANGE);
    }

    @Bean
    public Queue mailQueue() {
        return QueueBuilder.durable(MAIL_QUEUE).build();
    }

    @Bean
    public Binding mailBinding() {
        return BindingBuilder
                .bind(mailQueue())
                .to(mailExchange())
                .with(MAIL_SEND);
    }
}
