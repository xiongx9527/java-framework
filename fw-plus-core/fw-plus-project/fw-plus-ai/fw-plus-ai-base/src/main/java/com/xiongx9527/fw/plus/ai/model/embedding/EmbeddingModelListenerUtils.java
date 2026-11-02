package com.xiongx9527.fw.plus.ai.model.embedding;

import com.xiongx9527.fw.plus.ai.model.embedding.listener.EmbeddingModelErrorContext;
import com.xiongx9527.fw.plus.ai.model.embedding.listener.EmbeddingModelListener;
import com.xiongx9527.fw.plus.ai.model.embedding.listener.EmbeddingModelRequestContext;
import com.xiongx9527.fw.plus.ai.model.embedding.listener.EmbeddingModelResponseContext;

import java.util.List;

/**
 * 嵌入模型监听器派发工具（包内可见）。
 *
 * <p><b>作用</b>：模型实现只需在三个时机调用这里的静态方法完成广播；内部吞掉监听器异常。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。镜像
 * {@code dev.langchain4j.model.embedding.EmbeddingModelListenerUtils}。</p>
 *
 * @author xiongxiao
 */
final class EmbeddingModelListenerUtils {

    private EmbeddingModelListenerUtils() {
    }

    static void onRequest(EmbeddingModelRequestContext context, List<EmbeddingModelListener> listeners) {
        if (listeners == null || listeners.isEmpty()) {
            return;
        }
        for (EmbeddingModelListener listener : listeners) {
            try {
                listener.onRequest(context);
            } catch (RuntimeException ignored) {
                // 监听器异常不得影响主链路
            }
        }
    }

    static void onResponse(EmbeddingModelResponseContext context, List<EmbeddingModelListener> listeners) {
        if (listeners == null || listeners.isEmpty()) {
            return;
        }
        for (EmbeddingModelListener listener : listeners) {
            try {
                listener.onResponse(context);
            } catch (RuntimeException ignored) {
                // 监听器异常不得影响主链路
            }
        }
    }

    static void onError(EmbeddingModelErrorContext context, List<EmbeddingModelListener> listeners) {
        if (listeners == null || listeners.isEmpty()) {
            return;
        }
        for (EmbeddingModelListener listener : listeners) {
            try {
                listener.onError(context);
            } catch (RuntimeException ignored) {
                // 监听器异常不得影响主链路
            }
        }
    }
}
