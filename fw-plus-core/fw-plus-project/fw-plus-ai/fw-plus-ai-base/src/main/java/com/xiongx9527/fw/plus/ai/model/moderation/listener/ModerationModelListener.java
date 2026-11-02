package com.xiongx9527.fw.plus.ai.model.moderation.listener;

/**
 * 内容审核模型监听器：请求前、响应后、异常时回调。
 *
 * <p><b>作用</b>：审核调用处在用户输入 / 模型输出的关键路径上，其耗时与失败必须可观测；
 * 回调统一挂在这里，换审核模型不必改埋点。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。镜像
 * {@code dev.langchain4j.model.moderation.listener.ModerationModelListener}。</p>
 *
 * <p><b>实现约定</b>：全部默认空实现；<b>不得抛异常</b>；回调可能并发。</p>
 *
 * @author xiongxiao
 */
public interface ModerationModelListener {

    /**
     * 请求前回调。
     *
     * @param requestContext 请求上下文
     */
    default void onRequest(ModerationModelRequestContext requestContext) {
    }

    /**
     * 响应后回调。
     *
     * @param responseContext 响应上下文
     */
    default void onResponse(ModerationModelResponseContext responseContext) {
    }

    /**
     * 失败回调。
     *
     * @param errorContext 异常上下文
     */
    default void onError(ModerationModelErrorContext errorContext) {
    }
}
