package com.xiongx9527.fw.plus.ai.langchain4j.adapter;

import com.xiongx9527.fw.plus.ai.model.AiChatModel;
import com.xiongx9527.fw.plus.ai.model.AiChatRequest;
import com.xiongx9527.fw.plus.ai.model.AiChatResult;
import com.xiongx9527.fw.plus.ai.model.AiModelDefinition;
import com.xiongx9527.fw.plus.ai.model.AiModelUsage;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.TokenUsage;

import java.util.Collections;
import java.util.concurrent.Callable;

public final class LangChain4jAiChatModel extends LangChain4jAiModel implements AiChatModel {

	private final ChatModel delegate;

	LangChain4jAiChatModel(AiModelDefinition definition, LangChain4jModel nativeModel) {
		super(definition, nativeModel);
		this.delegate = (ChatModel) nativeModel.getModel();
	}

	@Override
	public AiChatResult chat(final AiChatRequest request) {
		if (request == null) {
			throw requestRejected("received a null chat request");
		}
		if (!request.getParameters().isEmpty()) {
			throw requestRejected("does not support per-request chat parameters");
		}
		return execute(request.getTimeoutMillis(), new Callable<AiChatResult>() {
			@Override
			public AiChatResult call() {
				ChatRequest nativeRequest = ChatRequest.builder()
						.messages(Collections.singletonList(UserMessage.from(request.getInput())))
						.build();
				ChatResponse response = delegate.chat(nativeRequest);
				AiMessage message = response == null ? null : response.aiMessage();
				if (message == null || message.text() == null) {
					throw requestRejected("returned no chat text");
				}
				String finishReason = response.finishReason() == null ? null : response.finishReason().name();
				return new AiChatResult(message.text(), getKey(), finishReason, usage(response.tokenUsage()));
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
