package com.xiongx9527.fw.plus.ai.nativeimpl.llm;

import com.xiongx9527.fw.plus.ai.base.model.LlmPort;

/** 创建 LLM 端口；同时配置厂商和协议时，始终以协议为准。 */
public final class LlmPortResolver {
    private LlmPortResolver() { }

    /**
     * 创建端口。厂商与协议至少配置其一；二者皆有时以协议为准，协议缺失时回退到厂商路由。
     */
    public static LlmPort create(LlmModelConfig config) {
        return resolve(config, null);
    }

    /**
     * 创建端口。显式传入厂商（仅当配置内未携带厂商时生效）；协议存在时忽略厂商。
     */
    public static LlmPort create(LlmVendor vendor, LlmModelConfig config) {
        return resolve(config, vendor);
    }

    private static LlmPort resolve(LlmModelConfig config, LlmVendor explicitVendor) {
        requireConfig(config);
        if (config.getProtocol() != null) {
            return createByProtocol(config);
        }
        LlmVendor vendor = explicitVendor != null ? explicitVendor : config.getVendor();
        if (vendor == null) {
            throw new IllegalArgumentException("llm protocol or vendor is required");
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
            case QWEN:
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
