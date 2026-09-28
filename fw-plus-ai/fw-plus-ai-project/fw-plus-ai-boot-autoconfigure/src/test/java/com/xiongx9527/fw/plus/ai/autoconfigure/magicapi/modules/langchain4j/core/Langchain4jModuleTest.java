package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.modules.langchain4j.core;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.MagicDynamicLangchain4jChatModelDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.MagicDynamicLangchain4jEmbeddingModelDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.MagicDynamicLangchain4jEmbeddingStoreDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.MagicDynamicLangchain4jLanguageModelDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.MagicDynamicLangchain4jStreamingChatModelDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.MagicDynamicLangchain4jStreamingLanguageModelDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.MagicDynamicLangchain4jTokenizerDataSource;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ssssssss.magicapi.core.annotation.MagicModule;

import java.io.Closeable;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;

/**
 * 脚本模块测试：模块只读取用、未配置时的中文异常、驱逐语义与关闭时清空。
 */
class Langchain4jModuleTest {

    private MagicDynamicLangchain4jChatModelDataSource chatModels;
    private MagicDynamicLangchain4jEmbeddingModelDataSource embeddingModels;
    private MagicDynamicLangchain4jEmbeddingStoreDataSource embeddingStores;
    private Langchain4jModule module;

    @BeforeEach
    void setUp() {
        chatModels = new MagicDynamicLangchain4jChatModelDataSource();
        embeddingModels = new MagicDynamicLangchain4jEmbeddingModelDataSource();
        embeddingStores = new MagicDynamicLangchain4jEmbeddingStoreDataSource();
        module = new Langchain4jModule(
                chatModels,
                new MagicDynamicLangchain4jStreamingChatModelDataSource(),
                new MagicDynamicLangchain4jLanguageModelDataSource(),
                new MagicDynamicLangchain4jStreamingLanguageModelDataSource(),
                embeddingModels,
                new MagicDynamicLangchain4jTokenizerDataSource(),
                embeddingStores);
    }

    @Test
    void shouldBeExposedAsLangchain4jModule() {
        MagicModule annotation = Langchain4jModule.class.getAnnotation(MagicModule.class);

        assertThat(annotation).as("脚本模块必须带 @MagicModule").isNotNull();
        assertThat(annotation.value()).isEqualTo("langchain4j");
    }

    @Test
    void shouldNotExposeProgrammaticBuilders() {
        for (Method method : Langchain4jModule.class.getDeclaredMethods()) {
            assertThat(method.getName()).as("不得再提供脚本侧构建入口").doesNotStartWith("build");
        }
    }

    @Test
    void shouldReadInstancesFromOwnTypeHolder() {
        ChatLanguageModel chatModel = mock(ChatLanguageModel.class);
        EmbeddingModel embeddingModel = mock(EmbeddingModel.class);
        chatModels.put("qwen-main", chatModel);
        embeddingModels.put("qwen-embed", embeddingModel);

        assertThat(module.getChatLanguageModel("qwen-main")).isSameAs(chatModel);
        assertThat(module.getEmbeddingModel("qwen-embed")).isSameAs(embeddingModel);
    }

    @Test
    void shouldFailWithChineseMessageForUnknownKey() {
        chatModels.put("qwen-main", mock(ChatLanguageModel.class));

        assertThatThrownBy(() -> module.getChatLanguageModel("absent"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("langchain4j-chat-model")
                .hasMessageContaining("absent")
                .hasMessageContaining("qwen-main");
    }

    @Test
    void shouldListConfiguredKeysInErrorMessage() {
        chatModels.put("openai-main", mock(ChatLanguageModel.class));
        chatModels.put("qwen-main", mock(ChatLanguageModel.class));

        assertThatThrownBy(() -> module.getChatLanguageModel("missing"))
                .hasMessageContaining("openai-main")
                .hasMessageContaining("qwen-main");
    }

    @Test
    void shouldEvictOnRemove() {
        chatModels.put("k", mock(ChatLanguageModel.class));

        module.remove("langchain4j-chat-model", "k");

        assertThat(chatModels.keys()).isEmpty();
        assertThatThrownBy(() -> module.getChatLanguageModel("k")).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shouldRejectUnknownTypeOnRemove() {
        assertThatThrownBy(() -> module.remove("unknown-type", "k"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldAllowReloadAfterRemove() {
        module.remove("langchain4j-embedding-model", "qwen-embed");
        EmbeddingModel reloaded = mock(EmbeddingModel.class);

        embeddingModels.put("qwen-embed", reloaded);

        assertThat(module.getEmbeddingModel("qwen-embed")).isSameAs(reloaded);
    }

    @Test
    void shouldClearAndCloseEverythingOnDestroy() throws Exception {
        AtomicInteger closed = new AtomicInteger();
        EmbeddingStore<TextSegment> store = closeableStore(closed);
        embeddingStores.put("milvus-main", store);
        chatModels.put("qwen-main", mock(ChatLanguageModel.class));
        embeddingModels.put("qwen-embed", mock(EmbeddingModel.class));

        module.destroy();

        assertThat(closed.get()).isEqualTo(1);
        assertThat(chatModels.keys()).isEmpty();
        assertThat(embeddingModels.keys()).isEmpty();
        assertThat(embeddingStores.keys()).isEmpty();
    }

    @SuppressWarnings("unchecked")
    private EmbeddingStore<TextSegment> closeableStore(AtomicInteger counter) throws Exception {
        EmbeddingStore<TextSegment> store = (EmbeddingStore<TextSegment>) mock(EmbeddingStore.class,
                withSettings().extraInterfaces(Closeable.class));
        doAnswer(invocation -> {
            counter.incrementAndGet();
            return null;
        }).when((Closeable) store).close();
        return store;
    }
}
