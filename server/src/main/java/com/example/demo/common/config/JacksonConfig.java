package com.example.demo.common.config;

import com.example.demo.common.serializer.StringTrimmerDeserializer;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    /**
     * {@link StringTrimmerDeserializer} trims empty spaces from @RequestParam, @PathVariable, @ModelAttribute
     * and form submissions (application/x-www-form-urlencoded)
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
        return builder -> {
            builder.modules(new SimpleModule("TrimStringsModule")
                    .addDeserializer(String.class, new StringTrimmerDeserializer()));
            builder.simpleDateFormat("dd-MM-yyyy'T'HH:mm:ss.SSSZ");
            builder.featuresToDisable(DeserializationFeature.ACCEPT_FLOAT_AS_INT);
            builder.featuresToDisable(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE);
            builder.propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
            builder.indentOutput(true);
        };
    }
}