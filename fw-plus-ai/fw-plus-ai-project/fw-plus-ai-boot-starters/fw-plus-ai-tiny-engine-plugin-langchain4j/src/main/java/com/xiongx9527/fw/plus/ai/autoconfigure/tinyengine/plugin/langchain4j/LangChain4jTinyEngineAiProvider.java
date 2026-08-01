package com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.plugin.langchain4j;

import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.TinyEngineAiProperties;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.chat.TinyEngineChatMessage;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.chat.TinyEngineChatRequest;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.chat.TinyEngineChatResponse;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.chat.TinyEngineChatStreamChunk;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.provider.TinyEngineAiCapability;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.provider.TinyEngineAiProvider;
import com.xiongx9527.fw.plus.ai.langchain4j.adapter.LangChain4jAiModel;
import com.xiongx9527.fw.plus.ai.langchain4j.adapter.LangChain4jChatModelAdapter;
import com.xiongx9527.fw.plus.ai.langchain4j.adapter.LangChain4jModel;
import com.xiongx9527.fw.plus.ai.model.AiModel;
import com.xiongx9527.fw.plus.ai.model.AiModelRegistry;
import com.xiongx9527.fw.plus.ai.model.AiModelType;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ResponseFormat;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.output.Response;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class LangChain4jTinyEngineAiProvider implements TinyEngineAiProvider {

	private final AiModelRegistry modelRegistry;
	private final TinyEngineAiProperties properties;

	public LangChain4jTinyEngineAiProvider(AiModelRegistry modelRegistry, TinyEngineAiProperties properties) {
		this.modelRegistry = modelRegistry;
		this.properties = properties;
	}

	@Override
	public String getKey() {
		return "langchain4j";
	}

	@Override
	public boolean supports(TinyEngineAiCapability capability) {
		return capability == TinyEngineAiCapability.CHAT || capability == TinyEngineAiCapability.STREAMING_CHAT;
	}

	@Override
	public TinyEngineChatResponse chat(TinyEngineChatRequest request) {
		LangChain4jModel nativeModel = resolveNativeModel(request, AiModelType.CHAT);
		ChatResponse response = chat(nativeModel.getModel(), request);
		AiMessage message = response == null ? null : response.aiMessage();
		Map<String, Object> metadata = requestMetadata(request);
		if (response != null && response.finishReason() != null) {
			metadata.put("finish_reason", response.finishReason().name());
		}
		return new TinyEngineChatResponse(message == null ? null : message.text(), metadata);
	}

	private ChatResponse chat(Object nativeModel, TinyEngineChatRequest request) {
		if (nativeModel instanceof ChatLanguageModel
				&& !overridesChatRequest((ChatLanguageModel) nativeModel)) {
			Response<AiMessage> response = ((ChatLanguageModel) nativeModel).generate(messages(request));
			return response == null ? null : ChatResponse.builder().aiMessage(response.content())
					.tokenUsage(response.tokenUsage()).finishReason(response.finishReason()).build();
		}
		ChatLanguageModel model = LangChain4jChatModelAdapter.asChatLanguageModel(nativeModel);
		if (model == null) {
			throw new IllegalStateException("TinyEngine AI provider 'langchain4j' native CHAT model is not ChatLanguageModel");
		}
		return model.chat(chatRequest(request));
	}

	private boolean overridesChatRequest(ChatLanguageModel model) {
		try {
			Method method = model.getClass().getMethod("chat", ChatRequest.class);
			return !ChatLanguageModel.class.equals(method.getDeclaringClass());
		} catch (NoSuchMethodException ex) {
			return false;
		}
	}

	@Override
	public List<TinyEngineChatStreamChunk> stream(TinyEngineChatRequest request) {
		LangChain4jModel nativeModel = resolveNativeModel(request, AiModelType.STREAMING_CHAT);
		StreamingChatLanguageModel model = LangChain4jChatModelAdapter
				.asStreamingChatLanguageModel(nativeModel.getModel());
		if (model == null) {
			throw new IllegalStateException("TinyEngine AI provider 'langchain4j' native STREAMING_CHAT model is not StreamingChatLanguageModel");
		}
		final List<TinyEngineChatStreamChunk> chunks = new ArrayList<TinyEngineChatStreamChunk>();
		final RuntimeException[] error = new RuntimeException[1];
		final CountDownLatch completed = new CountDownLatch(1);
		model.chat(chatRequest(request), new StreamingChatResponseHandler() {
			@Override
			public void onPartialResponse(String token) {
				chunks.add(new TinyEngineChatStreamChunk(token, false));
			}

			@Override
			public void onCompleteResponse(ChatResponse response) {
				AiMessage message = response == null ? null : response.aiMessage();
				chunks.add(new TinyEngineChatStreamChunk(message == null ? null : message.text(), true));
				completed.countDown();
			}

			@Override
			public void onError(Throwable throwable) {
				error[0] = throwable instanceof RuntimeException ? (RuntimeException) throwable
						: new IllegalStateException("TinyEngine AI streaming chat failed", throwable);
				completed.countDown();
			}
		});
		awaitCompletion(completed);
		if (error[0] != null) {
			throw error[0];
		}
		return chunks;
	}

	private ChatRequest chatRequest(TinyEngineChatRequest request) {
		ChatRequest.Builder builder = ChatRequest.builder().messages(messages(request));
		if (request != null) {
			if (request.getMaxTokens() != null) {
				builder.maxOutputTokens(request.getMaxTokens());
			}
			if (request.getPresencePenalty() != null) {
				builder.presencePenalty(request.getPresencePenalty());
			}
			if (request.getFrequencyPenalty() != null) {
				builder.frequencyPenalty(request.getFrequencyPenalty());
			}
		}
		ResponseFormat responseFormat = responseFormat(request);
		if (responseFormat != null) {
			builder.responseFormat(responseFormat);
		}
		return builder.build();
	}

	private List<ChatMessage> messages(TinyEngineChatRequest request) {
		List<ChatMessage> messages = new ArrayList<ChatMessage>();
		if (request != null && request.getMessages() != null && !request.getMessages().isEmpty()) {
			for (TinyEngineChatMessage message : request.getMessages()) {
				if (message != null && !isBlank(message.getContent())) {
					messages.add(message(message.getRole(), message.getContent()));
				}
			}
		}
		if (messages.isEmpty() && request != null && !isBlank(request.getMessage())) {
			messages.add(UserMessage.from(request.getMessage()));
		}
		if (messages.isEmpty()) {
			throw new IllegalStateException("TinyEngine AI chat request requires at least one message");
		}
		if (request != null && request.getTools() != null && !request.getTools().isEmpty()) {
			throw new IllegalStateException("TinyEngine AI provider 'langchain4j' does not support tool mapping yet");
		}
		return messages;
	}

	private ChatMessage message(String role, String content) {
		String normalizedRole = role == null ? "user" : role.trim().toLowerCase();
		if ("system".equals(normalizedRole)) {
			return SystemMessage.from(content);
		}
		if ("assistant".equals(normalizedRole) || "ai".equals(normalizedRole)) {
			return AiMessage.from(content);
		}
		if ("tool".equals(normalizedRole)) {
			return ToolExecutionResultMessage.from("tool", "tool", content);
		}
		return UserMessage.from(content);
	}

	private ResponseFormat responseFormat(TinyEngineChatRequest request) {
		if (request == null || isBlank(request.getResponseFormat())) {
			return null;
		}
		String responseFormat = request.getResponseFormat().trim().toLowerCase();
		if ("json".equals(responseFormat) || "json_object".equals(responseFormat)) {
			return ResponseFormat.JSON;
		}
		if ("text".equals(responseFormat)) {
			return ResponseFormat.TEXT;
		}
		throw new IllegalStateException("TinyEngine AI provider 'langchain4j' does not support response_format: "
				+ request.getResponseFormat());
	}

	private Map<String, Object> requestMetadata(TinyEngineChatRequest request) {
		Map<String, Object> metadata = new LinkedHashMap<String, Object>();
		if (request == null) {
			return metadata;
		}
		putIfNotNull(metadata, "response_format", request.getResponseFormat());
		putIfNotNull(metadata, "max_tokens", request.getMaxTokens());
		putIfNotNull(metadata, "presence_penalty", request.getPresencePenalty());
		putIfNotNull(metadata, "frequency_penalty", request.getFrequencyPenalty());
		putIfNotNull(metadata, "enable_thinking", request.getEnableThinking());
		putIfNotNull(metadata, "tool_choice", request.getToolChoice());
		putIfNotNull(metadata, "parallel_tool_calls", request.getParallelToolCalls());
		putIfNotNull(metadata, "enable_search", request.getEnableSearch());
		putIfNotNull(metadata, "stream_options", request.getStreamOptions());
		putIfNotNull(metadata, "search_options", request.getSearchOptions());
		return metadata;
	}

	private void putIfNotNull(Map<String, Object> metadata, String key, Object value) {
		if (value != null) {
			metadata.put(key, value);
		}
	}

	private void awaitCompletion(CountDownLatch completed) {
		try {
			if (!completed.await(30L, TimeUnit.SECONDS)) {
				throw new IllegalStateException("TinyEngine AI streaming chat timed out");
			}
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("TinyEngine AI streaming chat interrupted", ex);
		}
	}

	private LangChain4jModel resolveNativeModel(TinyEngineChatRequest request, AiModelType expectedType) {
		if (modelRegistry == null) {
			throw new IllegalStateException("TinyEngine AI provider 'langchain4j' requires AiModelRegistry");
		}
		String modelKey = resolveModelKey(request, expectedType);
		AiModel model = modelRegistry.get(modelKey);
		if (model == null) {
			throw new IllegalStateException("TinyEngine AI provider 'langchain4j' cannot resolve model: " + modelKey);
		}
		if (model.getType() != expectedType) {
			throw new IllegalStateException("TinyEngine AI provider 'langchain4j' requires model type '" + expectedType
					+ "' but model '" + modelKey + "' is '" + model.getType() + "'");
		}
		if (!(model instanceof LangChain4jAiModel)) {
			throw new IllegalStateException("TinyEngine AI provider 'langchain4j' does not support model framework/provider: "
					+ model.getFramework() + "/" + model.getProvider());
		}
		LangChain4jModel nativeModel = ((LangChain4jAiModel) model).getNativeModel();
		if (nativeModel == null || nativeModel.getModel() == null) {
			throw new IllegalStateException("TinyEngine AI provider 'langchain4j' cannot unwrap native model: " + modelKey);
		}
		return nativeModel;
	}

	private String resolveModelKey(TinyEngineChatRequest request, AiModelType type) {
		if (request != null && !isBlank(request.getModelKey())) {
			return request.getModelKey().trim();
		}
		String configuredKey = type == AiModelType.CHAT ? properties.getModel().getChatKey()
				: properties.getModel().getStreamingChatKey();
		if (!isBlank(configuredKey)) {
			return configuredKey.trim();
		}
		return modelRegistry.getDefaultKey();
	}

	private boolean isBlank(String value) {
		return value == null || value.trim().length() == 0;
	}
}
