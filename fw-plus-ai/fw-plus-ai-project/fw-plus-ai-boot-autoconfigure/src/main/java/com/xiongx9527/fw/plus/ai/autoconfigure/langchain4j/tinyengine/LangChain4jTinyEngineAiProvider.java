package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.tinyengine;

import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.TinyEngineAiProperties;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.chat.TinyEngineChatMessage;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.chat.TinyEngineChatRequest;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.chat.TinyEngineChatResponse;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.chat.TinyEngineChatStreamChunk;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.official.TinyEngineUpstreamException;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.provider.TinyEngineAiCapability;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.provider.TinyEngineAiProvider;
import com.xiongx9527.fw.plus.ai.langchain4j.adapter.LangChain4jAiModel;
import com.xiongx9527.fw.plus.ai.langchain4j.adapter.LangChain4jChatModelAdapter;
import com.xiongx9527.fw.plus.ai.langchain4j.adapter.LangChain4jModel;
import com.xiongx9527.fw.plus.ai.model.AiModel;
import com.xiongx9527.fw.plus.ai.model.AiModelRegistry;
import com.xiongx9527.fw.plus.ai.model.AiModelType;
import com.xiongx9527.fw.plus.tinyengine.support.ai.OpenAiCompatibleBaseUrl;
import dev.langchain4j.exception.HttpException;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ResponseFormat;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;

