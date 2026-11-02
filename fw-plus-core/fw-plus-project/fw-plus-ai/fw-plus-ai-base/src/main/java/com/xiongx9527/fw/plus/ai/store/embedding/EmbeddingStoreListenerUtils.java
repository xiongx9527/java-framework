package com.xiongx9527.fw.plus.ai.store.embedding;

import com.xiongx9527.fw.plus.ai.store.embedding.listener.EmbeddingStoreErrorContext;
import com.xiongx9527.fw.plus.ai.store.embedding.listener.EmbeddingStoreListener;
import com.xiongx9527.fw.plus.ai.store.embedding.listener.EmbeddingStoreRequestContext;
import com.xiongx9527.fw.plus.ai.store.embedding.listener.EmbeddingStoreResponseContext;

import java.util.List;

/**
 * 向量库监听器派发工具。
 *
 * <p><b>作用</b>：向量库实现只需在三个时机调用这里的静态方法完成广播；内部吞掉监听器异常。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。镜像
 * {@code dev.langchain4j.store.embedding.EmbeddingStoreListenerUtils}（包内可见工具类）。</p>
 *
 * @author xiongxiao
 */
final class EmbeddingStoreListenerUtils {

    private EmbeddingStoreListenerUtils() {
    }

    static void onRequest(EmbeddingStoreRequestContext<?> context, List<EmbeddingStoreListener> listeners) {
        if (listeners == null || listeners.isEmpty()) {
            return;
        }
        for (EmbeddingStoreListener listener : listeners) {
            try {
                listener.onRequest(context);
            } catch (RuntimeException ignored) {
                // 监听器异常不得影响主链路
            }
        }
    }

    static void onResponse(EmbeddingStoreResponseContext<?> context, List<EmbeddingStoreListener> listeners) {
        if (listeners == null || listeners.isEmpty()) {
            return;
        }
        for (EmbeddingStoreListener listener : listeners) {
            try {
                listener.onResponse(context);
            } catch (RuntimeException ignored) {
                // 监听器异常不得影响主链路
            }
        }
    }

    static void onError(EmbeddingStoreErrorContext<?> context, List<EmbeddingStoreListener> listeners) {
        if (listeners == null || listeners.isEmpty()) {
            return;
        }
        for (EmbeddingStoreListener listener : listeners) {
            try {
                listener.onError(context);
            } catch (RuntimeException ignored) {
                // 监听器异常不得影响主链路
            }
        }
    }
}
