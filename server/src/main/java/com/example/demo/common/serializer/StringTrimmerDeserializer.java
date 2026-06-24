package com.example.demo.common.serializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import com.fasterxml.jackson.databind.deser.std.StringDeserializer;

import java.io.IOException;

/**
 * {@link StringTrimmerDeserializer} trims empty spaces from @RequestParam, @PathVariable, @ModelAttribute
 * and form submissions (application/x-www-form-urlencoded)
 */
public class StringTrimmerDeserializer extends StdScalarDeserializer<String> {

    public StringTrimmerDeserializer() {
        super(String.class);
    }

    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String value = StringDeserializer.instance.deserialize(p, ctxt);
        return value == null ? null : value.trim();
    }
}
