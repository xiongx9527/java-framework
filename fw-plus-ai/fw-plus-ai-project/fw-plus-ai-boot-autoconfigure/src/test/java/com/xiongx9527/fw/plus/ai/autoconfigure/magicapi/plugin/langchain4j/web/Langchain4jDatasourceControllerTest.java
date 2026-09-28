package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.web;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.Langchain4jModelFactoryRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.ModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.MagicDynamicLangchain4jEmbeddingModelDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.support.Langchain4jOptionsReader;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.Tokenizer;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.language.LanguageModel;
import dev.langchain4j.model.language.StreamingLanguageModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ssssssss.magicapi.core.config.MagicConfiguration;

import java.io.Closeable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;

/**
 * datasource 连接测试：七类实例的最小探测、未保存配置可测、streaming 只验证构建、
 * embedding-store 探测后关闭临时实例、错误返回中文信息。
 */
class Langchain4jDatasourceControllerTest {

    private final StubFactory factory = new StubFactory();
    private MagicDynamicLangchain4jEmbeddingModelDataSource embeddingModels;
    private Langchain4jDatasourceController controller;

    @BeforeEach
    void setUp() {
        Langchain4jModelFactoryRegistry factories = new Langchain4jModelFactoryRegistry();
        factories.register(factory);
        embeddingModels = new MagicDynamicLangchain4jEmbeddingModelDataSource();
        controller = new Langchain4jDatasourceController(mock(MagicConfiguration.class), factories, embeddingModels);
    }

    @Test
    void shouldProbeChatModelWithoutPersisting() {
        assertThat(controller.test("chat-model", body("stub"))).isEqualTo("ok");
        assertThat(controller.test("streaming-chat-model", body("stub"))).isEqualTo("ok");
        assertThat(controller.test("language-model", body("stub"))).isEqualTo("ok");
        assertThat(controller.test("streaming-language-model", body("stub"))).isEqualTo("ok");
        assertThat(controller.test("embedding-model", body("stub"))).isEqualTo("ok");
        assertThat(controller.test("tokenizer", body("stub"))).isEqualTo("ok");
    }

    @Test
    void shouldSkipGenerationForStreamingTypes() {
        controller.test("streaming-chat-model", body("stub"));
        controller.test("streaming-language-model", body("stub"));

        assertThat(factory.chatCalls.get()).as("streaming 两类只验证构建").isZero();
    }

    @Test
    void shouldProbeAndCloseEmbeddingStore() {
        embeddingModels.put("qwen-embed", embeddingModel());

        assertThat(controller.test("embedding-store", storeBody("qwen-embed"))).isEqualTo("ok");
        assertThat(factory.storeClosed.get()).as("探测实例应被关闭").isEqualTo(1);
    }

    @Test
    void shouldReportMissingEmbeddingModel() {
        String result = controller.test("embedding-store", storeBody("absent-embed"));

        assertThat(result).contains("请先配置并启用 embedding-model").contains("absent-embed");
        assertThat(factory.storeClosed.get()).isZero();
    }

    @Test
    void shouldReportOptionPathWhenRequiredOptionMissing() {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("key", "tokenizer-main");
        body.put("provider", "stub");
        body.put("options", new LinkedHashMap<String, Object>());

        assertThat(controller.test("tokenizer", body))
                .contains("langchain4j-tokenizer[tokenizer-main].options.api-key");
    }

    @Test
    void shouldRejectUnknownType() {
        assertThat(controller.test("no-such-type", body("stub"))).contains("不支持的资源类型");
    }

    @Test
    void shouldReportUnknownProvider() {
        assertThat(controller.test("chat-model", body("no-such-provider"))).contains("可用 provider");
    }

    private static Map<String, Object> body(String provider) {
        Map<String, Object> options = new LinkedHashMap<String, Object>();
        options.put("api-key", "sk-test");
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("key", "probe-main");
        body.put("provider", provider);
        body.put("options", options);
        return body;
    }

    private static Map<String, Object> storeBody(String embeddingModelKey) {
        Map<String, Object> body = body("stub");
        body.put("embeddingModelKey", embeddingModelKey);
        return body;
    }

    private static EmbeddingModel embeddingModel() {
        EmbeddingModel model = mock(EmbeddingModel.class);
        @SuppressWarnings("unchecked")
        Response<Embedding> response = mock(Response.class);
        org.mockito.Mockito.when(response.content()).thenReturn(Embedding.from(new float[] { 0.1f, 0.2f }));
        org.mockito.Mockito.when(model.embed("test")).thenReturn(response);
        org.mockito.Mockito.when(model.dimension()).thenReturn(2);
        return model;
    }

    /** 测试替身工厂：覆盖七类能力，tokenizer 要求 api-key，store 返回可关闭实例。 */
    private static final class StubFactory implements ModelFactory {

        private final AtomicInteger chatCalls = new AtomicInteger();
        private final AtomicInteger storeClosed = new AtomicInteger();

        @Override
        public String provider() {
            return "stub";
        }

        @Override
        public ChatLanguageModel buildChatModel(Langchain4jOptionsReader o) {
            ChatLanguageModel model = mock(ChatLanguageModel.class);
            org.mockito.Mockito.when(model.generate(org.mockito.ArgumentMatchers.any(
                    dev.langchain4j.data.message.UserMessage.class)))
                    .thenAnswer(invocation -> {
                        chatCalls.incrementAndGet();
                        return null;
                    });
            return model;
        }

        @Override
        public StreamingChatLanguageModel buildStreamingChatModel(Langchain4jOptionsReader o) {
            return mock(StreamingChatLanguageModel.class);
        }

        @Override
        public LanguageModel buildLanguageModel(Langchain4jOptionsReader o) {
            return mock(LanguageModel.class);
        }

        @Override
        public StreamingLanguageModel buildStreamingLanguageModel(Langchain4jOptionsReader o) {
            return mock(StreamingLanguageModel.class);
        }

        @Override
        public EmbeddingModel buildEmbeddingModel(Langchain4jOptionsReader o) {
            return Langchain4jDatasourceControllerTest.embeddingModel();
        }

        @Override
        public Tokenizer buildTokenizer(Langchain4jOptionsReader o) {
            o.required("api-key");
            return mock(Tokenizer.class);
        }

        @Override
        @SuppressWarnings("unchecked")
        public EmbeddingStore<TextSegment> buildEmbeddingStore(Langchain4jOptionsReader o, EmbeddingModel model) {
            EmbeddingStore<TextSegment> store = (EmbeddingStore<TextSegment>) mock(EmbeddingStore.class,
                    withSettings().extraInterfaces(Closeable.class));
            try {
                org.mockito.Mockito.doAnswer(invocation -> {
                    storeClosed.incrementAndGet();
                    return null;
                }).when((Closeable) store).close();
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
            return store;
        }
    }
}
