package com.xiongx9527.fw.plus.ai.support.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiongx9527.fw.plus.ai.base.tool.ToolArgumentCodec;
import java.util.Collections;
import java.util.Map;

/**
 * Jackson-based ToolArgumentCodec. Bridges model tool-call JSON to the Map the framework tools consume,
 * and serializes tool results back to JSON. Lives in support so base stays JSON-free.
 */
public class JacksonToolArgumentCodec implements ToolArgumentCodec {

    private final ObjectMapper mapper;

    public JacksonToolArgumentCodec() {
        this(new ObjectMapper());
    }

    public JacksonToolArgumentCodec(ObjectMapper mapper) {
        this.mapper = mapper == null ? new ObjectMapper() : mapper;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> decode(String arguments) {
        if (arguments == null || arguments.trim().isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            Object parsed = mapper.readValue(arguments, Object.class);
            if (!(parsed instanceof Map)) {
                throw new IllegalArgumentException("tool arguments must be a JSON object");
            }
            return (Map<String, Object>) parsed;
        } catch (Exception e) {
            throw new IllegalArgumentException("tool arguments is not valid JSON: " + e.getMessage(), e);
        }
    }

    @Override
    public String encode(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return mapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalArgumentException("tool result is not serializable: " + e.getMessage(), e);
        }
    }
}
