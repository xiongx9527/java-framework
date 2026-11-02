package com.xiongx9527.fw.plus.ai.model.decision.listener;

/**
 * 决策模型监听器：请求前、响应后、异常时回调。
 *
 * <p><b>作用</b>：决策模型常处在「风控 / 审批 / 分类」的关键路径上，其耗时与结果必须可观测；
 * 回调统一挂在这里，换决策模型不必改埋点。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。镜像
 * {@code dev.langchain4j.model.decision.listener.DecisionModelListener}。</p>
 *
 * <p><b>实现约定</b>：全部默认空实现；<b>不得抛异常</b>；回调可能并发。</p>
 *
 * @author xiongxiao
 */
public interface DecisionModelListener {

    /**
     * 请求前回调。
     *
     * @param requestContext 请求上下文
     */
    default void onRequest(DecisionModelRequestContext requestContext) {
    }

    /**
     * 响应后回调。
     *
     * @param responseContext 响应上下文
     */
    default void onResponse(DecisionModelResponseContext responseContext) {
    }

    /**
     * 失败回调。
     *
     * @param errorContext 异常上下文
     */
    default void onError(DecisionModelErrorContext errorContext) {
    }
}
