package com.xiongx9527.fw.plus.ai.retriever.listener;

/**
 * 内容检索监听器：检索前 / 检索后 / 异常时回调。
 *
 * <p><b>作用</b>：召回质量分析（命中率、得分分布）、慢检索告警、检索审计都挂在这里。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。镜像
 * {@code dev.langchain4j.rag.content.retriever.listener.ContentRetrieverListener}。</p>
 *
 * <p><b>实现约定</b>：全部默认空实现；<b>不得抛异常</b>；回调可能并发。</p>
 *
 * @author xiongxiao
 */
public interface ContentRetrieverListener {

    /**
     * 检索前回调。
     *
     * @param requestContext 请求上下文
     */
    default void onRequest(ContentRetrieverRequestContext requestContext) {
    }

    /**
     * 检索后回调。
     *
     * @param responseContext 响应上下文
     */
    default void onResponse(ContentRetrieverResponseContext responseContext) {
    }

    /**
     * 检索失败回调。
     *
     * @param errorContext 异常上下文
     */
    default void onError(ContentRetrieverErrorContext errorContext) {
    }
}
