package com.xiongx9527.fw.plus.ai.spring.adapter;

import com.xiongx9527.fw.plus.ai.model.AiEmbeddingModel;
import com.xiongx9527.fw.plus.ai.model.AiEmbeddingRequest;
import com.xiongx9527.fw.plus.ai.model.AiEmbeddingResult;
import com.xiongx9527.fw.plus.ai.model.AiModelDefinition;
import com.xiongx9527.fw.plus.ai.model.AiModelUsage;
import org.springframework.ai.chat.metadata.EmptyUsage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;

import java.util.ArrayList;
import java.util.List;

final class SpringAiEmbeddingModel extends SpringAiModel implements AiEmbeddingModel {

    private final EmbeddingModel delegate;

    SpringAiEmbeddingModel(AiModelDefinition definition, EmbeddingModel delegate) {
        super(definition);
        this.delegate = delegate;
    }

    @Override
    public AiEmbeddingResult embed(AiEmbeddingRequest request) {
        if (request == null) {
            throw requestRejected("request must not be null");
        }
        if (!request.getParameters().isEmpty()) {
            throw requestRejected("per-request parameters are not supported");
        }
        return execute(request.getTimeoutMillis(), () -> toResult(request,
                delegate.call(new EmbeddingRequest(request.getInputs(), null))));
    }

    private AiEmbeddingResult toResult(AiEmbeddingRequest request, EmbeddingResponse response) {
        if (response == null || response.getResults() == null
                || response.getResults().size() != request.getInputs().size()) {
            throw requestRejected("returned an invalid embedding response");
        }
        float[][] ordered = new float[response.getResults().size()][];
        for (Embedding embedding : response.getResults()) {
            if (embedding == null || embedding.getIndex() == null || embedding.getOutput() == null
                    || embedding.getOutput().length == 0) {
                throw requestRejected("returned an invalid embedding vector");
            }
            int index = embedding.getIndex().intValue();
            if (index < 0 || index >= ordered.length || ordered[index] != null) {
                throw requestRejected("returned an invalid embedding index");
            }
            ordered[index] = embedding.getOutput().clone();
        }
        List<float[]> embeddings = new ArrayList<>(ordered.length);
        for (float[] embedding : ordered) {
            if (embedding == null) {
                throw requestRejected("returned an incomplete embedding response");
            }
            embeddings.add(embedding);
        }
        Usage usage = response.getMetadata() == null ? null : response.getMetadata().getUsage();
        return new AiEmbeddingResult(embeddings, getKey(), usage(usage));
    }

    private AiModelUsage usage(Usage usage) {
        if (usage == null || usage instanceof EmptyUsage || usage.getPromptTokens() == null
                || usage.getCompletionTokens() == null
                || usage.getTotalTokens() == null) {
            return AiModelUsage.unknown();
        }
        return new AiModelUsage(usage.getPromptTokens().longValue(), usage.getCompletionTokens().longValue(),
                usage.getTotalTokens().longValue());
    }
}
