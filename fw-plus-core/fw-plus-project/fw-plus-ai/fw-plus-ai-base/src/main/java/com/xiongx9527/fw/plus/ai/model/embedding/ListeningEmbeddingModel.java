package com.xiongx9527.fw.plus.ai.model.embedding;

import com.xiongx9527.fw.plus.ai.data.embedding.Embedding;
import com.xiongx9527.fw.plus.ai.data.segment.TextSegment;
import com.xiongx9527.fw.plus.ai.model.ModelProvider;
import com.xiongx9527.fw.plus.ai.message.ContentType;
import com.xiongx9527.fw.plus.ai.model.embedding.listener.EmbeddingModelErrorContext;
import com.xiongx9527.fw.plus.ai.model.embedding.listener.EmbeddingModelListener;
import com.xiongx9527.fw.plus.ai.model.embedding.listener.EmbeddingModelRequestContext;
import com.xiongx9527.fw.plus.ai.model.embedding.listener.EmbeddingModelResponseContext;
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
 * 带监听器的嵌入模型包装器（包内可见）。
 *
 * <p><b>作用</b>：把 {@link EmbeddingModelListener} 织入到模型调用上，
 * 让所有模型实现共享同一套监听派发逻辑。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。镜像
 * {@code dev.langchain4j.model.embedding.ListeningEmbeddingModel}。</p>
 *
 * <p><b>实现约定</b>：监听器异常由 {@link EmbeddingModelListenerUtils} 吞掉，不影响主链路。</p>
 *
 * @author xiongxiao
 */
final class ListeningEmbeddingModel implements EmbeddingModel {

    private final EmbeddingModel delegate;
    private final List<EmbeddingModelListener> listeners;

    /**
     * 构造包装器。
     *
     * @param delegate  被包装的模型
     * @param listeners 监听器列表
     */
    ListeningEmbeddingModel(EmbeddingModel delegate, List<EmbeddingModelListener> listeners) {
        this.delegate = delegate;
        this.listeners = new ArrayList<EmbeddingModelListener>(listeners);
    }

    @Override
    public List<EmbeddingModelListener> listeners() {
        return Collections.unmodifiableList(listeners);
    }

    @Override
    public EmbeddingResponse embed(EmbeddingRequest request) {
        List<TextSegment> textSegments = EmbeddingModel.textSegmentsOf(request);
        EmbeddingModelRequestContext requestContext = EmbeddingModelRequestContext.builder()
                .embeddingRequest(request)
                .embeddingModel(delegate)
                .textSegments(textSegments)
                .build();
        EmbeddingModelListenerUtils.onRequest(requestContext, listeners);
        try {
            EmbeddingResponse response = delegate.embed(request);
            EmbeddingModelListenerUtils.onResponse(EmbeddingModelResponseContext.builder()
                    .embeddingRequest(request)
                    .embeddingResponse(response)
                    .embeddingModel(delegate)
                    .textSegments(textSegments)
                    .build(), listeners);
            return response;
        } catch (RuntimeException e) {
            EmbeddingModelListenerUtils.onError(EmbeddingModelErrorContext.builder()
                    .error(e)
                    .embeddingRequest(request)
                    .embeddingModel(delegate)
                    .textSegments(textSegments)
                    .build(), listeners);
            throw e;
        }
    }

    @Override
    public CompletableFuture<EmbeddingResponse> embedAsync(EmbeddingRequest request) {
        return delegate.embedAsync(request);
    }

    @Override
    public EmbeddingResponse doEmbed(EmbeddingRequest request) {
        return delegate.doEmbed(request);
    }

    @Override
    public CompletableFuture<EmbeddingResponse> doEmbedAsync(EmbeddingRequest request) {
        return delegate.doEmbedAsync(request);
    }

    @Override
    public EmbeddingRequestParameters defaultRequestParameters() {
        return delegate.defaultRequestParameters();
    }

    @Override
    public Set<EmbeddingParameter<?>> supportedParameters() {
        return delegate.supportedParameters();
    }

    @Override
    public Set<ContentType> supportedContentTypes() {
        return delegate.supportedContentTypes();
    }

    @Override
    public Response<Embedding> embed(String text) {
        return delegate.embed(text);
    }

    @Override
    public Response<Embedding> embed(TextSegment textSegment) {
        return delegate.embed(textSegment);
    }

    @Override
    public Response<List<Embedding>> embedAll(List<TextSegment> textSegments) {
        return delegate.embedAll(textSegments);
    }

    @Override
    public int dimension() {
        return delegate.dimension();
    }

    @Override
    public String modelName() {
        return delegate.modelName();
    }

    @Override
    public ModelProvider provider() {
        return delegate.provider();
    }

    @Override
    public EmbeddingModel addListeners(List<EmbeddingModelListener> additional) {
        if (additional == null || additional.isEmpty()) {
            return this;
        }
        List<EmbeddingModelListener> merged = new ArrayList<EmbeddingModelListener>(listeners);
        merged.addAll(additional);
        return new ListeningEmbeddingModel(delegate, merged);
    }
}
