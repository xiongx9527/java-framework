package com.xiongx9527.fw.plus.ai.langchain4j.adapter.model;

import com.xiongx9527.fw.plus.ai.model.AiModel;
import com.xiongx9527.fw.plus.ai.model.AiModelAdapter;
import com.xiongx9527.fw.plus.ai.model.AiModelDefinition;
import com.xiongx9527.fw.plus.ai.model.AiModelType;
import com.xiongx9527.fw.plus.ai.model.DefaultAiModelRegistry;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DefaultLangChain4jModelRegistryTest {

    @Test
    public void shouldResolveMultipleModelsProviderAliasAndDefaultKeyThroughNeutralRegistry() {
        AiModelDefinition chat = definition("chat.alias", AiModelType.CHAT);
        AiModelDefinition embedding = definition("embedding.alias", AiModelType.EMBEDDING);
        AiModelAdapter adapter = new DefaultLangChain4jModelFactory(
                Collections.<LangChain4jModelProviderAdapter>singletonList(new AliasAdapter()));

        DefaultAiModelRegistry registry = new DefaultAiModelRegistry(
                Arrays.asList(chat, embedding), "embedding.alias", Collections.singletonList(adapter));

        AiModel chatModel = registry.get("chat.alias");
        AiModel embeddingModel = registry.get("embedding.alias");
        assertSame(embeddingModel, registry.getDefault());
        assertTrue(chatModel != embeddingModel);
        assertTrue(registry.contains("chat.alias"));
        assertTrue(registry.keys().contains("embedding.alias"));
        assertTrue("openai-compatible".equals(chatModel.getProvider()));
        assertTrue("openai-compatible".equals(embeddingModel.getProvider()));
    }

    private AiModelDefinition definition(String key, AiModelType type) {
        AiModelDefinition definition = new AiModelDefinition();
        definition.setKey(key);
        definition.setFramework("langchain4j");
        definition.setProvider("openai-compatible");
        definition.setType(type);
        definition.setEnabled(true);
        return definition;
    }

    @Test
    public void shouldResolveModelsAndDefaultModel() {
        Map<String, LangChain4jModel> models = new LinkedHashMap<String, LangChain4jModel>();
        LangChain4jModel chat = new LangChain4jModel("openai", LangChain4jModelType.CHAT, new Object());
        models.put("chat", chat);

        DefaultLangChain4jModelRegistry registry = new DefaultLangChain4jModelRegistry(models, "chat");

        assertSame(chat, registry.get("chat"));
        assertSame(chat, registry.getDefault());
        assertTrue(registry.contains("chat"));
        assertTrue(registry.keys().contains("chat"));
    }

    @Test
    public void shouldFailWhenDefaultModelKeyIsMissing() {
        LangChain4jModelException exception = assertThrows(LangChain4jModelException.class,
                () -> new DefaultLangChain4jModelRegistry(new LinkedHashMap<String, LangChain4jModel>(), "missing"));

        assertTrue(exception.getMessage().contains("missing"));
    }

    @Test
    public void shouldFailWhenModelKeyIsMissingOrMappedToNull() {
        DefaultLangChain4jModelRegistry registry =
                new DefaultLangChain4jModelRegistry(new LinkedHashMap<String, LangChain4jModel>(), null);

        LangChain4jModelException exception = assertThrows(LangChain4jModelException.class,
                () -> registry.get("missing"));

        assertTrue(exception.getMessage().contains("missing"));

        Map<String, LangChain4jModel> nullModels = new LinkedHashMap<String, LangChain4jModel>();
        nullModels.put("null-model", null);
        DefaultLangChain4jModelRegistry nullRegistry =
                new DefaultLangChain4jModelRegistry(nullModels, "null-model");

        LangChain4jModelException getException = assertThrows(LangChain4jModelException.class,
                () -> nullRegistry.get("null-model"));
        LangChain4jModelException defaultException = assertThrows(LangChain4jModelException.class,
                nullRegistry::getDefault);

        assertTrue(getException.getMessage().contains("null-model"));
        assertTrue(defaultException.getMessage().contains("null-model"));
    }

    @Test
    public void shouldStoreAnUnmodifiableCopyOfModels() {
        Map<String, LangChain4jModel> models = new LinkedHashMap<String, LangChain4jModel>();
        models.put("chat", new LangChain4jModel("openai", LangChain4jModelType.CHAT, new Object()));

        DefaultLangChain4jModelRegistry registry = new DefaultLangChain4jModelRegistry(models, "chat");
        models.clear();

        assertTrue(registry.contains("chat"));
        assertThrows(UnsupportedOperationException.class, () -> registry.keys().remove("chat"));
    }

    private static class AliasAdapter implements LangChain4jModelProviderAdapter {

        public boolean supports(LangChain4jModelDefinition definition) {
            return "openai-compatible".equals(definition.getProvider());
        }

        public LangChain4jModel create(LangChain4jModelDefinition definition) {
			Object model = definition.getModelType() == LangChain4jModelType.CHAT
					? chatModel() : embeddingModel();
			return new LangChain4jModel(definition.getProvider(), definition.getModelType(), model);
        }

		private ChatModel chatModel() {
			return new ChatModel() {
				@Override
				public ChatResponse chat(ChatRequest request) {
					return ChatResponse.builder().aiMessage(AiMessage.from("answer")).build();
				}
			};
		}

		private EmbeddingModel embeddingModel() {
			return new EmbeddingModel() {
				@Override
				public Response<List<Embedding>> embedAll(List<TextSegment> segments) {
					return Response.from(Collections.singletonList(Embedding.from(new float[] { 0.1F })));
				}

				@Override
				public int dimension() {
					return 1;
				}
			};
		}
    }
}
