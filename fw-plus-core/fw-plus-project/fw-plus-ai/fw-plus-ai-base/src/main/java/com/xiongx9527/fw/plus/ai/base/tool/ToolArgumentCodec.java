package com.xiongx9527.fw.plus.ai.base.tool;

import java.util.Map;

/**
 * 工具参数编解码端口：JSON ↔ Map。
 *
 * <p>base 不引入 JSON 库（避免把序列化实现绑进契约），由 adapter 用 Jackson / fastjson 实现。</p>
 */
public interface ToolArgumentCodec {

    /** 解析参数 JSON；空串或 {@code null} 返回空表。 */
    Map<String, Object> decode(String arguments);

    /** 序列化返回值；{@code null} 返回 {@code null}。 */
    String encode(Object value);
}
