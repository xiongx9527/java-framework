package com.xiongx9527.fw.plus.dify.utils;

import com.xiongx9527.fw.plus.dify.dataset.IDatasetClient;
import com.xiongx9527.fw.plus.dify.dataset.heros.DocumentHero;
import com.xiongx9527.fw.plus.dify.types.ApiConfig;
import com.xiongx9527.fw.plus.dify.dataset.heros.DatasetHero;
import com.xiongx9527.fw.plus.dify.dataset.impl.DatasetClientImpl;
import com.xiongx9527.fw.plus.dify.types.DifyClientException;

/**
 * Dify 知识库 builder
 */
public class DatasetBuilder {
    private String apiKey;
    private String baseUrl;

    /**
     * builder
     */
    public static DatasetBuilder builder() {
        return new DatasetBuilder();
    }

    /**
     * builder
     * @param server Dify 服务地址
     * @param apiKey 知识库 Api Key
     */
    public static DatasetBuilder builder(String server, String apiKey) {
        return new DatasetBuilder(server, apiKey);
    }

    /**
     * constructor
     */
    private DatasetBuilder() {

    }

    /**
     * constructor
     * @param server Dify 服务地址
     * @param apiKey 知识库 Api Key
     */
    private DatasetBuilder(String server, String apiKey) {
        this.apiKey = apiKey;
        this.baseUrl = server;
    }

    /**
     * config
     * @param config DifyConfig
     */
    public DatasetBuilder config(ApiConfig config) {
        this.baseUrl = config.getServer();
        this.apiKey = config.getApiKey();
        return this;
    }

    /**
     * dify server base url
     * @param baseUrl String
     */
    public DatasetBuilder baseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
        return this;
    }

    /**
     * dify app api key
     * @param apiKey String
     */
    public DatasetBuilder apiKey(String apiKey) {
        this.apiKey = apiKey;
        return this;
    }

    /**
     * build
     */
    public IDatasetClient build() {
        String normalizedBaseUrl = DifyValidationUtils.normalizeServer(baseUrl, "dataset", "build");
        String requiredApiKey = DifyValidationUtils.requireText(apiKey, "apiKey", "dataset", "build");

        try {
            return DatasetClientImpl.build(normalizedBaseUrl, requiredApiKey);
        } catch (Exception e) {
            throw new DifyClientException("Dify dataset client build error: failed to create DatasetClientImpl", e);
        }
    }

    /**
     * 快捷构建一个Dataset对象
     * @param datasetId 知识库ID
     */
    public DatasetHero of(String datasetId) {
        return DatasetHero.of(datasetId, ApiConfig.builder().server(DifyValidationUtils.normalizeServer(baseUrl, "dataset", "build")).apiKey(apiKey).build());
    }

    /**
     * 快捷创建一个Document对象
     * @param datasetId 知识库ID
     * @param documentId 文档ID
     */
    public DocumentHero ofDocument(String datasetId, String documentId) {
        return DocumentHero.of(datasetId, documentId, ApiConfig.builder().server(DifyValidationUtils.normalizeServer(baseUrl, "dataset", "build")).apiKey(apiKey).build());
    }

}
