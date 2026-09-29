package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.Langchain4jModelFactoryConfiguration;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jChatModelInfo;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jModelTypes;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.support.Langchain4jOptionsReader;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * provider 短名表与工厂分发测试：短名表由已注册工厂的能力探测生成，与存储校验同源。
 */
class Langchain4jModelFactoryRegistryTest {

    private static Langchain4jModelFactoryRegistry registry;

    @BeforeAll
    static void registerDefaultFactories() throws Exception {
        registry = new Langchain4jModelFactoryRegistry();
        for (ModelFactory factory : defaultFactories()) {
            registry.register(factory);
        }
    }

    @Test
    void shouldDeclareAtLeastOneProviderForEveryResourceType() {
        for (Langchain4jModelTypes type : Langchain4jModelTypes.values()) {
            assertThat(registry.providers(type)).as("%s 至少应有一个 provider", type.type()).isNotEmpty();
        }
    }

    @Test
    void shouldResolveEveryDeclaredShortName() {
        for (Langchain4jModelTypes type : Langchain4jModelTypes.values()) {
            for (String provider : registry.providers(type)) {
                assertThat(registry.supports(type, provider))
                        .as("%s 声明的短名 %s 应可构建", type.type(), provider).isTrue();
            }
        }
    }

    @Test
    void shouldRejectShortNameOutsideTheTable() {
        assertThat(registry.supports(Langchain4jModelTypes.CHAT_MODEL, "no-such-provider")).isFalse();

        assertThatThrownBy(() -> registry.build(Langchain4jModelTypes.CHAT_MODEL, chatInfo("no-such-provider")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("可用 provider");
    }

    @Test
    void shouldKeepShortNameSetsIndependentPerType() {
        assertThat(registry.providers(Langchain4jModelTypes.CHAT_MODEL))
                .contains("openai", "qwen")
                .doesNotContain("in-memory");
        assertThat(registry.providers(Langchain4jModelTypes.EMBEDDING_STORE))
                .contains("in-memory", "milvus")
                .doesNotContain("openai");
        assertThat(registry.providers(Langchain4jModelTypes.TOKENIZER))
                .contains("openai", "qwen")
                .doesNotContain("in-memory", "milvus");
    }

    @Test
    void shouldBuildThroughRegisteredFactory() {
        Langchain4jChatModelInfo info = chatInfo("openai");
        info.setKey("openai-main");
        Map<String, Object> options = new LinkedHashMap<String, Object>();
        options.put("api-key", "sk-test");
        options.put("model-name", "gpt-4o");
        info.setOptions(options);

        assertThat(registry.build(Langchain4jModelTypes.CHAT_MODEL, info)).isInstanceOf(ChatLanguageModel.class);
    }

    @Test
    void shouldPassListenersToChatModelFactory() {
        ChatModelListener listener = mock(ChatModelListener.class);
        Langchain4jModelFactoryRegistry withListeners =
                new Langchain4jModelFactoryRegistry(Collections.singletonList(listener));
        CapturingFactory capturing = new CapturingFactory();
        withListeners.register(capturing);

        withListeners.build(Langchain4jModelTypes.CHAT_MODEL, chatInfo("capturing"));

        assertThat(capturing.listeners).as("全局 ChatModelListener 应下发给 provider").containsExactly(listener);
    }

    @Test
    void shouldRejectUnbuildableValueWithTypeAndKey() {
        Langchain4jChatModelInfo info = chatInfo("openai");
        info.setKey("bad-key");
        Map<String, Object> options = new LinkedHashMap<String, Object>();
        options.put("model-name", "gpt-4o");
        info.setOptions(options);

        assertThatThrownBy(() -> registry.build(Langchain4jModelTypes.CHAT_MODEL, info))
                .hasMessageContaining("langchain4j-chat-model[bad-key].options.api-key");
    }

    private static ModelFactory[] defaultFactories() throws Exception {
        Method method = Langchain4jModelFactoryConfiguration.class.getDeclaredMethod("defaultFactories");
        method.setAccessible(true);
        return (ModelFactory[]) method.invoke(null);
    }

    /** 记录收到的 listeners 的测试替身工厂 */
    private static final class CapturingFactory implements ModelFactory {

        private List<ChatModelListener> listeners;

        @Override
        public String provider() {
            return "capturing";
        }

        @Override
        public ChatLanguageModel buildChatModel(Langchain4jOptionsReader o, List<ChatModelListener> listeners) {
            this.listeners = listeners;
            return mock(ChatLanguageModel.class);
        }
    }

    private static Langchain4jChatModelInfo chatInfo(String provider) {
        Langchain4jChatModelInfo info = new Langchain4jChatModelInfo();
        info.setProvider(provider);
        info.setEnabled(true);
        return info;
    }
}
