package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.model;

import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.AskDataErrorCode;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.AskDataProperties;
import com.xiongx9527.fw.plus.ai.model.AiModel;
import com.xiongx9527.fw.plus.ai.model.AiModelRegistry;
import com.xiongx9527.fw.plus.ai.model.AiModelType;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class AiModelAskDataModelRegistryTest {

	@Test
	public void shouldResolveConfiguredChatAndEmbeddingModelsFromNeutralRegistry() {
		AskDataProperties properties = new AskDataProperties();
		properties.getModel().setChatCandidates(Arrays.asList("chat-priority", "chat-default"));
		properties.getModel().setDefaultChatModel("chat-default");
		properties.getModel().setEmbeddingCandidates(Arrays.asList("embedding-priority", "embedding-default"));
		properties.getModel().setDefaultEmbeddingModel("embedding-default");

		Map<String, AiModel> models = new LinkedHashMap<String, AiModel>();
		models.put("chat-priority", new StubChatAiModel("chat-priority", "chat-response"));
		models.put("chat-default", new StubChatAiModel("chat-default", "default-response"));
		models.put("embedding-priority", new StubEmbeddingAiModel("embedding-priority", Arrays.asList(0.2F, 0.4F)));
		models.put("embedding-default", new StubEmbeddingAiModel("embedding-default", Arrays.asList(0.1F, 0.3F)));

		AskDataModelRegistry registry = new AiModelAskDataModelRegistry(properties, new StubAiModelRegistry(models));

		assertEquals("chat-response", registry.getChatModel(null).generate("question").getText());
		assertEquals(Arrays.asList(0.2F, 0.4F), registry.getEmbeddingModel(null).embed("question"));
	}

	@Test
	public void shouldRejectMissingConfiguredModelKey() {
		AskDataProperties properties = new AskDataProperties();
		properties.getModel().setDefaultChatModel("missing");

		AskDataModelRegistry registry = new AiModelAskDataModelRegistry(properties,
				new StubAiModelRegistry(new LinkedHashMap<String, AiModel>()));

		AskDataModelException exception = assertThrows(AskDataModelException.class,
				() -> registry.getChatModel(null));
		assertEquals(AskDataErrorCode.MODEL_NOT_FOUND, exception.getErrorCode());
	}

	private static final class StubAiModelRegistry implements AiModelRegistry {

		private final Map<String, AiModel> models;

		private StubAiModelRegistry(Map<String, AiModel> models) {
			this.models = models;
		}

		@Override
		public AiModel get(String key) {
			return models.get(key);
		}

		@Override
		public AiModel getDefault() {
			return null;
		}

		@Override
		public String getDefaultKey() {
			return null;
		}

		@Override
		public boolean contains(String key) {
			return models.containsKey(key);
		}

		@Override
		public Set<String> keys() {
			return new LinkedHashSet<String>(models.keySet());
		}
	}

	private static final class StubChatAiModel implements AiModel, AskDataChatModel {

		private final String key;
		private final String response;

		private StubChatAiModel(String key, String response) {
			this.key = key;
			this.response = response;
		}

		@Override
		public String getKey() {
			return key;
		}

		@Override
		public String getFramework() {
			return "stub";
		}

		@Override
		public String getProvider() {
			return "test";
		}

		@Override
		public AiModelType getType() {
			return AiModelType.CHAT;
		}

		@Override
		public AskDataModelResponse generate(String prompt) {
			return new AskDataModelResponse(response, null, key);
		}
	}

	private static final class StubEmbeddingAiModel implements AiModel, AskDataEmbeddingModel {

		private final String key;
		private final List<Float> embedding;

		private StubEmbeddingAiModel(String key, List<Float> embedding) {
			this.key = key;
			this.embedding = embedding;
		}

		@Override
		public String getKey() {
			return key;
		}

		@Override
		public String getFramework() {
			return "stub";
		}

		@Override
		public String getProvider() {
			return "test";
		}

		@Override
		public AiModelType getType() {
			return AiModelType.EMBEDDING;
		}

		@Override
		public List<Float> embed(String text) {
			return embedding;
		}
	}
}
