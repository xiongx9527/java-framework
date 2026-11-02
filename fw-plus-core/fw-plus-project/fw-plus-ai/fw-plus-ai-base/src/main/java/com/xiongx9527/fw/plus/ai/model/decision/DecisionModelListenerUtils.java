package com.xiongx9527.fw.plus.ai.model.decision;

import com.xiongx9527.fw.plus.ai.model.decision.listener.DecisionModelErrorContext;
import com.xiongx9527.fw.plus.ai.model.decision.listener.DecisionModelListener;
import com.xiongx9527.fw.plus.ai.model.decision.listener.DecisionModelRequestContext;
import com.xiongx9527.fw.plus.ai.model.decision.listener.DecisionModelResponseContext;

import java.util.List;

/**
 * 决策模型监听器派发工具（包内可见）。
 *
 * <p><b>作用</b>：决策模型实现只需在三个时机调用这里的静态方法完成广播；内部吞掉监听器异常。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。镜像
 * {@code dev.langchain4j.model.decision.DecisionModelListenerUtils}。</p>
 *
 * @author xiongxiao
 */
final class DecisionModelListenerUtils {

    private DecisionModelListenerUtils() {
    }

    static void onRequest(DecisionModelRequestContext context, List<DecisionModelListener> listeners) {
        if (listeners == null || listeners.isEmpty()) {
            return;
        }
        for (DecisionModelListener listener : listeners) {
            try {
                listener.onRequest(context);
            } catch (RuntimeException ignored) {
                // 监听器异常不得影响主链路
            }
        }
    }

    static void onResponse(DecisionModelResponseContext context, List<DecisionModelListener> listeners) {
        if (listeners == null || listeners.isEmpty()) {
            return;
        }
        for (DecisionModelListener listener : listeners) {
            try {
                listener.onResponse(context);
            } catch (RuntimeException ignored) {
                // 监听器异常不得影响主链路
            }
        }
    }

    static void onError(DecisionModelErrorContext context, List<DecisionModelListener> listeners) {
        if (listeners == null || listeners.isEmpty()) {
            return;
        }
        for (DecisionModelListener listener : listeners) {
            try {
                listener.onError(context);
            } catch (RuntimeException ignored) {
                // 监听器异常不得影响主链路
            }
        }
    }
}
