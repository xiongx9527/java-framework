package com.xiongx9527.fw.plus.ai.langchain4j.adapter;

import com.xiongx9527.fw.plus.ai.model.AiEmbeddingModel;
import com.xiongx9527.fw.plus.ai.model.AiEmbeddingRequest;
import com.xiongx9527.fw.plus.ai.model.AiEmbeddingResult;
import com.xiongx9527.fw.plus.ai.model.AiModelDefinition;
import com.xiongx9527.fw.plus.ai.model.AiModelUsage;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.output.TokenUsage;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

public final class LangChain4jAiEmbeddingModel extends LangChain4jAiModel implements AiEmbeddingModel {

	private final EmbeddingModel delegate;

	LangChain4jAiEmbeddingModel(AiModelDefinition definition, LangChain4jModel nativeModel) {
		super(definition, nativeModel);
		this.delegate = (EmbeddingModel) nativeModel.getModel();
	}

	@Override
	public AiEmbeddingResult embed(final AiEmbeddingRequest request) {
		if (request == null) {
			throw requestRejected("received a null embedding request");
		}
		if (!request.getParameters().isEmpty()) {
			throw requestRejected("does not support per-request embedding parameters in LangChain4j 0.35");
		}
		return execute(request.getTimeoutMillis(), new Callable<AiEmbeddingResult>() {
			@Override
			public AiEmbeddingResult call() {
				List<float[]> embeddings = new ArrayList<float[]>(request.getInputs().size());
				UsageAccumulator usage = new UsageAccumulator();
				for (String input : request.getInputs()) {
					Response<Embedding> response = delegate.embed(input);
					if (response == null || response.content() == null || response.content().vector() == null) {
						throw requestRejected("returned no embedding vector");
					}
					embeddings.add(response.content().vector());
					usage.add(response.tokenUsage());
				}
				return new AiEmbeddingResult(embeddings, getKey(), usage.result());
			}
		});
	}

	private static final class UsageAccumulator {

		private boolean known = true;
		private long inputTokens;
		private long outputTokens;
		private long totalTokens;

		private void add(TokenUsage usage) {
			if (usage == null || usage.inputTokenCount() == null || usage.outputTokenCount() == null
					|| usage.totalTokenCount() == null) {
				known = false;
				return;
			}
			inputTokens += usage.inputTokenCount().longValue();
			outputTokens += usage.outputTokenCount().longValue();
			totalTokens += usage.totalTokenCount().longValue();
		}

		private AiModelUsage result() {
			return known ? new AiModelUsage(inputTokens, outputTokens, totalTokens) : AiModelUsage.unknown();
		}
	}
}
