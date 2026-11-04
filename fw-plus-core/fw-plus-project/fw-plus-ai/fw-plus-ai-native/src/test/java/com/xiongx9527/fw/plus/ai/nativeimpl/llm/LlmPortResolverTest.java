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
    void rejectsProtocolThatDoesNotBelongToVendorFactory() {
        assertThrows(IllegalArgumentException.class,
                () -> LlmPortResolver.createAnthropic(config(LlmProtocol.GEMINI)));
    }

    private LlmModelConfig config(LlmProtocol protocol) {
        return LlmModelConfig.builder()
                .protocol(protocol)
                .baseUrl("http://localhost:8080/v1")
                .defaultModel("model")
                .apiVersion("2025-01-01")
                .build();
    }
}
