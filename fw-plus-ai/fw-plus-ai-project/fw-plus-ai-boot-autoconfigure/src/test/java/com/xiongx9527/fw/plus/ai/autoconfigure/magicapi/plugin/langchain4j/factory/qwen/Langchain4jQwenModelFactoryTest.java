package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.qwen;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.support.Langchain4jOptionsReader;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 通义千问（dashscope）代表工厂测试：六类能力的构建、qwen 特有选项与缺参/类型报错。
 */
class Langchain4jQwenModelFactoryTest {

    private final Langchain4jQwenModelFactory factory = new Langchain4jQwenModelFactory();

    @Test
    void shouldBuildEverySupportedCapability() {
        Langchain4jOptionsReader reader = reader(options(
                "api-key", "sk-test", "enable-search", true, "top-k", 20, "stops", "a, b",
                "repetition-penalty", 1.1d, "max-tokens", 2048));

        assertThat(factory.buildChatModel(reader)).isNotNull();
        assertThat(factory.buildStreamingChatModel(reader)).isNotNull();
        assertThat(factory.buildLanguageModel(reader)).isNotNull();
        assertThat(factory.buildStreamingLanguageModel(reader)).isNotNull();
        assertThat(factory.buildEmbeddingModel(reader)).isNotNull();
        assertThat(factory.buildTokenizer(reader)).isNotNull();
    }

    @Test
    void shouldBuildWithRequiredOptionsOnly() {
        Langchain4jOptionsReader reader = reader(options("api-key", "sk-test"));

        assertThat(factory.buildChatModel(reader)).isNotNull();
    }

    @Test
    void shouldReportMissingApiKeyWithFullPath() {
        assertThatThrownBy(() -> factory.buildChatModel(reader(options("model-name", "qwen-plus"))))
                .hasMessageContaining("langchain4j-chat-model[qwen-main].options.api-key");
    }

    @Test
    void shouldRejectNonBooleanEnableSearch() {
        assertThatThrownBy(() -> factory.buildChatModel(reader(options(
                "api-key", "sk-test", "enable-search", "maybe"))))
                .hasMessageContaining(".options.enable-search");
    }

    @Test
    void shouldRejectNonNumericTopP() {
        assertThatThrownBy(() -> factory.buildChatModel(reader(options("api-key", "sk-test", "top-p", "abc"))))
                .hasMessageContaining(".options.top-p");
    }

    @Test
    void shouldRejectUnsupportedResourceType() {
        assertThatThrownBy(() -> factory.buildEmbeddingStore(reader(options("api-key", "sk-test")), null))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    private static Langchain4jOptionsReader reader(Map<String, Object> options) {
        return new Langchain4jOptionsReader("chat-model", "qwen-main", options);
    }

    private static Map<String, Object> options(Object... pairs) {
        Map<String, Object> options = new LinkedHashMap<String, Object>();
        for (int index = 0; index < pairs.length; index += 2) {
            options.put(String.valueOf(pairs[index]), pairs[index + 1]);
        }
        return options;
    }
}
