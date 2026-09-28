package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.openai;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.support.Langchain4jOptionsReader;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * OpenAI 代表工厂测试：builder 链参数透传、缺省语义（未配置的可空参数不得被置为 0）与缺参报错。
 */
class Langchain4jOpenAiModelFactoryTest {

    private final Langchain4jOpenAiModelFactory factory = new Langchain4jOpenAiModelFactory();

    @Test
    void shouldPassThroughConfiguredParameters() throws Exception {
        OpenAiChatModel model = factory.buildChatModel(reader(options(
                "api-key", "sk-test", "model-name", "gpt-4o", "temperature", 0.3d, "top-p", 0.5d)));

        assertThat(field(model, "temperature")).isEqualTo(0.3d);
        assertThat(field(model, "topP")).isEqualTo(0.5d);
    }

    @Test
    void shouldKeepUnsetOptionalParametersUnset() throws Exception {
        OpenAiChatModel model = factory.buildChatModel(reader(options("api-key", "sk-test", "model-name", "gpt-4o")));

        assertThat(field(model, "topP")).as("未配置的 top-p 不应被置为 0").isNull();
        assertThat(field(model, "presencePenalty")).isNull();
        assertThat(field(model, "maxTokens")).isNull();
    }

    @Test
    void shouldApplyDefaultTemperatureWhenAbsent() throws Exception {
        OpenAiChatModel model = factory.buildChatModel(reader(options("api-key", "sk-test", "model-name", "gpt-4o")));

        assertThat(field(model, "temperature")).isEqualTo(0.7d);
    }

    @Test
    void shouldReportMissingRequiredWithFullPath() {
        assertThatThrownBy(() -> factory.buildChatModel(reader(options("model-name", "gpt-4o"))))
                .hasMessageContaining("langchain4j-chat-model[openai-main].options.api-key");
    }

    @Test
    void shouldRejectNonNumericTemperature() {
        assertThatThrownBy(() -> factory.buildChatModel(reader(options(
                "api-key", "sk-test", "model-name", "gpt-4o", "temperature", "abc"))))
                .hasMessageContaining(".options.temperature");
    }

    @Test
    void shouldBuildEverySupportedCapability() {
        Langchain4jOptionsReader reader = reader(options("api-key", "sk-test", "model-name", "gpt-4o"));

        assertThat(factory.buildChatModel(reader)).isNotNull();
        assertThat(factory.buildStreamingChatModel(reader)).isNotNull();
        assertThat(factory.buildLanguageModel(reader)).isNotNull();
        assertThat(factory.buildStreamingLanguageModel(reader)).isNotNull();
        assertThat(factory.buildEmbeddingModel(reader)).isNotNull();
        assertThat(factory.buildTokenizer(reader)).isNotNull();
    }

    @Test
    void shouldRejectUnsupportedResourceType() {
        assertThatThrownBy(() -> factory.buildEmbeddingStore(reader(options("api-key", "sk-test")), null))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    private static Langchain4jOptionsReader reader(Map<String, Object> options) {
        return new Langchain4jOptionsReader("chat-model", "openai-main", options);
    }

    private static Map<String, Object> options(Object... pairs) {
        Map<String, Object> options = new LinkedHashMap<String, Object>();
        for (int index = 0; index < pairs.length; index += 2) {
            options.put(String.valueOf(pairs[index]), pairs[index + 1]);
        }
        return options;
    }

    private static Object field(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }
}
