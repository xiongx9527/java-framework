package com.xiongx9527.fw.plus.ai.nativeimpl.embedding;

import com.xiongx9527.fw.plus.ai.base.model.EmbeddingPort;

/** 创建向量模型端口；同时配置厂商和协议时，始终以协议为准。 */
public final class EmbeddingPortResolver {
    private EmbeddingPortResolver() { }

    /**
     * 创建端口。厂商与协议至少配置其一；二者皆有时以协议为准，协议缺失时回退到厂商路由。
     */
    public static EmbeddingPort create(EmbeddingModelConfig config) {
        requireConfig(config);
        if (config.getProtocol() != null) {
            return createByProtocol(config);
        }
        if (config.getVendor() == null) {
            throw new IllegalArgumentException("embedding protocol or vendor is required");
        }
        return createByVendor(config.getVendor(), config);
    }

    private static EmbeddingPort createByProtocol(EmbeddingModelConfig config) {
        switch (config.getProtocol()) {
            case OPENAI_COMPATIBLE:
                return createOpenAiCompatible(config);
            default:
                throw new IllegalArgumentException("unsupported embedding protocol: " + config.getProtocol());
        }
    }

    private static EmbeddingPort createByVendor(EmbeddingVendor vendor, EmbeddingModelConfig config) {
        switch (vendor) {
            case OPENAI:
            case DASHSCOPE:
            case QWEN:
            case OLLAMA:
            case QIANFAN:
            case LOCAL_AI:
                return newOpenAiCompatible(config);
            case AZURE_OPENAI:
                throw new IllegalArgumentException("embedding vendor " + vendor
                        + " requires an explicit protocol; no native Azure embedding port is available");
            case HUGGING_FACE:
            case COHERE:
            case VOYAGE:
            case ZHIPU_AI:
                throw new IllegalArgumentException("embedding vendor " + vendor
                        + " requires an explicit protocol; no native vendor-specific port is available");
            default:
                throw new IllegalArgumentException("unsupported embedding vendor: " + vendor);
        }
    }

    public static EmbeddingPort createOpenAiCompatible(EmbeddingModelConfig config) {
        requireProtocol(config, EmbeddingProtocol.OPENAI_COMPATIBLE);
        return newOpenAiCompatible(config);
    }

    private static EmbeddingPort newOpenAiCompatible(EmbeddingModelConfig config) {
        return new OpenAiCompatibleEmbeddingPort(config.getBaseUrl(), config.getApiKey(),
                config.getDefaultModel(), config.getConnectTimeoutMillis(), config.getReadTimeoutMillis());
    }

    private static void requireProtocol(EmbeddingModelConfig config, EmbeddingProtocol... supportedProtocols) {
        requireConfig(config);
        for (EmbeddingProtocol supportedProtocol : supportedProtocols) {
            if (config.getProtocol() == supportedProtocol) {
                return;
            }
        }
        throw new IllegalArgumentException("protocol " + config.getProtocol()
                + " is not supported by this vendor factory");
    }

    private static void requireConfig(EmbeddingModelConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("embedding model config is required");
        }
    }

}
