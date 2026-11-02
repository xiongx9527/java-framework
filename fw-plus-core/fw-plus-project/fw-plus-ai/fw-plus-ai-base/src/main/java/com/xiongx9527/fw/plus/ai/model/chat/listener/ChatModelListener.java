package com.xiongx9527.fw.plus.ai.model.chat.listener;

/**
 * 聊天模型监听器：在请求前 / 响应后 / 异常时回调。
 *
 * <p><b>作用</b>：日志、指标、成本核算、审计、脱敏都挂在这里，而不侵入模型实现。
 * 同一组监听器可同时挂到多个模型实例上。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。镜像
 * {@code dev.langchain4j.model.chat.listener.ChatModelListener}。</p>
 *
 * <p><b>实现约定</b>：全部默认空实现；<b>监听器不得抛异常</b>，失败自行吞掉并记日志；
 * 回调可能并发，实现需线程安全。</p>
 *
 * @author xiongxiao
 */
public interface ChatModelListener {

    /**
     * 请求发出前回调。
     *
     * @param requestContext 请求上下文
     */
    default void onRequest(ChatModelRequestContext requestContext) {
    }

    /**
     * 收到响应后回调。
     *
     * @param responseContext 响应上下文
     */
    default void onResponse(ChatModelResponseContext responseContext) {
    }

    /**
     * 调用失败后回调。
     *
     * @param errorContext 异常上下文
     */
    default void onError(ChatModelErrorContext errorContext) {
    }
}
