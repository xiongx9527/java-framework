package com.xiongx9527.fw.plus.ai.nativeimpl.llm;

import com.xiongx9527.fw.plus.ai.base.model.LlmPort;

/** 创建 LLM 端口；同时配置厂商和协议时，始终以协议为准。 */
public final class LlmPortResolver {
    private LlmPortResolver() { }

    public static LlmPort create(LlmModelConfig config) {
        requireConfig(config);
        if (config.getProtocol() == null) {
            throw new IllegalArgumentException("llm protocol is required when vendor is not provided");
        }
        return createByProtocol(config);
    }

    /**
     * 创建端口。协议存在时忽略厂商；只有协议未配置时，才使用厂商作为默认路由。
     */
    public static LlmPort create(LlmVendor vendor, LlmModelConfig config) {
        requireConfig(config);
        if (config.getProtocol() != null) {
            return createByProtocol(config);
        }
        if (vendor == null) {
            throw new IllegalArgumentException("llm vendor is required");
        }
        return createByVendor(vendor, config);
    }

    private static LlmPort createByProtocol(LlmModelConfig config) {
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

    private static LlmPort createByVendor(LlmVendor vendor, LlmModelConfig config) {
        switch (vendor) {
            case OPENAI:
            case DASHSCOPE:
            case GITHUB_MODELS:
            case OLLAMA:
            case QIANFAN:
            case LOCAL_AI:
                return new OpenAiChatCompletionsLlmPort(
                        config.getBaseUrl(), config.getApiKey(), config.getDefaultModel());
            case AZURE_OPENAI:
                return new AzureOpenAiLlmPort(config.getBaseUrl(), config.getApiKey(),
                        config.getDefaultModel(), config.getApiVersion());
            case ANTHROPIC:
                return new AnthropicLlmPort(config.getBaseUrl(), config.getApiKey(), config.getDefaultModel());
            case GEMINI:
            case GOOGLE_AI_GEMINI:
                return new GeminiLlmPort(config.getBaseUrl(), config.getApiKey(),
                        config.getDefaultModel(), config.getApiVersion());
            case VERTEX_AI_GEMINI:
                throw new IllegalArgumentException("Vertex AI Gemini requires an explicit supported protocol; "
                        + "the native Gemini API port does not implement Vertex AI authentication");
            case HUGGING_FACE:
            case ZHIPU_AI:
            case COHERE:
                throw new IllegalArgumentException("llm vendor " + vendor
                        + " requires an explicit protocol; no native vendor-specific port is available");
            default:
                throw new IllegalArgumentException("unsupported llm vendor: " + vendor);
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
