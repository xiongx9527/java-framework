package com.xiongx9527.fw.plus.ai.model.embedding;

import com.xiongx9527.fw.plus.ai.data.embedding.Embedding;
import com.xiongx9527.fw.plus.ai.data.segment.TextSegment;
import com.xiongx9527.fw.plus.ai.exception.UnsupportedFeatureException;
import com.xiongx9527.fw.plus.ai.message.ContentType;
import com.xiongx9527.fw.plus.ai.model.ModelProvider;
import com.xiongx9527.fw.plus.ai.model.embedding.listener.EmbeddingModelErrorContext;
import com.xiongx9527.fw.plus.ai.model.embedding.listener.EmbeddingModelListener;
import com.xiongx9527.fw.plus.ai.model.embedding.listener.EmbeddingModelRequestContext;
import com.xiongx9527.fw.plus.ai.model.embedding.listener.EmbeddingModelResponseContext;
import com.xiongx9527.fw.plus.ai.model.embedding.request.EmbeddingInput;
import com.xiongx9527.fw.plus.ai.model.embedding.request.EmbeddingParameter;
import com.xiongx9527.fw.plus.ai.model.embedding.request.EmbeddingRequest;
import com.xiongx9527.fw.plus.ai.model.embedding.request.EmbeddingRequestParameters;
import com.xiongx9527.fw.plus.ai.model.embedding.response.EmbeddingResponse;
import com.xiongx9527.fw.plus.ai.model.output.Response;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * 向量化模型端口。
 *
 * <p><b>作用</b>：把文本（或多模态内容）映射为稠密向量，是 RAG 入库与检索的共同前置步骤。
 * 上层只依赖本接口，换模型（OpenAI / bge / 通义 / Ollama…）不改调用方代码。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。逐方法镜像
 * {@code dev.langchain4j.model.embedding.EmbeddingModel}。Spring AI 等价
 * {@code org.springframework.ai.embedding.EmbeddingModel}。</p>
 *
 * <p><b>实现约定</b>：
 * <ul>
 *   <li>实现者通常只需覆写 {@link #doEmbed(EmbeddingRequest)}；{@link #embed(EmbeddingRequest)}
 *       已负责监听器派发与异常回调，不要重复实现。</li>
 *   <li>{@link #dimension()} 无法推断时抛 {@link UnsupportedFeatureException}；
 *       已知维度请继承 {@link DimensionAwareEmbeddingModel}。</li>
 *   <li>{@link #listeners()} 返回不可变列表，追加监听器用 {@link #addListener} /
 *       {@link #addListeners}，返回新实例。</li>
 * </ul>
 *
 * @author xiongxiao
 */
public interface EmbeddingModel {

    /**
     * 执行嵌入请求（含监听器派发）。
     *
     * @param request 嵌入请求
     * @return 嵌入响应
     */
    default EmbeddingResponse embed(EmbeddingRequest request) {
        List<EmbeddingModelListener> listeners = listeners();
        EmbeddingModelRequestContext requestContext = EmbeddingModelRequestContext.builder()
                .embeddingRequest(request)
                .embeddingModel(this)
                .textSegments(textSegmentsOf(request))
                .build();
        EmbeddingModelListenerUtils.onRequest(requestContext, listeners);
        try {
            EmbeddingResponse response = doEmbed(request);
            EmbeddingModelListenerUtils.onResponse(EmbeddingModelResponseContext.builder()
                    .embeddingRequest(request)
                    .embeddingResponse(response)
                    .embeddingModel(this)
                    .textSegments(textSegmentsOf(request))
                    .build(), listeners);
            return response;
        } catch (RuntimeException e) {
            EmbeddingModelListenerUtils.onError(EmbeddingModelErrorContext.builder()
                    .error(e)
                    .embeddingRequest(request)
                    .embeddingModel(this)
                    .textSegments(textSegmentsOf(request))
                    .build(), listeners);
            throw e;
        }
    }

    /**
     * 异步执行嵌入请求。
     *
     * @param request 嵌入请求
     * @return 异步响应；实现不支持时返回失败的 Future
     */
    default CompletableFuture<EmbeddingResponse> embedAsync(EmbeddingRequest request) {
        try {
            return CompletableFuture.completedFuture(embed(request));
        } catch (RuntimeException e) {
            CompletableFuture<EmbeddingResponse> failed = new CompletableFuture<EmbeddingResponse>();
            failed.completeExceptionally(e);
            return failed;
        }
    }

    /**
     * 已注册的监听器。
     *
     * @return 不可变监听器列表，默认空
     */
    default List<EmbeddingModelListener> listeners() {
        return Collections.emptyList();
    }

    /**
     * 厂商标识。
     *
     * @return 厂商标识；未声明时为 {@code null}
     */
    default ModelProvider provider() {
        return null;
    }

    /**
     * 真正执行嵌入（由实现覆写，不含监听器派发）。
     *
     * @param request 嵌入请求
     * @return 嵌入响应
     */
    default EmbeddingResponse doEmbed(EmbeddingRequest request) {
        throw new UnsupportedFeatureException("Not supported yet.");
    }

    /**
     * 真正执行异步嵌入（由实现覆写）。
     *
     * @param request 嵌入请求
     * @return 异步响应
     */
    default CompletableFuture<EmbeddingResponse> doEmbedAsync(EmbeddingRequest request) {
        CompletableFuture<EmbeddingResponse> failed = new CompletableFuture<EmbeddingResponse>();
        failed.completeExceptionally(new UnsupportedFeatureException(
                getClass().getName() + " does not support doEmbedAsync() yet."));
        return failed;
    }

    /**
     * 默认请求参数。
     *
     * @return 默认参数，非 {@code null}
     */
    default EmbeddingRequestParameters defaultRequestParameters() {
        return EmbeddingRequestParameters.EMPTY;
    }

    /**
     * 支持的参数集合。
     *
     * @return 参数集合，默认空
     */
    default Set<EmbeddingParameter<?>> supportedParameters() {
        return Collections.emptySet();
    }

    /**
     * 支持的内容类型。
     *
     * @return 内容类型集合，默认空（表示仅支持纯文本）
     */
    default Set<ContentType> supportedContentTypes() {
        return Collections.emptySet();
    }

    /**
     * 嵌入单段文本。
     *
     * @param text 文本
     * @return 向量响应
     */
    default Response<Embedding> embed(String text) {
        return embed(TextSegment.from(text));
    }

    /**
     * 嵌入单个文本片段。
     *
     * @param textSegment 文本片段
     * @return 向量响应
     */
    default Response<Embedding> embed(TextSegment textSegment) {
        EmbeddingResponse response = embed(EmbeddingRequest.builder().textSegment(textSegment).build());
        List<Embedding> embeddings = response.embeddings();
        return Response.from(embeddings.isEmpty() ? null : embeddings.get(0), response.tokenUsage());
    }

    /**
     * 批量嵌入文本片段。
     *
     * @param textSegments 文本片段列表
     * @return 向量列表响应，顺序与入参一致
     */
    default Response<List<Embedding>> embedAll(List<TextSegment> textSegments) {
        EmbeddingResponse response = embed(EmbeddingRequest.builder().textSegments(textSegments).build());
        return Response.from(response.embeddings(), response.tokenUsage());
    }

    /**
     * 向量维度。
     *
     * @return 维度
     */
    default int dimension() {
        throw new UnsupportedFeatureException(getClass().getName() + " does not support dimension() yet.");
    }

    /**
     * 模型名。
     *
     * @return 模型名；未知时为 {@code null}
     */
    default String modelName() {
        return null;
    }

    /**
     * 追加一个监听器。
     *
     * @param listener 监听器，可为 {@code null}
     * @return 带该监听器的新模型实例
     */
    default EmbeddingModel addListener(EmbeddingModelListener listener) {
        return addListeners(listener == null ? null : Collections.singletonList(listener));
    }

    /**
     * 追加一组监听器。
     *
     * @param listeners 监听器列表，为空时返回 {@code this}
     * @return 带这些监听器的新模型实例
     */
    default EmbeddingModel addListeners(List<EmbeddingModelListener> listeners) {
        if (listeners == null || listeners.isEmpty()) {
            return this;
        }
        List<EmbeddingModelListener> merged = new ArrayList<EmbeddingModelListener>(listeners());
        merged.addAll(listeners);
        return new ListeningEmbeddingModel(this, merged);
    }

    /**
     * 从请求中提取纯文本视图，供监听器使用。
     *
     * @param request 嵌入请求
     * @return 文本片段列表，无法提取时为空列表
     */
    static List<TextSegment> textSegmentsOf(EmbeddingRequest request) {
        if (request == null || request.inputs() == null) {
            return Collections.emptyList();
        }
        List<TextSegment> segments = new ArrayList<TextSegment>(request.inputs().size());
        for (EmbeddingInput input : request.inputs()) {
            String text = input.text();
            if (text != null) {
                segments.add(TextSegment.from(text));
            }
        }
        return segments;
    }
}
