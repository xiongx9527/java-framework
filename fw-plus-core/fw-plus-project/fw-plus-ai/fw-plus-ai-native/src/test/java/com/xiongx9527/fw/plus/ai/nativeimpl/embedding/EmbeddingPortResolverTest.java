package com.xiongx9527.fw.plus.ai.nativeimpl.embedding;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.xiongx9527.fw.plus.ai.nativeimpl.embedding.AzureOpenAiEmbeddingPort;
import com.xiongx9527.fw.plus.ai.nativeimpl.embedding.CohereEmbeddingPort;
import com.xiongx9527.fw.plus.ai.nativeimpl.embedding.OpenAiCompatibleEmbeddingPort;
import com.xiongx9527.fw.plus.ai.nativeimpl.embedding.TeiEmbeddingPort;

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
    void resolvesVendorToNativeOrCompatiblePort() {
        // 这些厂商不再需要显式协议：各自落到原生端口或 OpenAI 兼容端口
        assertTrue(EmbeddingPortResolver.create(configWithoutProtocol(EmbeddingVendor.AZURE_OPENAI))
                instanceof AzureOpenAiEmbeddingPort);
        assertTrue(EmbeddingPortResolver.create(configWithoutProtocol(EmbeddingVendor.HUGGING_FACE))
                instanceof TeiEmbeddingPort);
        assertTrue(EmbeddingPortResolver.create(configWithoutProtocol(EmbeddingVendor.COHERE))
                instanceof CohereEmbeddingPort);
        assertTrue(EmbeddingPortResolver.create(configWithoutProtocol(EmbeddingVendor.VOYAGE))
                instanceof OpenAiCompatibleEmbeddingPort);
        assertTrue(EmbeddingPortResolver.create(configWithoutProtocol(EmbeddingVendor.ZHIPU_AI))
                instanceof OpenAiCompatibleEmbeddingPort);
    }

    @Test
    void resolvesProtocolToNativePort() {
        assertTrue(EmbeddingPortResolver.create(
                config(EmbeddingProtocol.COHERE)) instanceof CohereEmbeddingPort);
        assertTrue(EmbeddingPortResolver.create(
                config(EmbeddingProtocol.AZURE_OPENAI)) instanceof AzureOpenAiEmbeddingPort);
        assertTrue(EmbeddingPortResolver.create(
                config(EmbeddingProtocol.TEI)) instanceof TeiEmbeddingPort);
    }

    @Test
    void createsNativePortsThroughFactory() {
        assertTrue(EmbeddingPortResolver.createCohere(config(EmbeddingProtocol.COHERE))
                instanceof CohereEmbeddingPort);
        assertTrue(EmbeddingPortResolver.createAzureOpenAi(config(EmbeddingProtocol.AZURE_OPENAI))
                instanceof AzureOpenAiEmbeddingPort);
        assertTrue(EmbeddingPortResolver.createTei(config(EmbeddingProtocol.TEI))
                instanceof TeiEmbeddingPort);
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
