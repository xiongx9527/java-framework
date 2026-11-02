package com.xiongx9527.fw.plus.ai.model.embedding.listener;

/**
 * 嵌入模型监听器：请求前、响应后、异常时回调。
 *
 * <p><b>作用</b>：嵌入调用是 RAG 链路的必经环节，其耗时与失败率直接影响入库和检索体验；
 * 把回调统一挂在这里，换模型实现不必改埋点。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。镜像
 * {@code dev.langchain4j.model.embedding.listener.EmbeddingModelListener}。</p>
 *
 * <p><b>实现约定</b>：全部默认空实现；<b>不得抛异常</b>；回调可能并发，实现需线程安全。</p>
 *
 * @author xiongxiao
 */
public interface EmbeddingModelListener {

    /**
     * 请求前回调。
     *
     * @param requestContext 请求上下文
     */
    default void onRequest(EmbeddingModelRequestContext requestContext) {
    }

    /**
     * 响应后回调。
     *
     * @param responseContext 响应上下文
     */
    default void onResponse(EmbeddingModelResponseContext responseContext) {
    }

    /**
     * 失败回调。
     *
     * @param errorContext 异常上下文
     */
    default void onError(EmbeddingModelErrorContext errorContext) {
    }
}
