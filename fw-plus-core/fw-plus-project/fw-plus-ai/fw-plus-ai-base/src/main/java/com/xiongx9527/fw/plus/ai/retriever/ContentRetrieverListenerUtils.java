package com.xiongx9527.fw.plus.ai.retriever;

import com.xiongx9527.fw.plus.ai.retriever.listener.ContentRetrieverErrorContext;
import com.xiongx9527.fw.plus.ai.retriever.listener.ContentRetrieverListener;
import com.xiongx9527.fw.plus.ai.retriever.listener.ContentRetrieverRequestContext;
import com.xiongx9527.fw.plus.ai.retriever.listener.ContentRetrieverResponseContext;

import java.util.List;

/**
 * 内容检索监听器派发工具。
 *
 * <p><b>作用</b>：检索器实现只需在三个时机调用这里的静态方法完成广播；内部吞掉监听器异常。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。镜像
 * {@code dev.langchain4j.rag.content.retriever.ContentRetrieverListenerUtils}（包内可见工具类）。</p>
 *
 * @author xiongxiao
 */
final class ContentRetrieverListenerUtils {

    private ContentRetrieverListenerUtils() {
    }

    static void onRequest(ContentRetrieverRequestContext context, List<ContentRetrieverListener> listeners) {
        if (listeners == null || listeners.isEmpty()) {
            return;
        }
        for (ContentRetrieverListener listener : listeners) {
            try {
                listener.onRequest(context);
            } catch (RuntimeException ignored) {
                // 监听器异常不得影响主链路
            }
        }
    }

    static void onResponse(ContentRetrieverResponseContext context, List<ContentRetrieverListener> listeners) {
        if (listeners == null || listeners.isEmpty()) {
            return;
        }
        for (ContentRetrieverListener listener : listeners) {
            try {
                listener.onResponse(context);
            } catch (RuntimeException ignored) {
                // 监听器异常不得影响主链路
            }
        }
    }

    static void onError(ContentRetrieverErrorContext context, List<ContentRetrieverListener> listeners) {
        if (listeners == null || listeners.isEmpty()) {
            return;
        }
        for (ContentRetrieverListener listener : listeners) {
            try {
                listener.onError(context);
            } catch (RuntimeException ignored) {
                // 监听器异常不得影响主链路
            }
        }
    }
}
