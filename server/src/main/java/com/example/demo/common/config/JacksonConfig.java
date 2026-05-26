package com.example.demo.common.config;

import com.example.demo.common.serializer.StringTrimmerDeserializer;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

//    @Bean
//    public ObjectMapper objectMapper() {
//        ObjectMapper mapper = new ObjectMapper();
//
//        // Register default modules (JavaTimeModule, ParameterNamesModule, etc.)
//        mapper.findAndRegisterModules();
//
//        // Pretty print JSON
//        mapper.enable(SerializationFeature.INDENT_OUTPUT);
//
//        // Trim all string fields
//        SimpleModule module = new SimpleModule("TrimStringsModule", Version.unknownVersion());
//        module.addDeserializer(String.class, new StringTrimmerDeserializer());
//        mapper.registerModule(module);
//
//        return mapper;
//    }

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
        return builder ->
                builder.deserializerByType(String.class, new StringTrimmerDeserializer());
    }
}