package com.example.demo.common.serializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import com.fasterxml.jackson.databind.deser.std.StringDeserializer;

import java.io.IOException;

import static com.example.demo.common.utils.Utils.isNull;

public class StringTrimmerDeserializer extends StdScalarDeserializer<String> {

    public StringTrimmerDeserializer() {
        super(String.class);
    }

    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String value = StringDeserializer.instance.deserialize(p, ctxt);
        return isNull(value) ? null : value.trim();
    }
}
