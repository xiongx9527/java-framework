package com.xiongx9527.fw.plus.ai.spring.adapter;

import com.xiongx9527.fw.plus.ai.model.AiChatModel;
import com.xiongx9527.fw.plus.ai.model.AiChatRequest;
import com.xiongx9527.fw.plus.ai.model.AiChatResult;
import com.xiongx9527.fw.plus.ai.model.AiEmbeddingModel;
import com.xiongx9527.fw.plus.ai.model.AiEmbeddingRequest;
import com.xiongx9527.fw.plus.ai.model.AiEmbeddingResult;
import com.xiongx9527.fw.plus.ai.model.AiModel;
import com.xiongx9527.fw.plus.ai.model.AiModelAdapter;
import com.xiongx9527.fw.plus.ai.model.AiModelDefinition;
import com.xiongx9527.fw.plus.ai.model.AiModelException;
import com.xiongx9527.fw.plus.ai.model.AiModelType;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.embedding.EmbeddingResponseMetadata;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpringAiModelAdapterTest {

    @Test
    void shouldDescribeExplicitProviderBindingWithoutNativeModel() {
        SpringAiProviderBinding binding = new SpringAiProviderBinding(
                "spring-ai-alibaba", "dashscope", AiModelType.CHAT, "dashScopeChatModel");

        assertEquals("spring-ai-alibaba", binding.getFramework());
        assertEquals("dashscope", binding.getProvider());
        assertEquals(AiModelType.CHAT, binding.getType());
        assertEquals("dashScopeChatModel", binding.getBeanName());
        assertThrows(IllegalArgumentException.class, () -> new SpringAiProviderBinding(
                "spring-ai", "openai", AiModelType.IMAGE, "imageModel"));
        assertFalse(hasPublicNativeAccessor(binding));
    }

    @Test
    void shouldExposeSeparateFrameworkAdaptersWithSharedChatFacade() {
        ChatModel nativeModel = prompt -> chatResponse("answer", "STOP", new DefaultUsage(7, 3, 10));
        SpringAiModelProviderAdapter provider = new FakeProvider("openai", AiModelType.CHAT, nativeModel);
        SpringAiModelProviderAdapter alibabaProvider = new FakeProvider("dashscope", AiModelType.CHAT, nativeModel);
        AiModelAdapter springAdapter = new SpringAiModelAdapter(Collections.singletonList(provider));
        AiModelAdapter alibabaAdapter = new SpringAiAlibabaModelAdapter(Collections.singletonList(alibabaProvider));

        AiModel springModel = springAdapter.create(definition("spring-chat", "spring-ai", "openai", AiModelType.CHAT));
        AiModel alibabaModel = alibabaAdapter.create(
                definition("dashscope-chat", "spring-ai-alibaba", "dashscope", AiModelType.CHAT));

        assertEquals("spring-ai", springAdapter.getFramework());
        assertEquals("spring-ai-alibaba", alibabaAdapter.getFramework());
        assertEquals(springModel.getClass(), alibabaModel.getClass());
        assertFalse(hasPublicNativeAccessor(springModel));
        assertFalse(hasPublicNativeAccessor(alibabaModel));

        AiChatResult result = ((AiChatModel) springModel).chat(
                new AiChatRequest("hello", Collections.emptyMap(), 0L));
        assertEquals("answer", result.getText());
        assertEquals("spring-chat", result.getActualModelKey());
        assertEquals("STOP", result.getFinishReason());
        assertEquals(7L, result.getUsage().getInputTokens());
        assertEquals(3L, result.getUsage().getOutputTokens());
        assertEquals(10L, result.getUsage().getTotalTokens());
    }

    @Test
    void shouldEmbedBatchInOrderAndDefensivelyCopyVectors() {
        float[] first = new float[] { 0.1F, 0.2F };
        float[] second = new float[] { 0.3F, 0.4F };
        EmbeddingModel nativeModel = embeddingModel(new EmbeddingResponse(Arrays.asList(
                new Embedding(first, 0), new Embedding(second, 1)),
                new EmbeddingResponseMetadata("embedding-model", new DefaultUsage(4, 0, 4))));
        AiEmbeddingModel model = (AiEmbeddingModel) new SpringAiAlibabaModelAdapter(Collections.singletonList(
                new FakeProvider("dashscope", AiModelType.EMBEDDING, nativeModel)))
                .create(definition("dashscope-embedding", "spring-ai-alibaba", "dashscope",
                        AiModelType.EMBEDDING));

        AiEmbeddingResult result = model.embed(new AiEmbeddingRequest(
                Arrays.asList("first", "second"), Collections.emptyMap(), 0L));
        first[0] = 9F;
        second[0] = 9F;

        assertEquals("dashscope-embedding", result.getActualModelKey());
        assertArrayEquals(new float[] { 0.1F, 0.2F }, result.getEmbeddings().get(0));
        assertArrayEquals(new float[] { 0.3F, 0.4F }, result.getEmbeddings().get(1));
        assertEquals(4L, result.getUsage().getInputTokens());
        assertEquals(4L, result.getUsage().getTotalTokens());
    }

    @Test
    void shouldRestoreEmbeddingInputOrderFromResultIndexesAndUseUnknownUsage() {
        EmbeddingModel nativeModel = embeddingModel(new EmbeddingResponse(Arrays.asList(
                new Embedding(new float[] { 0.3F, 0.4F }, 1),
                new Embedding(new float[] { 0.1F, 0.2F }, 0))));
        AiEmbeddingModel model = (AiEmbeddingModel) new SpringAiModelAdapter(Collections.singletonList(
                new FakeProvider("openai", AiModelType.EMBEDDING, nativeModel)))
                .create(definition("ordered-embedding", "spring-ai", "openai", AiModelType.EMBEDDING));

        AiEmbeddingResult result = model.embed(new AiEmbeddingRequest(
                Arrays.asList("first", "second"), Collections.emptyMap(), 0L));

        assertArrayEquals(new float[] { 0.1F, 0.2F }, result.getEmbeddings().get(0));
        assertArrayEquals(new float[] { 0.3F, 0.4F }, result.getEmbeddings().get(1));
        assertTrue(result.getUsage().isUnknown());
    }

    @Test
    void shouldRemoveSensitiveProviderMessagesFromExceptionChain() {
        AiModelDefinition definition = definition("secured-chat", "spring-ai", "openai", AiModelType.CHAT);
        SpringAiModelAdapter creationAdapter = new SpringAiModelAdapter(Collections.singletonList(
                new ThrowingProvider(new SecurityException("api-key=super-secret"))));

        AiModelException creationFailure = assertThrows(AiModelException.class,
                () -> creationAdapter.create(definition));
        assertFalse(exceptionChainContains(creationFailure, "super-secret"));

        SpringAiModelAdapter modelExceptionAdapter = new SpringAiModelAdapter(Collections.singletonList(
                new ThrowingProvider(AiModelException.requestRejected("api-key=super-secret", null))));
        AiModelException modelCreationFailure = assertThrows(AiModelException.class,
                () -> modelExceptionAdapter.create(definition));
        assertEquals(AiModelException.MODEL_REQUEST_REJECTED, modelCreationFailure.getErrorCode());
        assertFalse(exceptionChainContains(modelCreationFailure, "super-secret"));

        AiChatModel invocationModel = chatModel("secured-invocation", prompt -> {
            throw AiModelException.infrastructureFailure("authorization=super-secret", null);
        });
        AiModelException invocationFailure = assertThrows(AiModelException.class, () -> invocationModel.chat(
                new AiChatRequest("hello", Collections.emptyMap(), 0L)));
        assertEquals(AiModelException.MODEL_INFRASTRUCTURE_FAILURE, invocationFailure.getErrorCode());
        assertTrue(invocationFailure.isRetryable());
        assertFalse(exceptionChainContains(invocationFailure, "super-secret"));
    }

    @Test
    void shouldBoundThreadsWhenTimedOutCallsIgnoreInterrupts() {
        ChatModel stubbornModel = prompt -> {
            long deadline = System.nanoTime() + 300_000_000L;
            while (System.nanoTime() < deadline) {
                if (Thread.interrupted()) {
                    // 模拟忽略中断的第三方客户端。
                }
            }
            return chatResponse("late", "STOP", null);
        };
        AiChatModel model = chatModel("bounded-timeout", stubbornModel);

        for (int index = 0; index < SpringAiModel.MAX_CONCURRENT_CALLS * 2; index++) {
            AiModelException failure = assertThrows(AiModelException.class, () -> model.chat(
                    new AiChatRequest("hello", Collections.emptyMap(), 1L)));
            assertEquals(AiModelException.MODEL_INFRASTRUCTURE_FAILURE, failure.getErrorCode());
        }

        assertTrue(countCallThreads() <= SpringAiModel.MAX_CONCURRENT_CALLS);
    }

    @Test
    void shouldRestoreInterruptStatusForSynchronousInterruptedFailure() {
        AiChatModel model = chatModel("interrupted-chat", prompt -> {
            throw new RuntimeException(new InterruptedException("interrupted"));
        });
        try {
            AiModelException failure = assertThrows(AiModelException.class, () -> model.chat(
                    new AiChatRequest("hello", Collections.emptyMap(), 0L)));
            assertEquals(AiModelException.MODEL_INFRASTRUCTURE_FAILURE, failure.getErrorCode());
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void shouldSanitizeProviderSupportFailuresAndValidateDirectDefinitions() {
        SpringAiModelAdapter adapter = new SpringAiModelAdapter(Collections.singletonList(
                new ThrowingSupportProvider(new SecurityException("api-key=super-secret"))));
        AiModelException supportFailure = assertThrows(AiModelException.class, () -> adapter.supports(
                definition("support", "spring-ai", "openai", AiModelType.CHAT)));
        assertFalse(exceptionChainContains(supportFailure, "super-secret"));

        AiModelDefinition blankKey = definition(" ", "spring-ai", "openai", AiModelType.CHAT);
        AiModelException validationFailure = assertThrows(AiModelException.class, () -> adapter.create(blankKey));
        assertTrue(validationFailure.getMessage().contains("key"));
    }

    @Test
    void shouldUseExactFrameworkMatchingLikeNeutralFactory() {
        SpringAiModelAdapter adapter = new SpringAiModelAdapter(Collections.singletonList(
                new FakeProvider("openai", AiModelType.CHAT,
                        (ChatModel) prompt -> chatResponse("answer", "STOP", null))));
        assertFalse(adapter.supports(definition("chat", "SPRING-AI", "openai", AiModelType.CHAT)));
        assertThrows(AiModelException.class,
                () -> adapter.create(definition("chat", "SPRING-AI", "openai", AiModelType.CHAT)));
    }

    @Test
    void shouldNormalizeProviderBindingIdentifiers() {
        SpringAiProviderBinding binding = new SpringAiProviderBinding(
                " SPRING-AI ", " OpenAI ", AiModelType.CHAT, " chatModel ");
        assertEquals("spring-ai", binding.getFramework());
        assertEquals("openai", binding.getProvider());
        assertEquals("chatModel", binding.getBeanName());
        assertTrue(binding.matches("SPRING-AI", "OPENAI", AiModelType.CHAT));
    }

    @Test
    void shouldRejectBlankChatText() {
        AiChatModel model = chatModel("blank-chat", prompt -> chatResponse("   ", "STOP", null));
        AiModelException failure = assertThrows(AiModelException.class, () -> model.chat(
                new AiChatRequest("hello", Collections.emptyMap(), 0L)));
        assertEquals(AiModelException.MODEL_REQUEST_REJECTED, failure.getErrorCode());
    }

    @Test
    void shouldRejectUnsupportedParametersBeforeCallingSdk() {
        AtomicInteger calls = new AtomicInteger();
        ChatModel nativeModel = prompt -> {
            calls.incrementAndGet();
            return chatResponse("answer", "STOP", null);
        };
        AiChatModel model = (AiChatModel) new SpringAiModelAdapter(Collections.singletonList(
                new FakeProvider("openai", AiModelType.CHAT, nativeModel)))
                .create(definition("parameter-chat", "spring-ai", "openai", AiModelType.CHAT));

        AiModelException exception = assertThrows(AiModelException.class, () -> model.chat(new AiChatRequest(
                "hello", Collections.singletonMap("temperature", 0.2D), 0L)));

        assertEquals(AiModelException.MODEL_REQUEST_REJECTED, exception.getErrorCode());
        assertFalse(exception.isRetryable());
        assertEquals(0, calls.get());
    }

    @Test
    void shouldClassifyTimeoutAndNetworkFailureAsInfrastructureFailures() {
        ChatModel slowModel = prompt -> {
            try {
                Thread.sleep(5000L);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(exception);
            }
            return chatResponse("late", "STOP", null);
        };
        AiChatModel timeoutModel = chatModel("slow-chat", slowModel);

        AiModelException timeout = assertThrows(AiModelException.class, () -> timeoutModel.chat(
                new AiChatRequest("hello", Collections.emptyMap(), 20L)));
        assertEquals(AiModelException.MODEL_INFRASTRUCTURE_FAILURE, timeout.getErrorCode());
        assertTrue(timeout.isRetryable());

        AiChatModel networkModel = chatModel("network-chat", prompt -> {
            throw new RuntimeException(new IOException("connection reset"));
        });
        AiModelException network = assertThrows(AiModelException.class, () -> networkModel.chat(
                new AiChatRequest("hello", Collections.emptyMap(), 0L)));
        assertEquals(AiModelException.MODEL_INFRASTRUCTURE_FAILURE, network.getErrorCode());
        assertTrue(network.isRetryable());
    }

    @Test
    void shouldRejectWrongFrameworkUnsupportedTypeAndAmbiguousProvidersWithoutSecrets() {
        List<SpringAiModelProviderAdapter> providers = Arrays.asList(
                new FakeProvider("openai", AiModelType.CHAT,
                        (ChatModel) prompt -> chatResponse("one", "STOP", null)),
                new FakeProvider("openai", AiModelType.CHAT,
                        (ChatModel) prompt -> chatResponse("two", "STOP", null)));
        SpringAiModelAdapter adapter = new SpringAiModelAdapter(providers);

        assertFalse(adapter.supports(definition("wrong", "spring-ai-alibaba", "openai", AiModelType.CHAT)));
        assertFalse(adapter.supports(definition("image", "spring-ai", "openai", AiModelType.IMAGE)));

        AiModelDefinition ambiguous = definition("ambiguous", "spring-ai", "openai", AiModelType.CHAT);
        ambiguous.getProperties().put("apiKey", "super-secret");
        AiModelException exception = assertThrows(AiModelException.class, () -> adapter.create(ambiguous));
        assertTrue(exception.getMessage().contains("key=ambiguous"));
        assertTrue(exception.getMessage().contains("framework=spring-ai"));
        assertTrue(exception.getMessage().contains("provider=openai"));
        assertTrue(exception.getMessage().contains("type=CHAT"));
        assertFalse(exception.getMessage().contains("super-secret"));
    }

    private AiChatModel chatModel(String key, ChatModel nativeModel) {
        return (AiChatModel) new SpringAiModelAdapter(Collections.singletonList(
                new FakeProvider("openai", AiModelType.CHAT, nativeModel)))
                .create(definition(key, "spring-ai", "openai", AiModelType.CHAT));
    }

    private static ChatResponse chatResponse(String text, String finishReason, DefaultUsage usage) {
        Generation generation = new Generation(new AssistantMessage(text),
                ChatGenerationMetadata.builder().finishReason(finishReason).build());
        ChatResponseMetadata metadata = ChatResponseMetadata.builder().usage(usage).build();
        return new ChatResponse(Collections.singletonList(generation), metadata);
    }

    private static EmbeddingModel embeddingModel(EmbeddingResponse response) {
        return new EmbeddingModel() {
            @Override
            public EmbeddingResponse call(EmbeddingRequest request) {
                return response;
            }

            @Override
            public float[] embed(Document document) {
                return response.getResult().getOutput();
            }
        };
    }

    private static AiModelDefinition definition(String key, String framework, String provider, AiModelType type) {
        AiModelDefinition definition = new AiModelDefinition();
        definition.setKey(key);
        definition.setFramework(framework);
        definition.setProvider(provider);
        definition.setType(type);
        definition.setEnabled(true);
        return definition;
    }

    private static boolean hasPublicNativeAccessor(Object model) {
        for (Method method : model.getClass().getMethods()) {
            if (method.getName().startsWith("getNative") || method.getName().startsWith("native")) {
                return true;
            }
        }
        return false;
    }

    private static int countCallThreads() {
        int count = 0;
        for (Thread thread : Thread.getAllStackTraces().keySet()) {
            if (thread.isAlive() && thread.getName().startsWith("fw-plus-spring-ai-model-call")) {
                count++;
            }
        }
        return count;
    }

    private static boolean exceptionChainContains(Throwable failure, String value) {
        Throwable current = failure;
        while (current != null) {
            if (current.getMessage() != null && current.getMessage().contains(value)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private static final class ThrowingSupportProvider implements SpringAiModelProviderAdapter {

        private final RuntimeException failure;

        private ThrowingSupportProvider(RuntimeException failure) {
            this.failure = failure;
        }

        @Override
        public boolean supports(AiModelDefinition definition) {
            throw failure;
        }

        @Override
        public Object create(AiModelDefinition definition) {
            throw new AssertionError("create must not be called");
        }
    }

    private static final class ThrowingProvider implements SpringAiModelProviderAdapter {

        private final RuntimeException failure;

        private ThrowingProvider(RuntimeException failure) {
            this.failure = failure;
        }

        @Override
        public boolean supports(AiModelDefinition definition) {
            return true;
        }

        @Override
        public Object create(AiModelDefinition definition) {
            throw failure;
        }
    }

    private static final class FakeProvider implements SpringAiModelProviderAdapter {

        private final String provider;
        private final AiModelType type;
        private final Object model;

        private FakeProvider(String provider, AiModelType type, Object model) {
            this.provider = provider;
            this.type = type;
            this.model = model;
        }

        @Override
        public boolean supports(AiModelDefinition definition) {
            return definition != null && provider.equals(definition.getProvider()) && type == definition.getType();
        }

        @Override
        public Object create(AiModelDefinition definition) {
            return model;
        }
    }
}
