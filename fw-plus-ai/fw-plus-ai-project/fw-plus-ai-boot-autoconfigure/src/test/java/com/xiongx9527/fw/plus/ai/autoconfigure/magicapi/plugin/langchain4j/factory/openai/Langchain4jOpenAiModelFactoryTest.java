package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.openai;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.support.Langchain4jOptionsReader;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * OpenAI 代表工厂测试：builder 链参数透传、缺省语义（未配置的可空参数不得被置为 0）与缺参报错。
 */
class Langchain4jOpenAiModelFactoryTest {

    private final Langchain4jOpenAiModelFactory factory = new Langchain4jOpenAiModelFactory();

    @Test
    void shouldPassThroughConfiguredParameters() {
        OpenAiChatModel model = factory.buildChatModel(reader(options(
                "api-key", "sk-test", "model-name", "gpt-4o", "temperature", 0.3d, "top-p", 0.5d)),
                Collections.emptyList());

        assertThat(model.defaultRequestParameters().temperature()).isEqualTo(0.3d);
        assertThat(model.defaultRequestParameters().topP()).isEqualTo(0.5d);
    }

    @Test
    void shouldKeepUnsetOptionalParametersUnset() {
        OpenAiChatModel model = factory.buildChatModel(reader(options("api-key", "sk-test", "model-name", "gpt-4o")),
                Collections.emptyList());

        assertThat(model.defaultRequestParameters().topP()).as("未配置的 top-p 不应被置为 0").isNull();
        assertThat(model.defaultRequestParameters().presencePenalty()).isNull();
        assertThat(model.defaultRequestParameters().maxOutputTokens()).isNull();
    }

    @Test
    void shouldApplyDefaultTemperatureWhenAbsent() {
        OpenAiChatModel model = factory.buildChatModel(reader(options("api-key", "sk-test", "model-name", "gpt-4o")),
                Collections.emptyList());

        assertThat(model.defaultRequestParameters().temperature()).isEqualTo(0.7d);
    }

    @Test
    void shouldPassListenersToBuiltModel() {
        ChatModelListener listener = mock(ChatModelListener.class);

        OpenAiChatModel model = factory.buildChatModel(reader(options("api-key", "sk-test", "model-name", "gpt-4o")),
                Collections.singletonList(listener));

        assertThat(model.listeners()).hasSize(1).first().isEqualTo(listener);
    }

    @Test
    void shouldReportMissingRequiredWithFullPath() {
        assertThatThrownBy(() -> factory.buildChatModel(reader(options("model-name", "gpt-4o")),
                Collections.emptyList()))
                .hasMessageContaining("langchain4j-chat-model[openai-main].options.api-key");
    }

    @Test
    void shouldRejectNonNumericTemperature() {
        assertThatThrownBy(() -> factory.buildChatModel(reader(options(
                "api-key", "sk-test", "model-name", "gpt-4o", "temperature", "abc")), Collections.emptyList()))
                .hasMessageContaining(".options.temperature");
    }

    @Test
    void shouldBuildEverySupportedCapability() {
        Langchain4jOptionsReader reader = reader(options("api-key", "sk-test", "model-name", "gpt-4o"));

        assertThat(factory.buildChatModel(reader, Collections.emptyList())).isNotNull();
        assertThat(factory.buildStreamingChatModel(reader, Collections.emptyList())).isNotNull();
        assertThat(factory.buildEmbeddingModel(reader)).isNotNull();
    }

    @Test
    void shouldRejectUnsupportedResourceType() {
        assertThatThrownBy(() -> factory.buildEmbeddingStore(reader(options("api-key", "sk-test")), null))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    private static Langchain4jOptionsReader reader(Map<String, Object> options) {
        return new Langchain4jOptionsReader("langchain4j-chat-model", "openai-main", options);
    }

    private static Map<String, Object> options(Object... pairs) {
        Map<String, Object> options = new LinkedHashMap<String, Object>();
        for (int index = 0; index < pairs.length; index += 2) {
            options.put(String.valueOf(pairs[index]), pairs[index + 1]);
        }
        return options;
    }
}
