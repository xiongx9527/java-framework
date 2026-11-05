package com.xiongx9527.fw.plus.ai.nativeimpl.llm;

import com.xiongx9527.fw.plus.ai.base.model.LlmRequest;
import okhttp3.Request;

/** Azure OpenAI Chat Completions：以 deployment、api-version 和 api-key 认证。 */
public class AzureOpenAiLlmPort extends OpenAiCompatibleLlmPort {
    private static final String DEFAULT_API_VERSION = "2024-02-15-preview";
    private final String apiVersion;

    public AzureOpenAiLlmPort(String baseUrl, String apiKey, String deployment, String apiVersion) {
        this(baseUrl, apiKey, deployment, apiVersion, null, null);
    }

    public AzureOpenAiLlmPort(String baseUrl, String apiKey, String deployment, String apiVersion,
            Integer connectTimeoutMillis, Integer readTimeoutMillis) {
        super(baseUrl, apiKey, deployment, connectTimeoutMillis, readTimeoutMillis);
        this.apiVersion = apiVersion == null || apiVersion.trim().isEmpty()
                ? DEFAULT_API_VERSION : apiVersion.trim();
    }

    @Override
    protected String resolveUrl(LlmRequest request) {
        String deployment = request.getModel() == null ? defaultModel : request.getModel();
        return baseUrl + "/openai/deployments/" + deployment
                + "/chat/completions?api-version=" + apiVersion;
    }

    @Override
    protected String modelsUrl() {
        return baseUrl + "/openai/models?api-version=" + apiVersion;
    }

    @Override
    protected void addAuthentication(Request.Builder request) {
        if (apiKey != null && !apiKey.isEmpty()) {
            request.addHeader("api-key", apiKey);
        }
    }
}
