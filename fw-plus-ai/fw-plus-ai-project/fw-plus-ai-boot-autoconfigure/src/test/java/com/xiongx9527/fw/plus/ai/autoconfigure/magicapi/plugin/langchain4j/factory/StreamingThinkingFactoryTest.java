package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.anthropic.Langchain4jAnthropicModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.cohere.Langchain4jCohereModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.googlegenai.Langchain4jGoogleGenAiModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.mistralai.Langchain4jMistralAiModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.openai.Langchain4jOpenAiModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.watsonx.Langchain4jWatsonxModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.zhipu.Langchain4jZhipuModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.support.Langchain4jOptionsReader;
import dev.langchain4j.community.model.CohereStreamingChatModel;
import dev.langchain4j.community.model.client.CohereChatRequestParameters;
import dev.langchain4j.community.model.client.CohereThinkingType;
import dev.langchain4j.community.model.zhipu.ZhipuAiChatRequestParameters;
import dev.langchain4j.community.model.zhipu.ZhipuAiStreamingChatModel;
import dev.langchain4j.model.anthropic.AnthropicChatRequestParameters;
import dev.langchain4j.model.anthropic.AnthropicStreamingChatModel;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.google.genai.GoogleGenAiStreamingChatModel;
import dev.langchain4j.model.mistralai.MistralAiChatRequestParameters;
import dev.langchain4j.model.mistralai.MistralAiStreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatRequestParameters;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.model.watsonx.WatsonxChatRequestParameters;
import dev.langchain4j.model.watsonx.WatsonxStreamingChatModel;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class StreamingThinkingFactoryTest {

    @Test
    void anthropicStreamingOptionsAreProviderSpecificAndUnsetValuesKeepSdkDefaults() throws Exception {
        Map<String, Object> configured = options("api-key", "test", "model-name", "claude-3-7-sonnet",
                "thinking-type", "enabled", "thinking-budget-tokens", 256, "thinking-display", "concise",
                "send-thinking", true, "return-thinking", true);
        AnthropicStreamingChatModel model = new Langchain4jAnthropicModelFactory()
                .buildStreamingChatModel(reader("anthropic", configured), Collections.<ChatModelListener>emptyList());
        AnthropicChatRequestParameters parameters =
                (AnthropicChatRequestParameters) model.defaultRequestParameters();

        assertThat(parameters.thinkingType()).isEqualTo("enabled");
        assertThat(parameters.thinkingBudgetTokens()).isEqualTo(256);
        assertThat(parameters.sendThinking()).isTrue();
        assertThat(parameters.returnThinking()).isTrue();
        assertThat(field(model, "thinkingDisplay")).isEqualTo("concise");

        AnthropicStreamingChatModel defaults = new Langchain4jAnthropicModelFactory()
                .buildStreamingChatModel(reader("anthropic", options("api-key", "test", "model-name", "claude")),
                        Collections.<ChatModelListener>emptyList());
        AnthropicStreamingChatModel sdkDefaults =
                AnthropicStreamingChatModel.builder().apiKey("test").modelName("claude").build();
        assertThat(anthropicParameters(defaults).thinkingType())
                .isEqualTo(anthropicParameters(sdkDefaults).thinkingType());
        assertThat(anthropicParameters(defaults).thinkingBudgetTokens())
                .isEqualTo(anthropicParameters(sdkDefaults).thinkingBudgetTokens());
        assertThat(anthropicParameters(defaults).sendThinking())
                .isEqualTo(anthropicParameters(sdkDefaults).sendThinking());
        assertThat(anthropicParameters(defaults).returnThinking())
                .isEqualTo(anthropicParameters(sdkDefaults).returnThinking());
        assertThat(field(defaults, "thinkingDisplay")).isEqualTo(field(sdkDefaults, "thinkingDisplay"));
    }

    @Test
    void cohereStreamingOptionsAreProviderSpecificAndUnsetValuesKeepSdkDefaults() {
        CohereStreamingChatModel model = new Langchain4jCohereModelFactory()
                .buildStreamingChatModel(reader("cohere", options("api-key", "test", "thinking-type", "ENABLED",
                        "thinking-token-budget", 512)), Collections.<ChatModelListener>emptyList());
        CohereChatRequestParameters parameters =
                (CohereChatRequestParameters) model.defaultRequestParameters();

        assertThat(parameters.thinkingType()).isEqualTo(CohereThinkingType.ENABLED);
        assertThat(parameters.thinkingTokenBudget()).isEqualTo(512);

        CohereStreamingChatModel defaults = new Langchain4jCohereModelFactory()
                .buildStreamingChatModel(reader("cohere", options("api-key", "test")),
                        Collections.<ChatModelListener>emptyList());
        CohereStreamingChatModel sdkDefaults = CohereStreamingChatModel.builder().apiKey("test").build();
        assertThat(cohereParameters(defaults).thinkingType()).isEqualTo(cohereParameters(sdkDefaults).thinkingType());
        assertThat(cohereParameters(defaults).thinkingTokenBudget())
                .isEqualTo(cohereParameters(sdkDefaults).thinkingTokenBudget());
    }

    @Test
    void googleGenAiStreamingOptionsAreProviderSpecificAndUnsetValuesKeepSdkDefaults() throws Exception {
        GoogleGenAiStreamingChatModel model = new Langchain4jGoogleGenAiModelFactory()
                .buildStreamingChatModel(reader("google-genai", options("api-key", "test",
                        "model-name", "gemini-2.5-pro",
                        "thinking-budget", 1024, "thinking-level", "high", "include-thoughts", true,
                        "send-thinking", true, "return-thinking", true)),
                        Collections.<ChatModelListener>emptyList());

        assertThat(field(model, "thinkingBudget")).isEqualTo(1024);
        assertThat(field(model, "thinkingLevel")).isEqualTo("high");
        assertThat(field(model, "includeThoughts")).isEqualTo(true);
        assertThat(field(model, "sendThinking")).isEqualTo(true);
        assertThat(field(model, "returnThinking")).isEqualTo(true);

        GoogleGenAiStreamingChatModel defaults = new Langchain4jGoogleGenAiModelFactory()
                .buildStreamingChatModel(reader("google-genai", options(
                        "api-key", "test", "model-name", "gemini-2.5-pro")),
                        Collections.<ChatModelListener>emptyList());
        GoogleGenAiStreamingChatModel sdkDefaults = GoogleGenAiStreamingChatModel.builder()
                .apiKey("test").modelName("gemini-2.5-pro").build();
        assertThat(field(defaults, "thinkingBudget")).isEqualTo(field(sdkDefaults, "thinkingBudget"));
        assertThat(field(defaults, "thinkingLevel")).isEqualTo(field(sdkDefaults, "thinkingLevel"));
        assertThat(field(defaults, "includeThoughts")).isEqualTo(field(sdkDefaults, "includeThoughts"));
        assertThat(field(defaults, "sendThinking")).isEqualTo(field(sdkDefaults, "sendThinking"));
        assertThat(field(defaults, "returnThinking")).isEqualTo(field(sdkDefaults, "returnThinking"));
    }

    @Test
    void mistralStreamingOptionsAreProviderSpecificAndUnsetValuesKeepSdkDefaults() {
        MistralAiStreamingChatModel model = new Langchain4jMistralAiModelFactory()
                .buildStreamingChatModel(reader("mistral-ai", options("api-key", "test", "model-name", "mistral-large",
                        "send-thinking", true, "return-thinking", true)),
                        Collections.<ChatModelListener>emptyList());
        MistralAiChatRequestParameters parameters =
                (MistralAiChatRequestParameters) model.defaultRequestParameters();

        assertThat(parameters.sendThinking()).isTrue();
        assertThat(parameters.returnThinking()).isTrue();

        MistralAiStreamingChatModel defaults = new Langchain4jMistralAiModelFactory()
                .buildStreamingChatModel(reader("mistral-ai", options("api-key", "test", "model-name", "mistral-large")),
                        Collections.<ChatModelListener>emptyList());
        MistralAiStreamingChatModel sdkDefaults =
                MistralAiStreamingChatModel.builder().apiKey("test").modelName("mistral-large").build();
        assertThat(mistralParameters(defaults).sendThinking())
                .isEqualTo(mistralParameters(sdkDefaults).sendThinking());
        assertThat(mistralParameters(defaults).returnThinking())
                .isEqualTo(mistralParameters(sdkDefaults).returnThinking());
    }

    @Test
    void openAiStreamingOptionsAreProviderSpecificAndUnsetValuesKeepSdkDefaults() throws Exception {
        OpenAiStreamingChatModel model = new Langchain4jOpenAiModelFactory()
                .buildStreamingChatModel(reader("openai", options("api-key", "test", "model-name", "o3",
                        "reasoning-effort", "high", "send-thinking", true, "return-thinking", true,
                        "thinking-field-name", "reasoning_content")),
                        Collections.<ChatModelListener>emptyList());
        OpenAiChatRequestParameters parameters = (OpenAiChatRequestParameters) model.defaultRequestParameters();

        assertThat(parameters.reasoningEffort()).isEqualTo("high");
        assertThat(field(model, "sendThinking")).isEqualTo(true);
        assertThat(field(model, "returnThinking")).isEqualTo(true);
        assertThat(field(model, "thinkingFieldName")).isEqualTo("reasoning_content");

        OpenAiStreamingChatModel defaults = new Langchain4jOpenAiModelFactory()
                .buildStreamingChatModel(reader("openai", options("api-key", "test", "model-name", "o3")),
                        Collections.<ChatModelListener>emptyList());
        OpenAiStreamingChatModel sdkDefaults =
                OpenAiStreamingChatModel.builder().apiKey("test").modelName("o3").build();
        assertThat(((OpenAiChatRequestParameters) defaults.defaultRequestParameters()).reasoningEffort())
                .isEqualTo(((OpenAiChatRequestParameters) sdkDefaults.defaultRequestParameters()).reasoningEffort());
        assertThat(field(defaults, "sendThinking")).isEqualTo(field(sdkDefaults, "sendThinking"));
        assertThat(field(defaults, "returnThinking")).isEqualTo(field(sdkDefaults, "returnThinking"));
        assertThat(field(defaults, "thinkingFieldName")).isEqualTo(field(sdkDefaults, "thinkingFieldName"));
    }

    @Test
    void watsonxStreamingThinkingOptionIsProviderSpecificAndUnsetValueKeepsSdkDefault() {
        Map<String, Object> required = options("base-url", "https://example.invalid", "api-key", "test",
                "project-id", "project", "model-name", "ibm/granite-3-8b-instruct");
        WatsonxStreamingChatModel model = new Langchain4jWatsonxModelFactory()
                .buildStreamingChatModel(reader("watsonx", options(required, "thinking", true)),
                        Collections.<ChatModelListener>emptyList());
        WatsonxChatRequestParameters parameters =
                (WatsonxChatRequestParameters) model.defaultRequestParameters();
        assertThat(parameters.thinking().enabled()).isTrue();

        WatsonxStreamingChatModel defaults = new Langchain4jWatsonxModelFactory()
                .buildStreamingChatModel(reader("watsonx", required), Collections.<ChatModelListener>emptyList());
        WatsonxStreamingChatModel sdkDefaults = WatsonxStreamingChatModel.builder()
                .baseUrl("https://example.invalid").apiKey("test").projectId("project")
                .modelName("ibm/granite-3-8b-instruct").build();
        assertThat(watsonxParameters(defaults).thinking()).isEqualTo(watsonxParameters(sdkDefaults).thinking());
    }

    @Test
    void zhipuStreamingOptionsAreProviderSpecificAndUnsetValuesKeepSdkDefaults() {
        ZhipuAiStreamingChatModel model = new Langchain4jZhipuModelFactory()
                .buildStreamingChatModel(reader("zhipu-ai",
                        options("api-key", "test", "model", "glm-4",
                                "thinking-type", "enabled", "clear-thinking", true)),
                        Collections.<ChatModelListener>emptyList());
        dev.langchain4j.community.model.zhipu.chat.Thinking thinking =
                zhipuParameters(model).thinking();

        assertThat(thinking.getType()).isEqualTo("enabled");
        assertThat(thinking.clearThinking()).isTrue();

        ZhipuAiStreamingChatModel defaults = new Langchain4jZhipuModelFactory()
                .buildStreamingChatModel(reader("zhipu-ai", options("api-key", "test", "model", "glm-4")),
                        Collections.<ChatModelListener>emptyList());
        ZhipuAiStreamingChatModel sdkDefaults =
                ZhipuAiStreamingChatModel.builder().apiKey("test").model("glm-4").build();
        assertThat(zhipuParameters(defaults).thinking()).isEqualTo(zhipuParameters(sdkDefaults).thinking());
    }

    private static AnthropicChatRequestParameters anthropicParameters(AnthropicStreamingChatModel model) {
        return (AnthropicChatRequestParameters) model.defaultRequestParameters();
    }

    private static CohereChatRequestParameters cohereParameters(CohereStreamingChatModel model) {
        return (CohereChatRequestParameters) model.defaultRequestParameters();
    }

    private static MistralAiChatRequestParameters mistralParameters(MistralAiStreamingChatModel model) {
        return (MistralAiChatRequestParameters) model.defaultRequestParameters();
    }

    private static WatsonxChatRequestParameters watsonxParameters(WatsonxStreamingChatModel model) {
        return (WatsonxChatRequestParameters) model.defaultRequestParameters();
    }

    private static ZhipuAiChatRequestParameters zhipuParameters(ZhipuAiStreamingChatModel model) {
        return (ZhipuAiChatRequestParameters) model.defaultRequestParameters();
    }

    private static Langchain4jOptionsReader reader(String provider, Map<String, Object> options) {
        return new Langchain4jOptionsReader("langchain4j-streaming-chat-model", provider, options);
    }

    private static Map<String, Object> options(Object... pairs) {
        Map<String, Object> options = new LinkedHashMap<String, Object>();
        for (int index = 0; index < pairs.length; index += 2) {
            options.put(String.valueOf(pairs[index]), pairs[index + 1]);
        }
        return options;
    }

    private static Map<String, Object> options(Map<String, Object> base, Object... pairs) {
        Map<String, Object> options = new LinkedHashMap<String, Object>(base);
        for (int index = 0; index < pairs.length; index += 2) {
            options.put(String.valueOf(pairs[index]), pairs[index + 1]);
        }
        return options;
    }

    private static Object field(Object target, String name) throws Exception {
        Class<?> type = target.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                return field.get(target);
            } catch (NoSuchFieldException e) {
                type = type.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }
}