import java.util.ArrayList;
import java.util.Collections;
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
		boolean dynamic = hasInvocationCredentials(request);
		Object nativeModel = dynamic ? chatModel(request) : resolveNativeModel(request, AiModelType.CHAT).getModel();
		ChatModel model = LangChain4jChatModelAdapter.asChatModel(nativeModel);
		if (model == null) {
			throw new IllegalStateException("TinyEngine AI provider 'langchain4j' native CHAT model is not ChatModel");
		}
		ChatResponse response;
		try {
			response = model.chat(chatRequest(request));
		} catch (RuntimeException ex) {
			throw normalizeFailure(ex, dynamic);
		}
		AiMessage message = response == null ? null : response.aiMessage();
		Map<String, Object> metadata = requestMetadata(request);
		if (response != null && response.finishReason() != null) {
			metadata.put("finish_reason", response.finishReason().name());
		}
		return new TinyEngineChatResponse(message == null ? null : message.text(), metadata);
	}

	@Override
	public List<TinyEngineChatStreamChunk> stream(TinyEngineChatRequest request) {
		boolean dynamic = hasInvocationCredentials(request);
		Object nativeModel = dynamic ? streamingChatModel(request)
				: resolveNativeModel(request, AiModelType.STREAMING_CHAT).getModel();
		StreamingChatModel model = LangChain4jChatModelAdapter.asStreamingChatModel(nativeModel);
		if (model == null) {
			throw new IllegalStateException("TinyEngine AI provider 'langchain4j' native STREAMING_CHAT model is not StreamingChatModel");
		}
		final List<TinyEngineChatStreamChunk> chunks = new ArrayList<TinyEngineChatStreamChunk>();
		final RuntimeException[] error = new RuntimeException[1];
		final CountDownLatch completed = new CountDownLatch(1);
		try {
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
					error[0] = normalizeFailure(throwable, dynamic);
					completed.countDown();
				}
			});
		} catch (RuntimeException ex) {
			throw normalizeFailure(ex, dynamic);
		}
		try {
			awaitCompletion(completed);
		} catch (RuntimeException ex) {
			throw normalizeFailure(ex, dynamic);
		}
		if (error[0] != null) {
			throw error[0];
		}
		return chunks;
	}

	private Object chatModel(TinyEngineChatRequest request) {
		if (hasInvocationCredentials(request)) {
			dev.langchain4j.model.openai.OpenAiChatModel.OpenAiChatModelBuilder builder =
					dev.langchain4j.model.openai.OpenAiChatModel.builder()
							.baseUrl(OpenAiCompatibleBaseUrl.normalize(request.getBaseUrl()))
							.apiKey(request.getApiKey()).modelName(request.getModel()).maxRetries(Integer.valueOf(0));
			applyModelSettings(builder, request);
			return builder.build();
		}
		return resolveNativeModel(request, AiModelType.CHAT).getModel();
	}

	private Object streamingChatModel(TinyEngineChatRequest request) {
		if (hasInvocationCredentials(request)) {
			dev.langchain4j.model.openai.OpenAiStreamingChatModel.OpenAiStreamingChatModelBuilder builder =
					dev.langchain4j.model.openai.OpenAiStreamingChatModel.builder()
							.baseUrl(OpenAiCompatibleBaseUrl.normalize(request.getBaseUrl()))
							.apiKey(request.getApiKey()).modelName(request.getModel());
			applyModelSettings(builder, request);
			return builder.build();
		}
		return resolveNativeModel(request, AiModelType.STREAMING_CHAT).getModel();
	}

	private void applyModelSettings(dev.langchain4j.model.openai.OpenAiChatModel.OpenAiChatModelBuilder builder,
			TinyEngineChatRequest request) {
		Map<String, Object> settings = modelSettings(request);
		builder.temperature(firstDouble(request.getTemperature(), settings, "temperature"));
		builder.topP(firstDouble(request.getTopP(), settings, "topP", "top_p"));
		builder.maxTokens(firstInteger(request.getMaxTokens(), settings, "maxTokens", "max_tokens"));
		builder.presencePenalty(firstDouble(request.getPresencePenalty(), settings, "presencePenalty", "presence_penalty"));
		builder.frequencyPenalty(firstDouble(request.getFrequencyPenalty(), settings, "frequencyPenalty", "frequency_penalty"));
		builder.responseFormat(firstString(request.getResponseFormat(), settings, "responseFormat", "response_format"));
		builder.parallelToolCalls(firstBoolean(request.getParallelToolCalls(), settings, "parallelToolCalls", "parallel_tool_calls"));
	}

	private void applyModelSettings(
			dev.langchain4j.model.openai.OpenAiStreamingChatModel.OpenAiStreamingChatModelBuilder builder,
			TinyEngineChatRequest request) {
		Map<String, Object> settings = modelSettings(request);
		builder.temperature(firstDouble(request.getTemperature(), settings, "temperature"));
		builder.topP(firstDouble(request.getTopP(), settings, "topP", "top_p"));
		builder.maxTokens(firstInteger(request.getMaxTokens(), settings, "maxTokens", "max_tokens"));
		builder.presencePenalty(firstDouble(request.getPresencePenalty(), settings, "presencePenalty", "presence_penalty"));
		builder.frequencyPenalty(firstDouble(request.getFrequencyPenalty(), settings, "frequencyPenalty", "frequency_penalty"));
		builder.responseFormat(firstString(request.getResponseFormat(), settings, "responseFormat", "response_format"));
		builder.parallelToolCalls(firstBoolean(request.getParallelToolCalls(), settings, "parallelToolCalls", "parallel_tool_calls"));
	}

	private Map<String, Object> modelSettings(TinyEngineChatRequest request) {
		return request == null || request.getModelSettings() == null
				? Collections.<String, Object>emptyMap() : request.getModelSettings();
	}

	private Double firstDouble(Double requestValue, Map<String, Object> settings, String... keys) {
		if (requestValue != null) return requestValue;
		Object value = firstSetting(settings, keys);
		if (value instanceof Number) return Double.valueOf(((Number) value).doubleValue());
		return value == null ? null : Double.valueOf(String.valueOf(value));
	}

	private Integer firstInteger(Integer requestValue, Map<String, Object> settings, String... keys) {
		if (requestValue != null) return requestValue;
		Object value = firstSetting(settings, keys);
		if (value instanceof Number) return Integer.valueOf(((Number) value).intValue());
		return value == null ? null : Integer.valueOf(String.valueOf(value));
	}

	private Boolean firstBoolean(Boolean requestValue, Map<String, Object> settings, String... keys) {
		if (requestValue != null) return requestValue;
		Object value = firstSetting(settings, keys);
		return value == null ? null : value instanceof Boolean ? (Boolean) value : Boolean.valueOf(String.valueOf(value));
	}

	private String firstString(String requestValue, Map<String, Object> settings, String... keys) {
		if (!isBlank(requestValue)) return requestValue;
		Object value = firstSetting(settings, keys);
		if (value instanceof Map) {
			Object type = ((Map<?, ?>) value).get("type");
			return type == null ? null : String.valueOf(type);
		}
		return value == null ? null : String.valueOf(value);
	}

	private Object firstSetting(Map<String, Object> settings, String... keys) {
		if (settings == null || keys == null) return null;
		for (String key : keys) {
			if (settings.containsKey(key)) return settings.get(key);
		}
		return null;
	}

	private boolean hasInvocationCredentials(TinyEngineChatRequest request) {
		if (request == null || isBlank(request.getBaseUrl()) || isBlank(request.getModel())) {
			return false;
		}
		if (!"openai".equalsIgnoreCase(request.getProvider())) {
			throw new IllegalStateException("TinyEngine AI provider 'langchain4j' does not support invocation provider");
		}
		return true;
	}

	private RuntimeException normalizeFailure(Throwable throwable, boolean dynamic) {
		if (!dynamic && !isInfrastructureFailure(throwable)) {
			return throwable instanceof RuntimeException ? (RuntimeException) throwable
					: new IllegalStateException("TinyEngine AI invocation failed", throwable);
		}
		return normalizeUpstreamFailure(throwable);
	}

	private boolean isInfrastructureFailure(Throwable throwable) {
		Throwable current = throwable;
		while (current != null) {
			if (current instanceof java.io.IOException || current instanceof java.util.concurrent.TimeoutException) {
				return true;
			}
			current = current.getCause();
		}
		String message = throwable == null ? null : throwable.getMessage();
		return message != null && (message.toLowerCase(java.util.Locale.ENGLISH).contains("connection refused")
				|| message.toLowerCase(java.util.Locale.ENGLISH).contains("connection reset"));
	}

	private RuntimeException normalizeUpstreamFailure(Throwable throwable) {
		if (throwable instanceof TinyEngineUpstreamException) {
			return (TinyEngineUpstreamException) throwable;
		}
		HttpException upstream = findHttpException(throwable);
		if (upstream != null) {
			return upstreamFailure(upstream);
		}
		return new TinyEngineUpstreamException(502, "UPSTREAM_ERROR",
				"TinyEngine AI upstream request failed");
	}

	private HttpException findHttpException(Throwable throwable) {
		Throwable current = throwable;
		while (current != null) {
			if (current instanceof HttpException) {
				return (HttpException) current;
			}
			current = current.getCause();
		}
		return null;
	}

	private TinyEngineUpstreamException upstreamFailure(HttpException ex) {
		int statusCode = ex.statusCode();
		return new TinyEngineUpstreamException(statusCode,
				statusCode == 401 || statusCode == 403 ? "INVALID_API_KEY" : "UPSTREAM_ERROR",
				statusCode == 401 || statusCode == 403
						? "TinyEngine AI upstream credential rejected" : "TinyEngine AI upstream request failed");
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
