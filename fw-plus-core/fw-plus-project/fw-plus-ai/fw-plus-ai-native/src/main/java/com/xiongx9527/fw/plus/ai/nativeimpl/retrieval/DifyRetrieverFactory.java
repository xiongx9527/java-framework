package com.xiongx9527.fw.plus.ai.nativeimpl.retrieval;

/**
 * Dify 检索器工厂：校验配置后产出 {@link DifyRetriever}。配置未就绪（缺 server / apiKey /
 * datasetId）时抛异常，避免运行时才暴露连接信息缺失。
 */
public final class DifyRetrieverFactory {

    private DifyRetrieverFactory() {
    }

    public static DifyRetriever create(DifyRetrieverConfig config) {
        if (config == null || !config.configured()) {
            throw new IllegalArgumentException(
                    "DifyRetrieverConfig is not configured: server, apiKey and datasetId are required");
        }
        return new DifyRetriever(config);
    }
}
