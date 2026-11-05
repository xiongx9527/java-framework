package com.xiongx9527.fw.plus.ai.nativeimpl.rerank;

import com.xiongx9527.fw.plus.ai.base.model.RerankPort;

/** 创建重排模型端口；同时配置厂商和协议时，始终以协议为准。 */
public final class RerankPortResolver {
    private RerankPortResolver() { }

    /**
     * 创建端口。厂商与协议至少配置其一；二者皆有时以协议为准，协议缺失时回退到厂商路由。
     */
    public static RerankPort create(RerankModelConfig config) {
        requireConfig(config);
        if (config.getProtocol() != null) {
            return createByProtocol(config);
        }
        if (config.getVendor() == null) {
            throw new IllegalArgumentException("rerank protocol or vendor is required");
        }
        return createByVendor(config);
    }

    private static RerankPort createByProtocol(RerankModelConfig config) {
        switch (config.getProtocol()) {
            case TEI:
            case COHERE:
                return createWithStyle(config, styleOf(config.getProtocol()));
            default:
                throw new IllegalArgumentException("unsupported rerank protocol: " + config.getProtocol());
        }
    }

    private static RerankPort createByVendor(RerankModelConfig config) {
        switch (config.getVendor()) {
            case COHERE:
            case JINA:
            case VOYAGE:
                return createWithStyle(config, RerankModelPort.Style.COHERE);
            case HUGGING_FACE:
                return createWithStyle(config, RerankModelPort.Style.TEI);
            default:
                throw new IllegalArgumentException("rerank vendor " + config.getVendor()
                        + " requires an explicit protocol; no native vendor-specific mapping is available");
        }
    }

    private static RerankPort createWithStyle(RerankModelConfig config, RerankModelPort.Style style) {
        int connect = config.getConnectTimeoutMillis() != null ? config.getConnectTimeoutMillis() : 5000;
        int read = config.getReadTimeoutMillis() != null ? config.getReadTimeoutMillis() : 10000;
        return new RerankModelPort(config.getEndpoint(), config.getApiKey(),
                config.getDefaultModel(), style, connect, read);
    }

    private static RerankModelPort.Style styleOf(RerankProtocol protocol) {
        switch (protocol) {
            case TEI:
                return RerankModelPort.Style.TEI;
            case COHERE:
                return RerankModelPort.Style.COHERE;
            default:
                throw new IllegalArgumentException("unsupported rerank protocol: " + protocol);
        }
    }

    private static void requireConfig(RerankModelConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("rerank model config is required");
        }
        if (config.getEndpoint() == null || config.getEndpoint().trim().isEmpty()) {
            throw new IllegalArgumentException("rerank endpoint is required");
        }
    }

}
