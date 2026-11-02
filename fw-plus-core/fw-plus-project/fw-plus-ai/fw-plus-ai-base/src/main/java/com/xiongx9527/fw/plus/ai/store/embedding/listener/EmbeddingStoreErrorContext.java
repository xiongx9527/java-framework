package com.xiongx9527.fw.plus.ai.store.embedding.listener;

import com.xiongx9527.fw.plus.ai.store.embedding.EmbeddingStore;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 向量库异常上下文。
 *
 * <p><b>作用</b>：携带失败原因与原请求上下文，供降级（跳过检索）与告警使用。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。镜像
 * {@code dev.langchain4j.store.embedding.listener.EmbeddingStoreErrorContext}。</p>
 *
 * @param <Embedded> 被嵌入对象类型
 * @author xiongxiao
 */
public class EmbeddingStoreErrorContext<Embedded> {

    private final Throwable error;
    private final EmbeddingStoreRequestContext<Embedded> requestContext;
    private final Map<Object, Object> attributes;

    /**
     * 构造异常上下文。
     *
     * @param error          异常
     * @param requestContext 请求上下文
     * @param attributes     附加属性
     */
    public EmbeddingStoreErrorContext(Throwable error, EmbeddingStoreRequestContext<Embedded> requestContext,
                                      Map<Object, Object> attributes) {
        this.error = error;
        this.requestContext = requestContext;
        this.attributes = attributes == null
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<Object, Object>(attributes));
    }

    /**
     * 异常。
     *
     * @return 异常
     */
    public Throwable error() {
        return error;
    }

    /**
     * 向量库。
     *
     * @return 向量库
     */
    public EmbeddingStore<Embedded> embeddingStore() {
        return requestContext == null ? null : requestContext.embeddingStore();
    }

    /**
     * 请求上下文。
     *
     * @return 请求上下文
     */
    public EmbeddingStoreRequestContext<Embedded> requestContext() {
        return requestContext;
    }

    /**
     * 附加属性。
     *
     * @return 只读属性
     */
    public Map<Object, Object> attributes() {
        return attributes;
    }
}
