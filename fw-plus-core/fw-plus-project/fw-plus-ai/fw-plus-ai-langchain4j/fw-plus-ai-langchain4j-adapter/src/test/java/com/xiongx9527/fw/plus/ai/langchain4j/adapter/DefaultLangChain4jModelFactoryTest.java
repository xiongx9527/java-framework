package com.xiongx9527.fw.plus.ai.langchain4j.adapter;

import com.xiongx9527.fw.plus.ai.model.AiModel;
import com.xiongx9527.fw.plus.ai.model.AiModelAdapter;
import com.xiongx9527.fw.plus.ai.model.AiChatModel;
import com.xiongx9527.fw.plus.ai.model.AiChatRequest;
import com.xiongx9527.fw.plus.ai.model.AiChatResult;
import com.xiongx9527.fw.plus.ai.model.AiEmbeddingModel;
import com.xiongx9527.fw.plus.ai.model.AiEmbeddingRequest;
import com.xiongx9527.fw.plus.ai.model.AiEmbeddingResult;
import com.xiongx9527.fw.plus.ai.model.AiModelDefinition;
import com.xiongx9527.fw.plus.ai.model.AiModelException;
import com.xiongx9527.fw.plus.ai.model.AiModelType;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.FinishReason;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.output.TokenUsage;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DefaultLangChain4jModelFactoryTest {

    private final DefaultLangChain4jModelFactory factory = new DefaultLangChain4jModelFactory();

    @Test
    public void shouldExposeFrameworkForStrictNeutralSelection() {
        assertEquals("langchain4j", factory.getFramework());
    }

    @Test
    public void shouldExposeLangChain4jAsNeutralAdapterWithoutNativeAccessor() {
		ChatLanguageModel nativeModel = chatModel("answer");
        CapturingAdapter providerAdapter = new CapturingAdapter(
                new LangChain4jModel("openai-compatible", LangChain4jModelType.CHAT, nativeModel));
        AiModelAdapter adapter = new DefaultLangChain4jModelFactory(
                Collections.<LangChain4jModelProviderAdapter>singletonList(providerAdapter));
        AiModelDefinition definition = neutralDefinition("chat.alias", "openai-compatible", AiModelType.CHAT);

        AiModel model = adapter.create(definition);

        assertEquals("chat.alias", model.getKey());
        assertEquals("langchain4j", model.getFramework());
        assertEquals("openai-compatible", model.getProvider());
        assertEquals(AiModelType.CHAT, model.getType());
        assertEquals("alias-model", providerAdapter.definition.getModelName());
        assertFalse(hasPublicNativeAccessor(model));
    }

	@Test
	public void shouldCreateTypedChatModelAndPreserveExecutionMetadata() {
		ChatLanguageModel nativeModel = new ChatLanguageModel() {
			@Override
			public Response<AiMessage> generate(List<ChatMessage> messages) {
				assertEquals("hello", ((dev.langchain4j.data.message.UserMessage) messages.get(0)).singleText());
				return Response.from(AiMessage.from("answer"), new TokenUsage(7, 3, 10), FinishReason.STOP);
			}
		};
		AiModel model = typedModel(neutralDefinition("sales-chat", "openai-compatible", AiModelType.CHAT),
				new LangChain4jModel("openai-compatible", LangChain4jModelType.CHAT, nativeModel));

		assertTrue(model instanceof AiChatModel);
		AiChatResult result = ((AiChatModel) model).chat(
				new AiChatRequest("hello", Collections.<String, Object>emptyMap(), 0L));

		assertEquals("answer", result.getText());
		assertEquals("sales-chat", result.getActualModelKey());
		assertEquals("STOP", result.getFinishReason());
		assertEquals(7L, result.getUsage().getInputTokens());
		assertEquals(3L, result.getUsage().getOutputTokens());
		assertEquals(10L, result.getUsage().getTotalTokens());
	}

	@Test
	public void shouldEmbedBatchInOrderAndAggregateUsage() {
		EmbeddingModel nativeModel = new EmbeddingModel() {
			@Override
			public Response<List<Embedding>> embedAll(List<TextSegment> segments) {
				List<Embedding> embeddings = new ArrayList<Embedding>(segments.size());
				for (TextSegment segment : segments) {
					embeddings.add("first".equals(segment.text())
							? Embedding.from(new float[] { 0.1F, 0.2F })
							: Embedding.from(new float[] { 0.3F, 0.4F }));
				}
				return Response.from(embeddings, new TokenUsage(2, 0, 2));
			}

			@Override
			public int dimension() {
				return 2;
			}
		};
		AiModel model = typedModel(neutralDefinition("sales-embedding", "openai-compatible",
				AiModelType.EMBEDDING), new LangChain4jModel("openai-compatible",
				LangChain4jModelType.EMBEDDING, nativeModel));

		assertTrue(model instanceof AiEmbeddingModel);
		AiEmbeddingResult result = ((AiEmbeddingModel) model).embed(new AiEmbeddingRequest(
				Arrays.asList("first", "second"), Collections.<String, Object>emptyMap(), 0L));

		assertEquals("sales-embedding", result.getActualModelKey());
		assertEquals(2, result.getEmbeddings().size());
		assertArrayEquals(new float[] { 0.1F, 0.2F }, result.getEmbeddings().get(0));
		assertArrayEquals(new float[] { 0.3F, 0.4F }, result.getEmbeddings().get(1));
		assertEquals(4L, result.getUsage().getInputTokens());
		assertEquals(4L, result.getUsage().getTotalTokens());
	}

	@Test
	public void shouldEnforceRequestTimeoutAsRetryableInfrastructureFailure() {
		ChatLanguageModel slowModel = new ChatLanguageModel() {
			@Override
			public Response<AiMessage> generate(List<ChatMessage> messages) {
				try {
					Thread.sleep(5000L);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					throw new RuntimeException(e);
				}
				return Response.from(AiMessage.from("late"));
			}
		};
		AiChatModel model = (AiChatModel) typedModel(
				neutralDefinition("slow-chat", "openai-compatible", AiModelType.CHAT),
				new LangChain4jModel("openai-compatible", LangChain4jModelType.CHAT, slowModel));

		AiModelException exception = assertThrows(AiModelException.class,
				() -> model.chat(new AiChatRequest("hello", Collections.<String, Object>emptyMap(), 20L)));

		assertEquals(AiModelException.MODEL_INFRASTRUCTURE_FAILURE, exception.getErrorCode());
		assertTrue(exception.isRetryable());
	}

	@Test
	public void shouldClassifyNetworkFailureAsRetryable() {
		AiChatModel model = (AiChatModel) typedModel(
				neutralDefinition("network-chat", "openai-compatible", AiModelType.CHAT),
				new LangChain4jModel("openai-compatible", LangChain4jModelType.CHAT,
						failingChatModel(new RuntimeException(new IOException("connection reset")))));

		AiModelException exception = assertThrows(AiModelException.class,
				() -> model.chat(new AiChatRequest("hello", Collections.<String, Object>emptyMap(), 0L)));

		assertEquals(AiModelException.MODEL_INFRASTRUCTURE_FAILURE, exception.getErrorCode());
		assertTrue(exception.isRetryable());
	}

	@Test
	public void shouldClassifyAuthenticationFailureAsNonRetryable() {
		AiChatModel model = (AiChatModel) typedModel(
				neutralDefinition("secured-chat", "openai-compatible", AiModelType.CHAT),
				new LangChain4jModel("openai-compatible", LangChain4jModelType.CHAT,
						failingChatModel(new SecurityException("denied"))));

		AiModelException exception = assertThrows(AiModelException.class,
				() -> model.chat(new AiChatRequest("hello", Collections.<String, Object>emptyMap(), 0L)));

		assertEquals(AiModelException.MODEL_REQUEST_REJECTED, exception.getErrorCode());
		assertFalse(exception.isRetryable());
	}

	@Test
	public void shouldRejectUnsupportedPerRequestParametersWithoutCallingSdk() {
		AtomicInteger calls = new AtomicInteger();
		ChatLanguageModel nativeModel = new ChatLanguageModel() {
			@Override
			public Response<AiMessage> generate(List<ChatMessage> messages) {
				calls.incrementAndGet();
				return Response.from(AiMessage.from("answer"));
			}
		};
		AiChatModel model = (AiChatModel) typedModel(
				neutralDefinition("parameter-chat", "openai-compatible", AiModelType.CHAT),
				new LangChain4jModel("openai-compatible", LangChain4jModelType.CHAT, nativeModel));

		AiModelException exception = assertThrows(AiModelException.class, () -> model.chat(new AiChatRequest("hello",
				Collections.<String, Object>singletonMap("temperature", Double.valueOf(0.2D)), 0L)));

		assertEquals(AiModelException.MODEL_REQUEST_REJECTED, exception.getErrorCode());
		assertFalse(exception.isRetryable());
		assertEquals(0, calls.get());
	}

    @Test
    public void shouldCreateModelWithFirstSupportedAdapter() {
        LangChain4jModelDefinition definition = definition("chat");
		LangChain4jModel expected = new LangChain4jModel("openai", LangChain4jModelType.CHAT,
				chatModel("answer"));

        LangChain4jModel model = factory.create(definition, Arrays.<LangChain4jModelProviderAdapter>asList(
                new FakeAdapter(false, null),
                new FakeAdapter(true, expected)
        ));

        assertSame(expected, model);
    }

    @Test
    public void shouldFailWhenDefinitionIsInvalid() {
        assertThrows(LangChain4jModelException.class,
                () -> factory.create(null, Collections.<LangChain4jModelProviderAdapter>emptyList()));

        assertInvalidDefinition("key", null, "openai", LangChain4jModelType.CHAT);
        assertInvalidDefinition("provider", "chat", " ", LangChain4jModelType.CHAT);
        assertInvalidDefinition("modelType", "chat", "openai", null);
    }

    @Test
    public void shouldFailWhenProviderIsUnsupported() {
        LangChain4jModelException exception = assertThrows(LangChain4jModelException.class,
                () -> factory.create(definition("chat"), Collections.<LangChain4jModelProviderAdapter>emptyList()));

        assertContext(exception);
    }

    @Test
    public void shouldWrapAdapterExceptionWithContext() {
        ThrowingAdapter adapter = new ThrowingAdapter();
        LangChain4jModelException exception = assertThrows(LangChain4jModelException.class,
                () -> factory.create(definition("chat"),
                        Collections.<LangChain4jModelProviderAdapter>singletonList(adapter)));

        assertContext(exception);
        assertSame(adapter.failure, exception.getCause());
    }

    @Test
    public void shouldFailWhenAdapterReturnsNull() {
        LangChain4jModelException exception = assertThrows(LangChain4jModelException.class,
                () -> factory.create(definition("chat"),
                        Collections.<LangChain4jModelProviderAdapter>singletonList(new FakeAdapter(true, null))));

        assertTrue(exception.getMessage().contains("returned null"));
        assertContext(exception);
    }

    private void assertInvalidDefinition(String field, String key, String provider, LangChain4jModelType modelType) {
        LangChain4jModelDefinition definition = definition(key);
        definition.setProvider(provider);
        definition.setModelType(modelType);

        LangChain4jModelException exception = assertThrows(LangChain4jModelException.class,
                () -> factory.create(definition, Collections.<LangChain4jModelProviderAdapter>emptyList()));

        assertTrue(exception.getMessage().contains(field));
    }

    private void assertContext(LangChain4jModelException exception) {
        assertTrue(exception.getMessage().contains("chat"));
        assertTrue(exception.getMessage().contains("openai"));
        assertTrue(exception.getMessage().contains("CHAT"));
    }

    private LangChain4jModelDefinition definition(String key) {
        LangChain4jModelDefinition definition = new LangChain4jModelDefinition();
        definition.setKey(key);
        definition.setProvider("openai");
        definition.setModelType(LangChain4jModelType.CHAT);
        definition.setEnabled(true);
        return definition;
    }

    private AiModelDefinition neutralDefinition(String key, String provider, AiModelType type) {
        Map<String, Object> properties = new LinkedHashMap<String, Object>();
        properties.put("modelName", "alias-model");
        AiModelDefinition definition = new AiModelDefinition();
        definition.setKey(key);
        definition.setFramework("langchain4j");
        definition.setProvider(provider);
        definition.setType(type);
        definition.setEnabled(true);
        definition.setProperties(properties);
        return definition;
    }

	private AiModel typedModel(AiModelDefinition definition, LangChain4jModel nativeModel) {
		return new DefaultLangChain4jModelFactory(
				Collections.<LangChain4jModelProviderAdapter>singletonList(new CapturingAdapter(nativeModel)))
				.create(definition);
	}

	private ChatLanguageModel chatModel(final String text) {
		return new ChatLanguageModel() {
			@Override
			public Response<AiMessage> generate(List<ChatMessage> messages) {
				return Response.from(AiMessage.from(text));
			}
		};
	}

	private ChatLanguageModel failingChatModel(final RuntimeException failure) {
		return new ChatLanguageModel() {
			@Override
			public Response<AiMessage> generate(List<ChatMessage> messages) {
				throw failure;
			}
		};
	}

    private boolean hasPublicNativeAccessor(AiModel model) {
        for (Method method : model.getClass().getMethods()) {
            if ("getModel".equals(method.getName())
                    || method.getReturnType().getName().startsWith("dev.langchain4j.")) {
                return true;
            }
        }
        return false;
    }

    private static class FakeAdapter implements LangChain4jModelProviderAdapter {
        private final boolean supports;
        private final LangChain4jModel model;

        private FakeAdapter(boolean supports, LangChain4jModel model) {
            this.supports = supports;
            this.model = model;
        }

        public boolean supports(LangChain4jModelDefinition definition) {
            return supports;
        }

        public LangChain4jModel create(LangChain4jModelDefinition definition) {
            return model;
        }
    }

    private static class ThrowingAdapter implements LangChain4jModelProviderAdapter {
        private final LangChain4jModelException failure = new LangChain4jModelException("boom");

        public boolean supports(LangChain4jModelDefinition definition) {
            return true;
        }

        public LangChain4jModel create(LangChain4jModelDefinition definition) {
            throw failure;
        }
    }

    private static class CapturingAdapter implements LangChain4jModelProviderAdapter {
        private final LangChain4jModel model;
        private LangChain4jModelDefinition definition;

        private CapturingAdapter(LangChain4jModel model) {
            this.model = model;
        }

        public boolean supports(LangChain4jModelDefinition definition) {
            return "openai-compatible".equals(definition.getProvider());
        }

        public LangChain4jModel create(LangChain4jModelDefinition definition) {
            this.definition = definition;
            return model;
        }
    }
}
