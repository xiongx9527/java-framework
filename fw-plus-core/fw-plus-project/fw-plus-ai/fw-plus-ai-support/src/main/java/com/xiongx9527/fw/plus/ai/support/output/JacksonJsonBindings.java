package com.xiongx9527.fw.plus.ai.support.output;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiongx9527.fw.plus.ai.base.output.JsonBindings;
import com.xiongx9527.fw.plus.ai.base.output.JsonOutputException;
import java.lang.reflect.Type;

/**
 * Jackson-based JsonBindings for AiServices JsonResult structured returns.
 */
public class JacksonJsonBindings implements JsonBindings {

    private final ObjectMapper mapper;

    public JacksonJsonBindings() {
        this(new ObjectMapper());
    }

    public JacksonJsonBindings(ObjectMapper mapper) {
        this.mapper = mapper == null ? new ObjectMapper() : mapper;
    }

    @Override
    public <T> T bind(String json, Type type) {
        if (json == null) {
            return null;
        }
        try {
            return mapper.readValue(json, mapper.getTypeFactory().constructType(type));
        } catch (Exception e) {
            throw new JsonOutputException("json bind failed: " + e.getMessage(), e);
        }
    }
}
