package com.xiongx9527.fw.plus.ai.model.chat;

import com.xiongx9527.fw.plus.ai.model.ModelProvider;
import com.xiongx9527.fw.plus.ai.model.chat.listener.ChatModelErrorContext;
import com.xiongx9527.fw.plus.ai.model.chat.listener.ChatModelListener;
import com.xiongx9527.fw.plus.ai.model.chat.listener.ChatModelRequestContext;
import com.xiongx9527.fw.plus.ai.model.chat.listener.ChatModelResponseContext;
import com.xiongx9527.fw.plus.ai.model.chat.request.ChatRequest;
import com.xiongx9527.fw.plus.ai.model.chat.response.ChatResponse;

import java.util.List;
import java.util.Map;

/**
 * 聊天模型监听器派发工具。
 *
 * <p><b>作用</b>：模型实现只需在三个时机调用这里的静态方法，即可完成对所有监听器的广播；
 * 内部吞掉监听器异常，保证监听失败不影响主链路。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。镜像 {@code dev.langchain4j.model.chat.ChatModelListenerUtils}
 * （包内可见的工具类，非公开 API）。</p>
 *
 * @author xiongxiao
 */
final class ChatModelListenerUtils {

    private ChatModelListenerUtils() {
    }

    static void onRequest(ChatRequest chatRequest, ModelProvider modelProvider,
                          Map<Object, Object> attributes, List<ChatModelListener> listeners) {
        if (listeners == null || listeners.isEmpty()) {
            return;
        }
        ChatModelRequestContext context = new ChatModelRequestContext(chatRequest, modelProvider, attributes);
        for (ChatModelListener listener : listeners) {
            try {
                listener.onRequest(context);
            } catch (RuntimeException ignored) {
                // 监听器异常不得影响主链路
            }
        }
    }

    static void onResponse(ChatResponse chatResponse, ChatRequest chatRequest, ModelProvider modelProvider,
                           Map<Object, Object> attributes, List<ChatModelListener> listeners) {
        if (listeners == null || listeners.isEmpty()) {
            return;
        }
        ChatModelResponseContext context =
                new ChatModelResponseContext(chatResponse, chatRequest, modelProvider, attributes);
        for (ChatModelListener listener : listeners) {
            try {
                listener.onResponse(context);
            } catch (RuntimeException ignored) {
                // 监听器异常不得影响主链路
            }
        }
    }

    static void onError(Throwable error, ChatRequest chatRequest, ModelProvider modelProvider,
                        Map<Object, Object> attributes, List<ChatModelListener> listeners) {
        if (listeners == null || listeners.isEmpty()) {
            return;
        }
        ChatModelErrorContext context = new ChatModelErrorContext(error, chatRequest, modelProvider, attributes);
        for (ChatModelListener listener : listeners) {
            try {
                listener.onError(context);
            } catch (RuntimeException ignored) {
                // 监听器异常不得影响主链路
            }
        }
    }
}
