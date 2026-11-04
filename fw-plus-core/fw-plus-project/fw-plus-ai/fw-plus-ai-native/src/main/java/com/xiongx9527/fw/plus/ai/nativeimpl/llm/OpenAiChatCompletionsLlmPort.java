package com.xiongx9527.fw.plus.ai.nativeimpl.llm;

/** OpenAI Chat Completions 协议端口。 */
public class OpenAiChatCompletionsLlmPort extends OpenAiCompatibleLlmPort {
    public OpenAiChatCompletionsLlmPort(String baseUrl, String apiKey, String defaultModel) {
        super(baseUrl, apiKey, defaultModel);
    }
}
