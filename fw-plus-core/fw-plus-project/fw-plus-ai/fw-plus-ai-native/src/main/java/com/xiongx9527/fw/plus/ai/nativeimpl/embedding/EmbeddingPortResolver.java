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
            case COHERE:
                return createCohere(config);
            case AZURE_OPENAI:
                return createAzureOpenAi(config);
            case TEI:
                return createTei(config);
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
                return newAzureOpenAi(config);
            case COHERE:
                return newCohere(config);
            case HUGGING_FACE:
                return newTei(config);
            case VOYAGE:
            case ZHIPU_AI:
                // Voyage / 智谱 向量接口与 OpenAI 兼容（/v1/embeddings + Bearer 鉴权）
                return newOpenAiCompatible(config);
            default:
                throw new IllegalArgumentException("unsupported embedding vendor: " + vendor);
        }
    }

    public static EmbeddingPort createOpenAiCompatible(EmbeddingModelConfig config) {
        requireProtocol(config, EmbeddingProtocol.OPENAI_COMPATIBLE);
        return newOpenAiCompatible(config);
    }

    public static EmbeddingPort createCohere(EmbeddingModelConfig config) {
        requireProtocol(config, EmbeddingProtocol.COHERE);
        return newCohere(config);
    }

    public static EmbeddingPort createAzureOpenAi(EmbeddingModelConfig config) {
        requireProtocol(config, EmbeddingProtocol.AZURE_OPENAI);
        return newAzureOpenAi(config);
    }

    public static EmbeddingPort createTei(EmbeddingModelConfig config) {
        requireProtocol(config, EmbeddingProtocol.TEI);
        return newTei(config);
    }

    private static EmbeddingPort newOpenAiCompatible(EmbeddingModelConfig config) {
        return new OpenAiCompatibleEmbeddingPort(config.getBaseUrl(), config.getApiKey(),
                config.getDefaultModel(), config.getConnectTimeoutMillis(), config.getReadTimeoutMillis());
    }

    private static EmbeddingPort newCohere(EmbeddingModelConfig config) {
        return new CohereEmbeddingPort(config.getBaseUrl(), config.getApiKey(),
                config.getDefaultModel(), config.getConnectTimeoutMillis(), config.getReadTimeoutMillis(), config.getProperties());
    }

    private static EmbeddingPort newAzureOpenAi(EmbeddingModelConfig config) {
        return new AzureOpenAiEmbeddingPort(config.getBaseUrl(), config.getApiKey(),
                config.getDefaultModel(), config.getConnectTimeoutMillis(), config.getReadTimeoutMillis(), config.getProperties());
    }

    private static EmbeddingPort newTei(EmbeddingModelConfig config) {
        return new TeiEmbeddingPort(config.getBaseUrl(), config.getApiKey(),
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
