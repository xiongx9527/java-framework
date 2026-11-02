package com.xiongx9527.fw.plus.ai.model.moderation;

import com.xiongx9527.fw.plus.ai.model.moderation.listener.ModerationModelErrorContext;
import com.xiongx9527.fw.plus.ai.model.moderation.listener.ModerationModelListener;
import com.xiongx9527.fw.plus.ai.model.moderation.listener.ModerationModelRequestContext;
import com.xiongx9527.fw.plus.ai.model.moderation.listener.ModerationModelResponseContext;

import java.util.List;

/**
 * 内容审核监听器派发工具（包内可见）。
 *
 * <p><b>作用</b>：审核模型实现只需在三个时机调用这里的静态方法完成广播；内部吞掉监听器异常。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。镜像
 * {@code dev.langchain4j.model.moderation.ModerationModelListenerUtils}。</p>
 *
 * @author xiongxiao
 */
final class ModerationModelListenerUtils {

    private ModerationModelListenerUtils() {
    }

    static void onRequest(ModerationModelRequestContext context, List<ModerationModelListener> listeners) {
        if (listeners == null || listeners.isEmpty()) {
            return;
        }
        for (ModerationModelListener listener : listeners) {
            try {
                listener.onRequest(context);
            } catch (RuntimeException ignored) {
                // 监听器异常不得影响主链路
            }
        }
    }

    static void onResponse(ModerationModelResponseContext context, List<ModerationModelListener> listeners) {
        if (listeners == null || listeners.isEmpty()) {
            return;
        }
        for (ModerationModelListener listener : listeners) {
            try {
                listener.onResponse(context);
            } catch (RuntimeException ignored) {
                // 监听器异常不得影响主链路
            }
        }
    }

    static void onError(ModerationModelErrorContext context, List<ModerationModelListener> listeners) {
        if (listeners == null || listeners.isEmpty()) {
            return;
        }
        for (ModerationModelListener listener : listeners) {
            try {
                listener.onError(context);
            } catch (RuntimeException ignored) {
                // 监听器异常不得影响主链路
            }
        }
    }
}
