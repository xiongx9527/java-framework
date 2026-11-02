package com.xiongx9527.fw.plus.ai.model.chat.listener;

import com.xiongx9527.fw.plus.ai.model.ModelProvider;
import com.xiongx9527.fw.plus.ai.model.chat.request.ChatRequest;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 聊天模型异常上下文。
 *
 * <p><b>作用</b>：携带失败原因与原请求，供熔断、告警、故障转移决策使用。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。镜像
 * {@code dev.langchain4j.model.chat.listener.ChatModelErrorContext}。</p>
 *
 * @author xiongxiao
 */
public class ChatModelErrorContext {

    private final Throwable error;
    private final ChatRequest chatRequest;
    private final ModelProvider modelProvider;
    private final Map<Object, Object> attributes;

    /**
     * 构造异常上下文。
     *
     * @param error         异常
     * @param chatRequest   原请求
     * @param modelProvider 提供方
     * @param attributes    附加属性
     */
    public ChatModelErrorContext(Throwable error, ChatRequest chatRequest,
                                 ModelProvider modelProvider, Map<Object, Object> attributes) {
        this.error = error;
        this.chatRequest = chatRequest;
        this.modelProvider = modelProvider;
        this.attributes = attributes == null
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<Object, Object>(attributes));
    }

    /**
     * 异常。
     *
     * @return 异常
     */
    public Throwable error() {
        return error;
    }

    /**
     * 原请求。
     *
     * @return 请求
     */
    public ChatRequest chatRequest() {
        return chatRequest;
    }

    /**
     * 提供方。
     *
     * @return 提供方
     */
    public ModelProvider modelProvider() {
        return modelProvider;
    }

    /**
     * 附加属性。
     *
     * @return 只读属性
     */
    public Map<Object, Object> attributes() {
        return attributes;
    }
}
