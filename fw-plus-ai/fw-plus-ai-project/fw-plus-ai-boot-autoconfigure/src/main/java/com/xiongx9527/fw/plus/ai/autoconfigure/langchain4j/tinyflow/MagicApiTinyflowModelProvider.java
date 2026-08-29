package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.tinyflow;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.model.model.MagicDynamicModelDataSource;
import com.xiongx9527.fw.plus.ai.langchain4j.adapter.model.LangChain4jAiModel;
import com.xiongx9527.fw.plus.ai.model.AiModel;
import com.xiongx9527.fw.plus.ai.model.AiModelType;
import com.xiongx9527.fw.plus.ai.tinyflow.TinyflowException;
import com.xiongx9527.fw.plus.ai.tinyflow.model.TinyflowEmbeddingModel;
import com.xiongx9527.fw.plus.ai.tinyflow.model.TinyflowModelDescriptor;
import com.xiongx9527.fw.plus.ai.tinyflow.model.TinyflowModelProvider;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class MagicApiTinyflowModelProvider implements TinyflowModelProvider {

	private final MagicDynamicModelDataSource dataSource;

	public MagicApiTinyflowModelProvider(MagicDynamicModelDataSource dataSource) {
		if (dataSource == null) {
			throw new IllegalArgumentException("dataSource must not be null");
		}
		this.dataSource = dataSource;
	}

	@Override
	public List<TinyflowModelDescriptor> listChatModels() {
		return descriptors(AiModelType.CHAT);
	}

	@Override
	public List<TinyflowModelDescriptor> listEmbeddingModels() {
		return descriptors(AiModelType.EMBEDDING);
	}

	@Override
	public ChatLanguageModel getChatModel(String key) {
		Object nativeModel = nativeModel(key, AiModelType.CHAT);
		if (!(nativeModel instanceof ChatLanguageModel)) {
			throw unavailable("chat", key);
		}
		return (ChatLanguageModel) nativeModel;
	}

	@Override
	public TinyflowEmbeddingModel getEmbeddingModel(String key) {
		Object nativeModel = nativeModel(key, AiModelType.EMBEDDING);
		if (!(nativeModel instanceof EmbeddingModel)) {
			throw unavailable("embedding", key);
		}
		final EmbeddingModel embeddingModel = (EmbeddingModel) nativeModel;
		return new TinyflowEmbeddingModel() {
			@Override
			public float[] embed(String text) {
				return embeddingModel.embed(text).content().vector();
			}
		};
	}

	private List<TinyflowModelDescriptor> descriptors(AiModelType requiredType) {
		List<TinyflowModelDescriptor> result = new ArrayList<TinyflowModelDescriptor>();
		for (String key : dataSource.keys()) {
			AiModel model = dataSource.getModel(key);
			if (supports(model, requiredType)) {
				result.add(new TinyflowModelDescriptor(key, key, requiredType.name(), model.getProvider()));
			}
		}
		return Collections.unmodifiableList(result);
	}

	private Object nativeModel(String key, AiModelType type) {
		AiModel model = dataSource.getModel(key);
		if (!supports(model, type)) {
			throw unavailable(type == AiModelType.CHAT ? "chat" : "embedding", key);
		}
		return ((LangChain4jAiModel) model).getNativeModel().getModel();
	}

	private boolean supports(AiModel model, AiModelType type) {
		if (model == null || !type.equals(model.getType()) || !(model instanceof LangChain4jAiModel)) {
			return false;
		}
		Object nativeModel = ((LangChain4jAiModel) model).getNativeModel().getModel();
		return type == AiModelType.CHAT ? nativeModel instanceof ChatLanguageModel : nativeModel instanceof EmbeddingModel;
	}

	private TinyflowException unavailable(String capability, String key) {
		return new TinyflowException("Tinyflow " + capability + " model is unavailable or unsupported: " + key);
	}
}
