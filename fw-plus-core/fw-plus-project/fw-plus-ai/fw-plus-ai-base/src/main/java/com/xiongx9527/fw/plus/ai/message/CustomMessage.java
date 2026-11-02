package com.xiongx9527.fw.plus.ai.message;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 自定义消息：承载特定厂商 / 特定协议的自由结构。
 *
 * <p><b>作用</b>：当标准四类消息（System / User / Ai / ToolExecutionResult）无法表达某些厂商的
 * 私有字段（如 reasoning 段、引用块）时，用属性包原样透传，避免为每家厂商改端口。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。镜像 {@code dev.langchain4j.data.message.CustomMessage}
 * （{@code attributes()} + {@code from}/{@code customMessage}）。</p>
 *
 * <p><b>实现约定</b>：不可变；属性只读；消费方不认识该结构时应忽略而不是报错。</p>
 *
 * @author xiongxiao
 */
public class CustomMessage implements ChatMessage {

    private final Map<String, Object> attributes;

    /**
     * 构造自定义消息。
     *
     * @param attributes 属性包
     */
    public CustomMessage(Map<String, Object> attributes) {
        this.attributes = attributes == null
                ? Collections.<String, Object>emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<String, Object>(attributes));
    }

    /**
     * 属性包。
     *
     * @return 只读属性，永不为 {@code null}
     */
    public Map<String, Object> attributes() {
        return attributes;
    }

    @Override
    public ChatMessageType type() {
        return ChatMessageType.CUSTOM;
    }

    @Override
    public String toString() {
        return "CustomMessage{attributes=" + attributes + '}';
    }

    /**
     * 由属性包创建。
     *
     * @param attributes 属性包
     * @return 自定义消息
     */
    public static CustomMessage from(Map<String, Object> attributes) {
        return new CustomMessage(attributes);
    }

    /**
     * 由属性包创建（{@link #from(Map)} 的别名）。
     *
     * @param attributes 属性包
     * @return 自定义消息
     */
    public static CustomMessage customMessage(Map<String, Object> attributes) {
        return new CustomMessage(attributes);
    }
}
