package com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.provider;

import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.TinyEngineAiProperties;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.chat.TinyEngineChatRequest;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.chat.TinyEngineChatResponse;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.chat.TinyEngineChatStreamChunk;
import com.xiongx9527.fw.plus.ai.langchain4j.adapter.LangChain4jAiModel;
import com.xiongx9527.fw.plus.ai.langchain4j.adapter.LangChain4jModel;
import com.xiongx9527.fw.plus.ai.model.AiModel;
import com.xiongx9527.fw.plus.ai.model.AiModelRegistry;
import com.xiongx9527.fw.plus.ai.model.AiModelType;

import java.util.List;

public class LangChain4jTinyEngineAiProvider implements TinyEngineAiProvider {

	private final AiModelRegistry modelRegistry;
	private final TinyEngineAiProperties properties;

	public LangChain4jTinyEngineAiProvider(AiModelRegistry modelRegistry, TinyEngineAiProperties properties) {
		this.modelRegistry = modelRegistry;
		this.properties = properties;
	}

	@Override
	public String getKey() {
		return TinyEngineAiProviderRegistry.DEFAULT_PROVIDER_KEY;
	}

	@Override
	public boolean supports(TinyEngineAiCapability capability) {
		return capability == TinyEngineAiCapability.CHAT || capability == TinyEngineAiCapability.STREAMING_CHAT;
	}

	@Override
	public TinyEngineChatResponse chat(TinyEngineChatRequest request) {
		resolveNativeModel(request, AiModelType.CHAT);
		throw nativeInvocationUnavailable(TinyEngineAiCapability.CHAT);
	}

	@Override
	public List<TinyEngineChatStreamChunk> stream(TinyEngineChatRequest request) {
		resolveNativeModel(request, AiModelType.STREAMING_CHAT);
		throw nativeInvocationUnavailable(TinyEngineAiCapability.STREAMING_CHAT);
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

	private IllegalStateException nativeInvocationUnavailable(TinyEngineAiCapability capability) {
		return new IllegalStateException("TinyEngine AI provider 'langchain4j' native " + capability
				+ " invocation is unavailable until TinyEngine request mapping is defined");
	}

	private boolean isBlank(String value) {
		return value == null || value.trim().length() == 0;
	}
}
