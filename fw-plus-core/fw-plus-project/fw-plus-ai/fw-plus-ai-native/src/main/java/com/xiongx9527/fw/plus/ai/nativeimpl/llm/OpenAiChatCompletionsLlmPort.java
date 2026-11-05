package com.xiongx9527.fw.plus.ai.nativeimpl.llm;

/** OpenAI Chat Completions 协议端口。 */
public class OpenAiChatCompletionsLlmPort extends OpenAiCompatibleLlmPort {
    public OpenAiChatCompletionsLlmPort(String baseUrl, String apiKey, String defaultModel) {
        this(baseUrl, apiKey, defaultModel, null, null);
    }

    public OpenAiChatCompletionsLlmPort(String baseUrl, String apiKey, String defaultModel,
            Integer connectTimeoutMillis, Integer readTimeoutMillis) {
        super(baseUrl, apiKey, defaultModel, connectTimeoutMillis, readTimeoutMillis);
    }
}
