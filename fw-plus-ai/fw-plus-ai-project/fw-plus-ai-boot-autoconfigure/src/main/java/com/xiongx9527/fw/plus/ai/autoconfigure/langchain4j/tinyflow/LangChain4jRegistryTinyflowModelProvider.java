package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.tinyflow;

import com.xiongx9527.fw.plus.ai.langchain4j.adapter.LangChain4jAiModel;
import com.xiongx9527.fw.plus.ai.langchain4j.adapter.LangChain4jModel;
import com.xiongx9527.fw.plus.ai.langchain4j.adapter.LangChain4jModelRegistry;
import com.xiongx9527.fw.plus.ai.langchain4j.adapter.LangChain4jModelType;
import com.xiongx9527.fw.plus.ai.model.AiModel;
import com.xiongx9527.fw.plus.ai.model.AiModelRegistry;
import com.xiongx9527.fw.plus.ai.model.AiModelType;
import com.xiongx9527.fw.plus.ai.tinyflow.TinyflowException;
import com.xiongx9527.fw.plus.ai.tinyflow.model.TinyflowEmbeddingModel;
import com.xiongx9527.fw.plus.ai.tinyflow.model.TinyflowModelDescriptor;
import com.xiongx9527.fw.plus.ai.tinyflow.model.TinyflowModelProvider;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class LangChain4jRegistryTinyflowModelProvider implements TinyflowModelProvider {

	private final AiModelRegistry modelRegistry;
	private final LangChain4jModelRegistry langChain4jModelRegistry;

	public LangChain4jRegistryTinyflowModelProvider(AiModelRegistry modelRegistry,
			LangChain4jModelRegistry langChain4jModelRegistry) {
		this.modelRegistry = modelRegistry;
		this.langChain4jModelRegistry = langChain4jModelRegistry;
	}

	@Override
	public List<TinyflowModelDescriptor> listChatModels() {
		return descriptors(true);
	}

	@Override
	public List<TinyflowModelDescriptor> listEmbeddingModels() {
		return descriptors(false);
	}

	@Override
	public ChatModel getChatModel(String key) {
		String resolvedKey = resolveKey(key);
		Object model = nativeModel(resolvedKey);
		if (!hasType(resolvedKey, true) || !(model instanceof ChatModel)) {
			throw unsupported("chat", resolvedKey);
		}
		return (ChatModel) model;
	}

	@Override
	public TinyflowEmbeddingModel getEmbeddingModel(String key) {
		String resolvedKey = resolveKey(key);
		Object model = nativeModel(resolvedKey);
		if (!hasType(resolvedKey, false) || !(model instanceof EmbeddingModel)) {
			throw unsupported("embedding", resolvedKey);
		}
		final EmbeddingModel embeddingModel = (EmbeddingModel) model;
		return new TinyflowEmbeddingModel() {
			@Override
			public float[] embed(String text) {
				return embeddingModel.embed(text).content().vector();
			}
		};
	}

	private List<TinyflowModelDescriptor> descriptors(boolean chat) {
		if (modelRegistry == null && langChain4jModelRegistry == null) {
			return Collections.emptyList();
		}
		List<TinyflowModelDescriptor> descriptors = new ArrayList<TinyflowModelDescriptor>();
		for (String key : keys()) {
			try {
				Object model = nativeModel(key);
				if (hasType(key, chat) && ((chat && model instanceof ChatModel)
						|| (!chat && model instanceof EmbeddingModel))) {
					descriptors.add(descriptor(key, chat));
				}
			} catch (RuntimeException ignored) {
				// A stale or unsupported registry entry is intentionally not exposed to the Tinyflow UI.
			}
		}
		return descriptors;
	}

	private TinyflowModelDescriptor descriptor(String key, boolean chat) {
		AiModel model = modelRegistry == null || !modelRegistry.contains(key) ? null : modelRegistry.get(key);
		LangChain4jModel nativeModel = langChain4jModelRegistry == null || !langChain4jModelRegistry.contains(key)
				? null : langChain4jModelRegistry.get(key);
		String provider = model == null ? nativeModel == null ? null : nativeModel.getProvider() : model.getProvider();
		return new TinyflowModelDescriptor(key, key, chat ? "CHAT" : "EMBEDDING", provider);
	}

	private Object nativeModel(String key) {
		if (modelRegistry != null && modelRegistry.contains(key)) {
			AiModel model = modelRegistry.get(key);
			if (model instanceof LangChain4jAiModel) {
				return ((LangChain4jAiModel) model).getNativeModel().getModel();
			}
		}
		if (langChain4jModelRegistry != null && langChain4jModelRegistry.contains(key)) {
			LangChain4jModel model = langChain4jModelRegistry.get(key);
			return model == null ? null : model.getModel();
		}
		throw unsupported("model", key);
	}

	private boolean hasType(String key, boolean chat) {
		if (modelRegistry != null && modelRegistry.contains(key)) {
			AiModel model = modelRegistry.get(key);
			return model != null && (chat ? AiModelType.CHAT : AiModelType.EMBEDDING).equals(model.getType());
		}
		if (langChain4jModelRegistry != null && langChain4jModelRegistry.contains(key)) {
			LangChain4jModel model = langChain4jModelRegistry.get(key);
			return model != null && (chat ? LangChain4jModelType.CHAT : LangChain4jModelType.EMBEDDING)
					.equals(model.getModelType());
		}
		return false;
	}

	private String resolveKey(String key) {
		if (hasText(key)) {
			return key.trim();
		}
		if (modelRegistry != null && hasText(modelRegistry.getDefaultKey())) {
			return modelRegistry.getDefaultKey();
		}
		if (langChain4jModelRegistry != null && hasText(langChain4jModelRegistry.getDefaultKey())) {
			return langChain4jModelRegistry.getDefaultKey();
		}
		throw new TinyflowException("Tinyflow model key must not be blank");
	}

	private Set<String> keys() {
		Set<String> keys = new LinkedHashSet<String>();
		if (modelRegistry != null) {
			keys.addAll(modelRegistry.keys());
		}
		if (langChain4jModelRegistry != null) {
			keys.addAll(langChain4jModelRegistry.keys());
		}
		return keys;
	}

	private TinyflowException unsupported(String capability, String key) {
		return new TinyflowException("Tinyflow " + capability + " model is unavailable or unsupported: " + key);
	}

	private boolean hasText(String value) {
		return value != null && value.trim().length() > 0;
	}
}
