package com.xiongx9527.fw.plus.ai.rag;

import java.util.concurrent.CompletableFuture;

/**
 * 检索增强端口：对一条消息做「检索 + 注入」，返回增强后的消息。
 *
 * <p><b>作用</b>：RAG 的总入口。实现内部通常串起 QueryTransformer → QueryRouter →
 * ContentRetriever → ContentAggregator → ContentInjector。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。镜像 {@code dev.langchain4j.rag.RetrievalAugmentor}
 * （{@code augment} + 默认 {@code augmentAsync}）；Spring AI 等价 Advisor。</p>
 *
 * <p><b>实现约定</b>：不修改入参；无可用内容时应原样返回入参消息。</p>
 *
 * @author xiongxiao
 */
public interface RetrievalAugmentor {

    /**
     * 执行检索增强。
     *
     * @param request 增强请求
     * @return 增强结果，永不为 {@code null}
     */
    AugmentationResult augment(AugmentationRequest request);

    /**
     * 异步执行检索增强（默认同步包装）。
     *
     * @param request 增强请求
     * @return 异步结果
     */
    default CompletableFuture<AugmentationResult> augmentAsync(AugmentationRequest request) {
        return CompletableFuture.completedFuture(augment(request));
    }
}
