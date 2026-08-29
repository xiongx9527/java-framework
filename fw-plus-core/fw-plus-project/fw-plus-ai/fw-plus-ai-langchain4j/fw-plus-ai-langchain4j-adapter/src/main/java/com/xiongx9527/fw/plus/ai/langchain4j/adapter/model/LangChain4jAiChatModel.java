package com.xiongx9527.fw.plus.ai.langchain4j.adapter.model;

import com.xiongx9527.fw.plus.ai.model.AiChatModel;
import com.xiongx9527.fw.plus.ai.model.AiChatRequest;
import com.xiongx9527.fw.plus.ai.model.AiChatResult;
import com.xiongx9527.fw.plus.ai.model.AiModelDefinition;
import com.xiongx9527.fw.plus.ai.model.AiModelUsage;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.output.TokenUsage;

import java.util.Collections;
import java.util.concurrent.Callable;

public final class LangChain4jAiChatModel extends LangChain4jAiModel implements AiChatModel {

	private final ChatLanguageModel delegate;

	LangChain4jAiChatModel(AiModelDefinition definition, LangChain4jModel nativeModel) {
		super(definition, nativeModel);
		this.delegate = (ChatLanguageModel) nativeModel.getModel();
	}

	@Override
	public AiChatResult chat(final AiChatRequest request) {
		if (request == null) {
			throw requestRejected("received a null chat request");
		}
		if (!request.getParameters().isEmpty()) {
			throw requestRejected("does not support per-request chat parameters in LangChain4j 0.35");
		}
		return execute(request.getTimeoutMillis(), new Callable<AiChatResult>() {
			@Override
			public AiChatResult call() {
				Response<AiMessage> response = delegate.generate(Collections.<ChatMessage>singletonList(
						UserMessage.from(request.getInput())));
				if (response == null || response.content() == null || response.content().text() == null) {
					throw requestRejected("returned no chat text");
				}
				String finishReason = response.finishReason() == null ? null : response.finishReason().name();
				return new AiChatResult(response.content().text(), getKey(), finishReason, usage(response.tokenUsage()));
			}
		});
	}

	private AiModelUsage usage(TokenUsage usage) {
		if (usage == null || usage.inputTokenCount() == null || usage.outputTokenCount() == null
				|| usage.totalTokenCount() == null) {
			return AiModelUsage.unknown();
		}
		return new AiModelUsage(usage.inputTokenCount().longValue(), usage.outputTokenCount().longValue(),
				usage.totalTokenCount().longValue());
	}
}
