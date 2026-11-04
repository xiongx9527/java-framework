package com.xiongx9527.fw.plus.ai.nativeimpl.llm;

import com.xiongx9527.fw.plus.ai.base.model.LlmPort;

/** 按厂商创建 LLM 端口，并在厂商内部按明确配置的协议选择 API 实现。 */
public final class LlmPortResolver {
    private LlmPortResolver() { }

    public static LlmPort create(LlmModelConfig config) {
        requireConfig(config);
        switch (config.getProtocol()) {
            case OPENAI_CHAT_COMPLETIONS:
            case OPENAI_RESPONSES:
                return createOpenAi(config);
            case AZURE_OPENAI_CHAT_COMPLETIONS:
                return createAzureOpenAi(config);
            case ANTHROPIC_MESSAGES:
                return createAnthropic(config);
            case GEMINI:
                return createGemini(config);
            default:
                throw new IllegalArgumentException("unsupported llm protocol: " + config.getProtocol());
        }
    }

    public static LlmPort createOpenAi(LlmModelConfig config) {
        requireProtocol(config, LlmProtocol.OPENAI_CHAT_COMPLETIONS, LlmProtocol.OPENAI_RESPONSES);
        if (config.getProtocol() == LlmProtocol.OPENAI_RESPONSES) {
            return new OpenAiResponsesLlmPort(config.getBaseUrl(), config.getApiKey(), config.getDefaultModel());
        }
        return new OpenAiChatCompletionsLlmPort(config.getBaseUrl(), config.getApiKey(), config.getDefaultModel());
    }

    public static LlmPort createAzureOpenAi(LlmModelConfig config) {
        requireProtocol(config, LlmProtocol.AZURE_OPENAI_CHAT_COMPLETIONS);
        return new AzureOpenAiLlmPort(config.getBaseUrl(), config.getApiKey(),
                config.getDefaultModel(), config.getApiVersion());
    }

    public static LlmPort createAnthropic(LlmModelConfig config) {
        requireProtocol(config, LlmProtocol.ANTHROPIC_MESSAGES);
        return new AnthropicLlmPort(config.getBaseUrl(), config.getApiKey(), config.getDefaultModel());
    }

    public static LlmPort createGemini(LlmModelConfig config) {
        requireProtocol(config, LlmProtocol.GEMINI);
        return new GeminiLlmPort(config.getBaseUrl(), config.getApiKey(),
                config.getDefaultModel(), config.getApiVersion());
    }

    private static void requireProtocol(LlmModelConfig config, LlmProtocol... supportedProtocols) {
        requireConfig(config);
        for (LlmProtocol supportedProtocol : supportedProtocols) {
            if (config.getProtocol() == supportedProtocol) {
                return;
            }
        }
        throw new IllegalArgumentException("protocol " + config.getProtocol()
                + " is not supported by this vendor factory");
    }

    private static void requireConfig(LlmModelConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("llm model config is required");
        }
    }
}
