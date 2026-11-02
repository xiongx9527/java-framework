package com.xiongx9527.fw.plus.ai.store.embedding.listener;

/**
 * 向量库监听器：写入 / 检索前、后、异常时回调。
 *
 * <p><b>作用</b>：向量库的耗时、失败率、召回规模都需要观测；把回调统一挂在这里，
 * 换向量库实现不必改埋点代码。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。镜像
 * {@code dev.langchain4j.store.embedding.listener.EmbeddingStoreListener}。</p>
 *
 * <p><b>实现约定</b>：全部默认空实现；<b>不得抛异常</b>；回调可能并发。</p>
 *
 * @author xiongxiao
 */
public interface EmbeddingStoreListener {

    /**
     * 请求前回调。
     *
     * @param requestContext 请求上下文
     */
    default void onRequest(EmbeddingStoreRequestContext<?> requestContext) {
    }

    /**
     * 响应后回调。
     *
     * @param responseContext 响应上下文
     */
    default void onResponse(EmbeddingStoreResponseContext<?> responseContext) {
    }

    /**
     * 失败回调。
     *
     * @param errorContext 异常上下文
     */
    default void onError(EmbeddingStoreErrorContext<?> errorContext) {
    }
}
