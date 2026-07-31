package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.model;

import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.AskDataErrorCode;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.AskDataProperties;
import com.xiongx9527.fw.plus.ai.langchain4j.adapter.LangChain4jAiModel;
import com.xiongx9527.fw.plus.ai.langchain4j.adapter.LangChain4jModel;
import com.xiongx9527.fw.plus.ai.model.AiModel;
import com.xiongx9527.fw.plus.ai.model.AiModelRegistry;
import com.xiongx9527.fw.plus.ai.model.AiModelType;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;

import java.util.List;

/**
 * 从中立模型注册表解析 AskData 模型；LangChain4j 仅在此处作为默认运行时适配。
 */
public class AiModelAskDataModelRegistry implements AskDataModelRegistry {

	private final AskDataProperties properties;
	private final AiModelRegistry modelRegistry;

	public AiModelAskDataModelRegistry(AskDataProperties properties, AiModelRegistry modelRegistry) {
		this.properties = properties;
		this.modelRegistry = modelRegistry;
	}

	@Override
	public AskDataChatModel getChatModel(String modelKey) {
		String resolvedKey = resolveModelKey(modelKey, properties.getModel().getChatCandidates(),
				getDefaultChatModelKey(), AiModelType.CHAT);
		AiModel model = getModel(resolvedKey, AiModelType.CHAT, "聊天");
		if (model instanceof AskDataChatModel) {
			return (AskDataChatModel) model;
		}
		Object nativeModel = nativeModel(model, resolvedKey);
		if (!(nativeModel instanceof ChatLanguageModel)) {
			throw unsupportedModel("聊天", resolvedKey);
		}
		final ChatLanguageModel chatLanguageModel = (ChatLanguageModel) nativeModel;
		return prompt -> new AskDataModelResponse(chatLanguageModel.generate(prompt), null, resolvedKey);
	}

	@Override
	public AskDataEmbeddingModel getEmbeddingModel(String modelKey) {
		String resolvedKey = resolveModelKey(modelKey, properties.getModel().getEmbeddingCandidates(),
				getDefaultEmbeddingModelKey(), AiModelType.EMBEDDING);
		AiModel model = getModel(resolvedKey, AiModelType.EMBEDDING, "向量");
		if (model instanceof AskDataEmbeddingModel) {
			return (AskDataEmbeddingModel) model;
		}
		Object nativeModel = nativeModel(model, resolvedKey);
		if (!(nativeModel instanceof EmbeddingModel)) {
			throw unsupportedModel("向量", resolvedKey);
		}
		final EmbeddingModel embeddingModel = (EmbeddingModel) nativeModel;
		return text -> embeddingModel.embed(text).content().vectorAsList();
	}

	@Override
	public String getDefaultChatModelKey() {
		return properties.getModel().getDefaultChatModel();
	}

	@Override
	public String getDefaultEmbeddingModelKey() {
		return properties.getModel().getDefaultEmbeddingModel();
	}

	private String resolveModelKey(String modelKey, List<String> candidates, String defaultModelKey,
			AiModelType expectedType) {
		if (hasText(modelKey)) {
			return modelKey.trim();
		}
		if (candidates != null) {
			for (String candidate : candidates) {
				if (hasText(candidate) && containsModel(candidate.trim(), expectedType)) {
					return candidate.trim();
				}
			}
		}
		if (!hasText(defaultModelKey)) {
			throw new AskDataModelException(AskDataErrorCode.DEFAULT_MODEL_NOT_CONFIGURED, "未配置默认模型");
		}
		return defaultModelKey.trim();
	}

	private boolean containsModel(String key, AiModelType expectedType) {
		if (modelRegistry == null || !modelRegistry.contains(key)) {
			return false;
		}
		AiModel model = modelRegistry.get(key);
		return model != null && expectedType == model.getType();
	}

	private AiModel getModel(String key, AiModelType expectedType, String modelType) {
		if (!containsModel(key, expectedType)) {
			throw new AskDataModelException(AskDataErrorCode.MODEL_NOT_FOUND, "未找到" + modelType + "模型: " + key);
		}
		return modelRegistry.get(key);
	}

	private Object nativeModel(AiModel model, String key) {
		if (!(model instanceof LangChain4jAiModel)) {
			throw unsupportedModel("", key);
		}
		LangChain4jModel nativeModel = ((LangChain4jAiModel) model).getNativeModel();
		return nativeModel == null ? null : nativeModel.getModel();
	}

	private AskDataModelException unsupportedModel(String modelType, String key) {
		return new AskDataModelException(AskDataErrorCode.MODEL_NOT_FOUND,
				"AskData 不支持" + modelType + "模型适配: " + key);
	}

	private boolean hasText(String value) {
		return value != null && value.trim().length() > 0;
	}
}
