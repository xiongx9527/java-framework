package com.xiongx9527.fw.plus.ai.nativeimpl.embedding;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class EmbeddingPortResolverTest {

    @Test
    void resolvesProtocolToOpenAiCompatiblePort() {
        assertTrue(EmbeddingPortResolver.create(EmbeddingModelConfig.builder()
                .protocol(EmbeddingProtocol.OPENAI_COMPATIBLE)
                .baseUrl("http://localhost:8080/v1")
                .defaultModel("text-embedding-3-small")
                .build()) instanceof OpenAiCompatibleEmbeddingPort);
    }

    @Test
    void createsOpenAiCompatibleThroughFactory() {
        assertTrue(EmbeddingPortResolver.createOpenAiCompatible(
                config(EmbeddingProtocol.OPENAI_COMPATIBLE)) instanceof OpenAiCompatibleEmbeddingPort);
    }

    @Test
    void createsPortsThroughUnifiedFactory() {
        assertTrue(EmbeddingPortResolver.create(
                config(EmbeddingProtocol.OPENAI_COMPATIBLE, EmbeddingVendor.OPENAI))
                instanceof OpenAiCompatibleEmbeddingPort);
        assertTrue(EmbeddingPortResolver.create(
                config(EmbeddingProtocol.OPENAI_COMPATIBLE, EmbeddingVendor.DASHSCOPE))
                instanceof OpenAiCompatibleEmbeddingPort);
    }

    @Test
    void protocolWinsWhenVendorAndProtocolAreBothProvided() {
        assertTrue(EmbeddingPortResolver.create(
                config(EmbeddingProtocol.OPENAI_COMPATIBLE, EmbeddingVendor.COHERE))
                instanceof OpenAiCompatibleEmbeddingPort);
    }

    @Test
    void vendorIsUsedWhenProtocolIsNotProvided() {
        assertTrue(EmbeddingPortResolver.create(configWithoutProtocol(EmbeddingVendor.OPENAI))
                instanceof OpenAiCompatibleEmbeddingPort);
        assertTrue(EmbeddingPortResolver.create(configWithoutProtocol(EmbeddingVendor.DASHSCOPE))
                instanceof OpenAiCompatibleEmbeddingPort);
        assertTrue(EmbeddingPortResolver.create(configWithoutProtocol(EmbeddingVendor.QWEN))
                instanceof OpenAiCompatibleEmbeddingPort);
        assertTrue(EmbeddingPortResolver.create(configWithoutProtocol(EmbeddingVendor.OLLAMA))
                instanceof OpenAiCompatibleEmbeddingPort);
        assertTrue(EmbeddingPortResolver.create(configWithoutProtocol(EmbeddingVendor.QIANFAN))
                instanceof OpenAiCompatibleEmbeddingPort);
        assertTrue(EmbeddingPortResolver.create(configWithoutProtocol(EmbeddingVendor.LOCAL_AI))
                instanceof OpenAiCompatibleEmbeddingPort);
    }

    @Test
    void rejectsVendorRequiringExplicitProtocol() {
        assertThrows(IllegalArgumentException.class,
                () -> EmbeddingPortResolver.create(configWithoutProtocol(EmbeddingVendor.AZURE_OPENAI)));
        assertThrows(IllegalArgumentException.class,
                () -> EmbeddingPortResolver.create(configWithoutProtocol(EmbeddingVendor.HUGGING_FACE)));
        assertThrows(IllegalArgumentException.class,
                () -> EmbeddingPortResolver.create(configWithoutProtocol(EmbeddingVendor.COHERE)));
        assertThrows(IllegalArgumentException.class,
                () -> EmbeddingPortResolver.create(configWithoutProtocol(EmbeddingVendor.VOYAGE)));
        assertThrows(IllegalArgumentException.class,
                () -> EmbeddingPortResolver.create(configWithoutProtocol(EmbeddingVendor.ZHIPU_AI)));
    }

    @Test
    void rejectsWhenNeitherProtocolNorVendorProvided() {
        assertThrows(IllegalArgumentException.class,
                () -> EmbeddingPortResolver.create(configWithoutProtocol(null)));
    }

    @Test
    void rejectsWhenConfigMissingRequiredFields() {
        assertThrows(IllegalArgumentException.class, () -> EmbeddingModelConfig.builder()
                .protocol(EmbeddingProtocol.OPENAI_COMPATIBLE)
                .defaultModel("m")
                .build());
        assertThrows(IllegalArgumentException.class, () -> EmbeddingModelConfig.builder()
                .protocol(EmbeddingProtocol.OPENAI_COMPATIBLE)
                .baseUrl("http://localhost:8080/v1")
                .build());
    }

    private EmbeddingModelConfig config(EmbeddingProtocol protocol) {
        return config(protocol, null);
    }

    private EmbeddingModelConfig config(EmbeddingProtocol protocol, EmbeddingVendor vendor) {
        return EmbeddingModelConfig.builder()
                .protocol(protocol)
                .vendor(vendor)
                .baseUrl("http://localhost:8080/v1")
                .defaultModel("text-embedding-3-small")
                .build();
    }

    private EmbeddingModelConfig configWithoutProtocol() {
        return EmbeddingModelConfig.builder()
                .baseUrl("http://localhost:8080/v1")
                .defaultModel("text-embedding-3-small")
                .build();
    }

    private EmbeddingModelConfig configWithoutProtocol(EmbeddingVendor vendor) {
        return EmbeddingModelConfig.builder()
                .vendor(vendor)
                .baseUrl("http://localhost:8080/v1")
                .defaultModel("text-embedding-3-small")
                .build();
    }

    @Test
    void acceptsTimeoutAndProperties() {
        assertTrue(EmbeddingPortResolver.create(EmbeddingModelConfig.builder()
                .protocol(EmbeddingProtocol.OPENAI_COMPATIBLE)
                .baseUrl("http://localhost:8080/v1")
                .defaultModel("text-embedding-3-small")
                .connectTimeoutMillis(1234)
                .readTimeoutMillis(5678)
                .property("region", "cn")
                .build()) instanceof OpenAiCompatibleEmbeddingPort);
    }
}
