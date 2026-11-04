package com.xiongx9527.fw.plus.ai.support.output;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiongx9527.fw.plus.ai.base.output.JsonBindings;
import com.xiongx9527.fw.plus.ai.base.output.JsonOutputException;
import java.lang.reflect.Type;

/**
 * {@link JsonBindings} 的 Jackson 实现：把 {@code base} 提取出的 JSON 文本反序列化为目标类型。
 *
 * <p>目标类型支持参数化（如 {@code List<Foo>}）：底层用 {@link ObjectMapper#getTypeFactory()}
 * 构造 {@link com.fasterxml.jackson.databind.JavaType}，因此 {@link Type} 即可正确绑定泛型。</p>
 *
 * <p>反序列化失败统一包装为 {@link JsonOutputException}，避免把 Jackson 的受检/非受检异常泄漏到 {@code base} 层。</p>
 */
public class JacksonJsonBindings implements JsonBindings {

    private final ObjectMapper mapper;

    /** 使用默认 {@link ObjectMapper} 构造。 */
    public JacksonJsonBindings() {
        this(new ObjectMapper());
    }

    /** 使用调用方提供的 {@link ObjectMapper}（可预配置日期格式 / 命名策略等）。 */
    public JacksonJsonBindings(ObjectMapper mapper) {
        if (mapper == null) {
            throw new IllegalArgumentException("ObjectMapper is required");
        }
        this.mapper = mapper;
    }

    @Override
    public <T> T bind(String json, Type type) {
        if (json == null) {
            return null;
        }
        try {
            return mapper.readValue(json, mapper.getTypeFactory().constructType(type));
        } catch (Exception e) {
            throw new JsonOutputException("failed to bind JSON to " + type + ": " + e.getMessage(), e);
        }
    }
}
