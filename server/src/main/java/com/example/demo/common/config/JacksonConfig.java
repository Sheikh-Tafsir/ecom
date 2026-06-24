package com.example.demo.common.config;

import com.example.demo.common.serializer.StringTrimmerDeserializer;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@link Jackson2ObjectMapperBuilderCustomizer} trims empty spaces from @RequestBdy in form submissions (application/json)
 * through {@link StringTrimmerDeserializer}
 */
@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
        return builder -> {
            builder.featuresToDisable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
            builder.featuresToEnable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
            builder.featuresToEnable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS);
            builder.modules(new SimpleModule("TrimStringsModule")
                    .addDeserializer(String.class, new StringTrimmerDeserializer()));
            builder.modules(new JavaTimeModule());
            builder.simpleDateFormat("dd-MM-yyyy'T'HH:mm:ss.SSSZ");

            builder.indentOutput(true);
        };
    }
}