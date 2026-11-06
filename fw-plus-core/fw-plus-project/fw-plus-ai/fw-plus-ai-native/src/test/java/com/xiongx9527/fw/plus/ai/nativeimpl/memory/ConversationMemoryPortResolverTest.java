package com.xiongx9527.fw.plus.ai.nativeimpl.memory;

import com.xiongx9527.fw.plus.ai.base.model.EmbeddingPort;
import com.xiongx9527.fw.plus.ai.base.store.vector.VectorStorePort;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ConversationMemoryPortResolverTest {

    @Test
    void nullConfigReturnsUnconfigured() {
        assertThat(ConversationMemoryPortResolver.create(null))
                .isInstanceOf(UnconfiguredConversationMemoryPort.class);
    }

    @Test
    void noVendorOrProtocolReturnsUnconfigured() {
        ConversationMemoryConfig config = ConversationMemoryConfig.builder().build();
        assertThat(ConversationMemoryPortResolver.create(config))
                .isInstanceOf(UnconfiguredConversationMemoryPort.class);
    }

    @Test
    void mem0ProtocolRoutesToMem0Port() {
        ConversationMemoryConfig config = ConversationMemoryConfig.builder()
                .protocol(ConversationMemoryProtocol.MEM0)
                .baseUrl("http://localhost:8888")
                .apiKey("k")
                .build();
        assertThat(ConversationMemoryPortResolver.create(config))
                .isInstanceOf(Mem0ServerConversationMemoryPort.class);
    }

    @Test
    void inMemoryProtocolRoutesToInMemoryPort() {
        ConversationMemoryConfig config = ConversationMemoryConfig.builder()
                .protocol(ConversationMemoryProtocol.IN_MEMORY)
                .build();
        assertThat(ConversationMemoryPortResolver.create(config))
                .isInstanceOf(InMemoryConversationMemoryPort.class);
    }

    @Test
    void vectorProtocolRoutesToVectorPort() {
        ConversationMemoryConfig config = ConversationMemoryConfig.builder()
                .protocol(ConversationMemoryProtocol.VECTOR)
                .embeddingPort(new com.xiongx9527.fw.plus.ai.nativeimpl.memory.VectorConversationMemoryPortTest.FakeEmbeddingPort())
                .vectorStorePort(new com.xiongx9527.fw.plus.ai.nativeimpl.memory.VectorConversationMemoryPortTest.FakeVectorStore())
                .build();
        assertThat(ConversationMemoryPortResolver.create(config))
                .isInstanceOf(VectorConversationMemoryPort.class);
    }

    @Test
    void mem0RequiresBaseUrl() {
        ConversationMemoryConfig config = ConversationMemoryConfig.builder()
                .protocol(ConversationMemoryProtocol.MEM0)
                .build();
        assertThat(ConversationMemoryPortResolver.create(config))
                .isInstanceOf(UnconfiguredConversationMemoryPort.class);
    }

    @Test
    void vectorRequiresPorts() {
        ConversationMemoryConfig config = ConversationMemoryConfig.builder()
                .protocol(ConversationMemoryProtocol.VECTOR)
                .build();
        assertThat(ConversationMemoryPortResolver.create(config))
                .isInstanceOf(UnconfiguredConversationMemoryPort.class);
    }
}
