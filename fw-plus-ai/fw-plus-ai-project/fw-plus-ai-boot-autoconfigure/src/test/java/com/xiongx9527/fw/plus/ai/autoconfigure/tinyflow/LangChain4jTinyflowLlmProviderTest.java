package com.xiongx9527.fw.plus.ai.autoconfigure.tinyflow;

import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.tinyflow.LangChain4jRegistryTinyflowModelProvider;
import com.xiongx9527.fw.plus.ai.langchain4j.adapter.LangChain4jAiModel;
import com.xiongx9527.fw.plus.ai.langchain4j.adapter.LangChain4jModel;
import com.xiongx9527.fw.plus.ai.langchain4j.adapter.LangChain4jModelRegistry;
import com.xiongx9527.fw.plus.ai.langchain4j.adapter.LangChain4jModelType;
import com.xiongx9527.fw.plus.ai.model.AiModel;
import com.xiongx9527.fw.plus.ai.model.AiModelDefinition;
import com.xiongx9527.fw.plus.ai.model.AiModelRegistry;
import com.xiongx9527.fw.plus.ai.model.AiModelType;
import com.xiongx9527.fw.plus.ai.tinyflow.TinyflowException;
import com.xiongx9527.fw.plus.ai.tinyflow.model.TinyflowEmbeddingModel;
import com.xiongx9527.fw.plus.ai.tinyflow.model.TinyflowModelProvider;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import org.junit.Test;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class LangChain4jTinyflowLlmProviderTest {

	@Test
	public void shouldRejectNonChatModelAndIncludeLlmId() {
		TinyflowModelProvider provider = provider(models("embedding-only", model("embedding-only",
				AiModelType.EMBEDDING, LangChain4jModelType.EMBEDDING, new FixedEmbeddingModel())));

		TinyflowException error = assertThrows(TinyflowException.class, () -> provider.getChatModel("embedding-only"));

		assertTrue(error.getMessage().contains("embedding-only"));
	}

	@Test
	public void shouldResolveEmbeddingModelByKeyAndAdaptItsVector() {
		TinyflowModelProvider provider = provider(models("embedding", model("embedding", AiModelType.EMBEDDING,
				LangChain4jModelType.EMBEDDING, new FixedEmbeddingModel())));

		TinyflowEmbeddingModel embeddingModel = provider.getEmbeddingModel("embedding");

		assertArrayEquals(new float[] { 1.0F, 2.0F }, embeddingModel.embed("text"), 0.0F);
	}

	@Test
	public void shouldRejectNativeEmbeddingModelWhenRegisteredTypeIsChat() {
		TinyflowModelProvider provider = provider(models("wrong-type", model("wrong-type", AiModelType.CHAT,
				LangChain4jModelType.EMBEDDING, new FixedEmbeddingModel())));

		TinyflowException error = assertThrows(TinyflowException.class, () -> provider.getEmbeddingModel("wrong-type"));

		assertTrue(error.getMessage().contains("wrong-type"));
	}

	@Test
	public void shouldExposeOnlyRealCapabilityMatchedModelMetadata() {
		TinyflowModelProvider provider = provider(models("embedding", model("embedding", AiModelType.EMBEDDING,
				LangChain4jModelType.EMBEDDING, new FixedEmbeddingModel())));

		assertEquals(0, provider.listChatModels().size());
		assertEquals(1, provider.listEmbeddingModels().size());
		assertEquals("embedding", provider.listEmbeddingModels().get(0).getKey());
	}

	private LangChain4jRegistryTinyflowModelProvider provider(Map<String, AiModel> models) {
		return new LangChain4jRegistryTinyflowModelProvider(new TestAiModelRegistry(models, "embedding"),
				new TestLangChain4jModelRegistry());
	}

	private Map<String, AiModel> models(String key, AiModel model) {
		Map<String, AiModel> values = new LinkedHashMap<String, AiModel>();
		values.put(key, model);
		return values;
	}

	private AiModel model(String key, AiModelType type, LangChain4jModelType nativeType, Object nativeModel) {
		AiModelDefinition definition = new AiModelDefinition();
		definition.setKey(key);
		definition.setFramework("langchain4j");
		definition.setProvider("test");
		definition.setType(type);
		return new LangChain4jAiModel(definition, new LangChain4jModel("test", nativeType, nativeModel));
	}

	private static final class FixedEmbeddingModel implements EmbeddingModel {
		@Override
		public Response<Embedding> embed(String text) {
			return Response.from(Embedding.from(new float[] { 1.0F, 2.0F }));
		}

		@Override
		public Response<Embedding> embed(dev.langchain4j.data.segment.TextSegment textSegment) {
			return embed(textSegment.text());
		}

		@Override
		public Response<java.util.List<Embedding>> embedAll(
				java.util.List<dev.langchain4j.data.segment.TextSegment> textSegments) {
			return Response.from(java.util.Collections.singletonList(Embedding.from(new float[] { 1.0F, 2.0F })));
		}

		@Override
		public int dimension() {
			return 2;
		}
	}

	private static final class TestAiModelRegistry implements AiModelRegistry {
		private final Map<String, AiModel> models;
		private final String defaultKey;
		private TestAiModelRegistry(Map<String, AiModel> models, String defaultKey) {
			this.models = models;
			this.defaultKey = defaultKey;
		}
		@Override public AiModel get(String key) { return models.get(key); }
		@Override public AiModel getDefault() { return models.get(defaultKey); }
		@Override public String getDefaultKey() { return defaultKey; }
		@Override public boolean contains(String key) { return models.containsKey(key); }
		@Override public Set<String> keys() { return models.keySet(); }
	}

	private static final class TestLangChain4jModelRegistry implements LangChain4jModelRegistry {
		@Override public LangChain4jModel get(String key) { return null; }
		@Override public LangChain4jModel getDefault() { return null; }
		@Override public boolean contains(String key) { return false; }
		@Override public Set<String> keys() { return Collections.emptySet(); }
	}
}
