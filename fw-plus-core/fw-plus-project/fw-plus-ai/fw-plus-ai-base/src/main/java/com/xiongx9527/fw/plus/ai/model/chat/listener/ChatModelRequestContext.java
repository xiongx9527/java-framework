package com.xiongx9527.fw.plus.ai.model.chat.listener;

import com.xiongx9527.fw.plus.ai.model.ModelProvider;
import com.xiongx9527.fw.plus.ai.model.chat.request.ChatRequest;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 聊天模型请求上下文。
 *
 * <p><b>作用</b>：把「发出去的请求 + 提供方 + 调用方附加属性」打包给监听器。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。镜像
 * {@code dev.langchain4j.model.chat.listener.ChatModelRequestContext}。</p>
 *
 * @author xiongxiao
 */
public class ChatModelRequestContext {

    private final ChatRequest chatRequest;
    private final ModelProvider modelProvider;
    private final Map<Object, Object> attributes;

    /**
     * 构造请求上下文。
     *
     * @param chatRequest   请求
     * @param modelProvider 提供方
     * @param attributes    附加属性
     */
    public ChatModelRequestContext(ChatRequest chatRequest, ModelProvider modelProvider, Map<Object, Object> attributes) {
        this.chatRequest = chatRequest;
        this.modelProvider = modelProvider;
        this.attributes = attributes == null
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<Object, Object>(attributes));
    }

    /**
     * 请求。
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
