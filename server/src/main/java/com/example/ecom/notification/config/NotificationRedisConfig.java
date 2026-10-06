package com.example.ecom.notification.config;

import com.example.ecom.notification.service.NotificationService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
@ConditionalOnProperty(name = "app.cache.strategy", havingValue = "redis", matchIfMissing = true)
@ConditionalOnBean(RedisConnectionFactory.class)
public class NotificationRedisConfig {

    @Bean
    public RedisMessageListenerContainer notificationRedisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            NotificationService notificationService) {

        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(notificationService, new ChannelTopic(NotificationService.NOTIFICATION_CHANNEL));
        return container;
    }
}
