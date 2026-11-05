package com.xiongx9527.fw.plus.ai.nativeimpl.llm;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class LlmPortResolverTest {

    @Test
    void resolvesEachProtocolToItsDedicatedPort() {
        assertTrue(LlmPortResolver.create(LlmModelConfig.builder()
                .protocol(LlmProtocol.OPENAI_CHAT_COMPLETIONS)
                .baseUrl("http://localhost:8080/v1")
                .defaultModel("chat-model")
                .build()) instanceof OpenAiCompatibleLlmPort);
        assertTrue(LlmPortResolver.create(LlmModelConfig.builder()
                .protocol(LlmProtocol.AZURE_OPENAI_CHAT_COMPLETIONS)
                .baseUrl("http://localhost:8080")
                .defaultModel("deployment")
                .apiVersion("2025-01-01")
                .build()) instanceof AzureOpenAiLlmPort);
        assertTrue(LlmPortResolver.create(LlmModelConfig.builder()
                .protocol(LlmProtocol.OPENAI_RESPONSES)
                .baseUrl("http://localhost:8080/v1")
                .defaultModel("responses-model")
                .build()) instanceof OpenAiResponsesLlmPort);
        assertTrue(LlmPortResolver.create(LlmModelConfig.builder()
                .protocol(LlmProtocol.ANTHROPIC_MESSAGES)
                .baseUrl("http://localhost:8080")
                .defaultModel("claude")
                .build()) instanceof AnthropicLlmPort);
        assertTrue(LlmPortResolver.create(LlmModelConfig.builder()
                .protocol(LlmProtocol.GEMINI)
                .baseUrl("http://localhost:8080")
                .defaultModel("gemini")
                .build()) instanceof GeminiLlmPort);
    }

    @Test
    void createsOpenAiPortsThroughVendorFactory() {
        assertTrue(LlmPortResolver.createOpenAi(config(LlmProtocol.OPENAI_CHAT_COMPLETIONS))
                instanceof OpenAiChatCompletionsLlmPort);
        assertTrue(LlmPortResolver.createOpenAi(config(LlmProtocol.OPENAI_RESPONSES))
                instanceof OpenAiResponsesLlmPort);
    }

    @Test
    void createsOtherVendorPortsThroughVendorFactories() {
        assertTrue(LlmPortResolver.createAzureOpenAi(config(LlmProtocol.AZURE_OPENAI_CHAT_COMPLETIONS))
                instanceof AzureOpenAiLlmPort);
        assertTrue(LlmPortResolver.createAnthropic(config(LlmProtocol.ANTHROPIC_MESSAGES))
                instanceof AnthropicLlmPort);
        assertTrue(LlmPortResolver.createGemini(config(LlmProtocol.GEMINI))
                instanceof GeminiLlmPort);
    }

    @Test
    void createsPortsThroughUnifiedVendorFactory() {
        assertTrue(LlmPortResolver.create(config(LlmProtocol.OPENAI_CHAT_COMPLETIONS, LlmVendor.OPENAI))
                instanceof OpenAiChatCompletionsLlmPort);
        assertTrue(LlmPortResolver.create(config(LlmProtocol.OPENAI_RESPONSES, LlmVendor.OPENAI))
                instanceof OpenAiResponsesLlmPort);
        assertTrue(LlmPortResolver.create(config(LlmProtocol.AZURE_OPENAI_CHAT_COMPLETIONS, LlmVendor.AZURE_OPENAI))
                instanceof AzureOpenAiLlmPort);
        assertTrue(LlmPortResolver.create(config(LlmProtocol.ANTHROPIC_MESSAGES, LlmVendor.ANTHROPIC))
                instanceof AnthropicLlmPort);
        assertTrue(LlmPortResolver.create(config(LlmProtocol.GEMINI, LlmVendor.GEMINI))
                instanceof GeminiLlmPort);
    }

    @Test
    void protocolWinsWhenVendorAndProtocolAreBothProvided() {
        assertTrue(LlmPortResolver.create(config(LlmProtocol.GEMINI, LlmVendor.ANTHROPIC))
                instanceof GeminiLlmPort);
    }

    @Test
    void vendorIsUsedWhenProtocolIsNotProvided() {
        assertTrue(LlmPortResolver.create(configWithoutProtocol(LlmVendor.ANTHROPIC))
                instanceof AnthropicLlmPort);
    }

    @Test
    void routesLangChain4jProvidersWithoutExplicitProtocol() {
        assertTrue(LlmPortResolver.create(configWithoutProtocol(LlmVendor.DASHSCOPE))
                instanceof OpenAiChatCompletionsLlmPort);
        assertTrue(LlmPortResolver.create(configWithoutProtocol(LlmVendor.QWEN))
                instanceof OpenAiChatCompletionsLlmPort);
        assertTrue(LlmPortResolver.create(configWithoutProtocol(LlmVendor.GITHUB_MODELS))
                instanceof OpenAiChatCompletionsLlmPort);
        assertTrue(LlmPortResolver.create(configWithoutProtocol(LlmVendor.OLLAMA))
                instanceof OpenAiChatCompletionsLlmPort);
        assertTrue(LlmPortResolver.create(configWithoutProtocol(LlmVendor.QIANFAN))
                instanceof OpenAiChatCompletionsLlmPort);
        assertTrue(LlmPortResolver.create(configWithoutProtocol(LlmVendor.LOCAL_AI))
                instanceof OpenAiChatCompletionsLlmPort);
        assertTrue(LlmPortResolver.create(configWithoutProtocol(LlmVendor.GOOGLE_AI_GEMINI))
                instanceof GeminiLlmPort);
        assertThrows(IllegalArgumentException.class,
                () -> LlmPortResolver.create(configWithoutProtocol(LlmVendor.VERTEX_AI_GEMINI)));
        assertThrows(IllegalArgumentException.class,
                () -> LlmPortResolver.create(configWithoutProtocol(LlmVendor.HUGGING_FACE)));
        assertThrows(IllegalArgumentException.class,
                () -> LlmPortResolver.create(configWithoutProtocol(LlmVendor.ZHIPU_AI)));
        assertThrows(IllegalArgumentException.class,
                () -> LlmPortResolver.create(configWithoutProtocol(LlmVendor.COHERE)));
    }

    @Test
    void vendorFromConfigResolvesPortWhenProtocolMissing() {
        assertTrue(LlmPortResolver.create(configWithoutProtocol(LlmVendor.ANTHROPIC))
                instanceof AnthropicLlmPort);
        assertTrue(LlmPortResolver.create(configWithoutProtocol(LlmVendor.OPENAI))
                instanceof OpenAiChatCompletionsLlmPort);
        assertTrue(LlmPortResolver.create(configWithoutProtocol(LlmVendor.GOOGLE_AI_GEMINI))
                instanceof GeminiLlmPort);
    }

    @Test
    void rejectsWhenNeitherProtocolNorVendorProvided() {
        assertThrows(IllegalArgumentException.class,
                () -> LlmPortResolver.create(configWithoutProtocol(null)));
    }

    @Test
    void rejectsProtocolThatDoesNotBelongToVendorFactory() {
        assertThrows(IllegalArgumentException.class,
                () -> LlmPortResolver.createAnthropic(config(LlmProtocol.GEMINI)));
    }

    private LlmModelConfig config(LlmProtocol protocol) {
        return config(protocol, null);
    }

    private LlmModelConfig config(LlmProtocol protocol, LlmVendor vendor) {
        return LlmModelConfig.builder()
                .protocol(protocol)
                .vendor(vendor)
                .baseUrl("http://localhost:8080/v1")
                .defaultModel("model")
                .apiVersion("2025-01-01")
                .build();
    }

    private LlmModelConfig configWithoutProtocol() {
        return LlmModelConfig.builder()
                .baseUrl("http://localhost:8080/v1")
                .defaultModel("model")
                .apiVersion("2025-01-01")
                .build();
    }

    private LlmModelConfig configWithoutProtocol(LlmVendor vendor) {
        return LlmModelConfig.builder()
                .vendor(vendor)
                .baseUrl("http://localhost:8080/v1")
                .defaultModel("model")
                .apiVersion("2025-01-01")
                .build();
    }

    @Test
    void acceptsTimeoutAndProperties() {
        assertTrue(LlmPortResolver.create(LlmModelConfig.builder()
                .protocol(LlmProtocol.OPENAI_CHAT_COMPLETIONS)
                .baseUrl("http://localhost:8080/v1")
                .defaultModel("chat-model")
                .connectTimeoutMillis(1234)
                .readTimeoutMillis(5678)
                .property("organization", "acme")
                .build()) instanceof OpenAiChatCompletionsLlmPort);
    }
}
