package com.xiongx9527.fw.plus.ai.model.chat.listener;

import com.xiongx9527.fw.plus.ai.model.ModelProvider;
import com.xiongx9527.fw.plus.ai.model.chat.request.ChatRequest;
import com.xiongx9527.fw.plus.ai.model.chat.response.ChatResponse;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 聊天模型响应上下文。
 *
 * <p><b>作用</b>：同时携带请求与响应，便于监听器计算耗时、比对内容、统计 token。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。镜像
 * {@code dev.langchain4j.model.chat.listener.ChatModelResponseContext}。</p>
 *
 * @author xiongxiao
 */
public class ChatModelResponseContext {

    private final ChatResponse chatResponse;
    private final ChatRequest chatRequest;
    private final ModelProvider modelProvider;
    private final Map<Object, Object> attributes;

    /**
     * 构造响应上下文。
     *
     * @param chatResponse  响应
     * @param chatRequest   请求
     * @param modelProvider 提供方
     * @param attributes    附加属性
     */
    public ChatModelResponseContext(ChatResponse chatResponse, ChatRequest chatRequest,
                                    ModelProvider modelProvider, Map<Object, Object> attributes) {
        this.chatResponse = chatResponse;
        this.chatRequest = chatRequest;
        this.modelProvider = modelProvider;
        this.attributes = attributes == null
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<Object, Object>(attributes));
    }

    /**
     * 响应。
     *
     * @return 响应
     */
    public ChatResponse chatResponse() {
        return chatResponse;
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
