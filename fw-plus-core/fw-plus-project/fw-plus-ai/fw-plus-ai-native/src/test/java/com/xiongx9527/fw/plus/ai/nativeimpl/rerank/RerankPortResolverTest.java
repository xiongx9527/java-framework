package com.xiongx9527.fw.plus.ai.nativeimpl.rerank;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class RerankPortResolverTest {

    @Test
    void resolvesProtocolToRerankPort() {
        assertTrue(RerankPortResolver.create(RerankModelConfig.builder()
                .protocol(RerankProtocol.TEI)
                .endpoint("http://localhost:8080/rerank")
                .build()) instanceof RerankModelPort);
        assertTrue(RerankPortResolver.create(RerankModelConfig.builder()
                .protocol(RerankProtocol.COHERE)
                .endpoint("http://localhost:8080/v1/rerank")
                .defaultModel("rerank-v3")
                .build()) instanceof RerankModelPort);
    }

    @Test
    void createsPortsThroughUnifiedFactory() {
        assertTrue(RerankPortResolver.create(
                config(RerankProtocol.TEI, RerankVendor.HUGGING_FACE))
                instanceof RerankModelPort);
        assertTrue(RerankPortResolver.create(
                config(RerankProtocol.COHERE, RerankVendor.COHERE))
                instanceof RerankModelPort);
    }

    @Test
    void protocolWinsWhenVendorAndProtocolAreBothProvided() {
        assertTrue(RerankPortResolver.create(
                config(RerankProtocol.TEI, RerankVendor.COHERE))
                instanceof RerankModelPort);
    }

    @Test
    void vendorIsUsedWhenProtocolIsNotProvided() {
        assertTrue(RerankPortResolver.create(configWithoutProtocol(RerankVendor.COHERE))
                instanceof RerankModelPort);
        assertTrue(RerankPortResolver.create(configWithoutProtocol(RerankVendor.JINA))
                instanceof RerankModelPort);
        assertTrue(RerankPortResolver.create(configWithoutProtocol(RerankVendor.VOYAGE))
                instanceof RerankModelPort);
        assertTrue(RerankPortResolver.create(configWithoutProtocol(RerankVendor.HUGGING_FACE))
                instanceof RerankModelPort);
    }

    @Test
    void rejectsWhenNeitherProtocolNorVendorProvided() {
        assertThrows(IllegalArgumentException.class,
                () -> RerankPortResolver.create(configWithoutProtocol(null)));
    }

    @Test
    void rejectsWhenEndpointMissing() {
        assertThrows(IllegalArgumentException.class, () -> RerankModelConfig.builder()
                .protocol(RerankProtocol.COHERE)
                .build());
        assertThrows(IllegalArgumentException.class, () -> RerankPortResolver.create(
                RerankModelConfig.builder().protocol(RerankProtocol.COHERE).endpoint("  ").build()));
    }

    private RerankModelConfig config(RerankProtocol protocol) {
        return config(protocol, null);
    }

    private RerankModelConfig config(RerankProtocol protocol, RerankVendor vendor) {
        return RerankModelConfig.builder()
                .protocol(protocol)
                .vendor(vendor)
                .endpoint("http://localhost:8080/rerank")
                .defaultModel("rerank-v3")
                .build();
    }

    private RerankModelConfig configWithoutProtocol() {
        return RerankModelConfig.builder()
                .endpoint("http://localhost:8080/rerank")
                .defaultModel("rerank-v3")
                .build();
    }

    private RerankModelConfig configWithoutProtocol(RerankVendor vendor) {
        return RerankModelConfig.builder()
                .vendor(vendor)
                .endpoint("http://localhost:8080/rerank")
                .defaultModel("rerank-v3")
                .build();
    }

    @Test
    void acceptsTimeoutAndProperties() {
        assertTrue(RerankPortResolver.create(RerankModelConfig.builder()
                .protocol(RerankProtocol.COHERE)
                .endpoint("http://localhost:8080/v1/rerank")
                .defaultModel("rerank-v3")
                .connectTimeoutMillis(1234)
                .readTimeoutMillis(5678)
                .property("extra", "x")
                .build()) instanceof RerankModelPort);
    }
}
