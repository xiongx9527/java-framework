package com.xiongx9527.fw.plus.ai.nativeimpl.memory;

import com.xiongx9527.fw.plus.ai.base.memory.ConversationMemoryException;
import com.xiongx9527.fw.plus.ai.base.memory.ConversationMemoryPort;

/**
 * 对话记忆后端解析：按协议（优先）或厂商（回退）路由到具体端口实现。
 *
 * <p>与 LLM / 向量库 Resolver 同构：</p>
 * <ul>
 *   <li>{@link ConversationMemoryProtocol#MEM0} → 自托管 mem0 服务 REST 客户端；</li>
 *   <li>{@link ConversationMemoryProtocol#VECTOR} → 向量库语义记忆（需 {@code embeddingPort} + {@code vectorStorePort}）；</li>
 *   <li>{@link ConversationMemoryProtocol#IN_MEMORY} → 进程内兜底 / 测试；</li>
 *   <li>协议缺失时回退厂商路由；两者皆缺返回未配置占位端口。</li>
 * </ul>
 */
public final class ConversationMemoryPortResolver {

    private ConversationMemoryPortResolver() {
    }

    /** 按配置创建端口；配置不足或解析失败时统一返回未配置占位端口，保证调用方零判空。 */
    public static ConversationMemoryPort create(ConversationMemoryConfig config) {
        if (config == null) {
            return new UnconfiguredConversationMemoryPort();
        }
        try {
            if (config.getProtocol() != null) {
                return createByProtocol(config);
            }
            if (config.getVendor() == null) {
                return new UnconfiguredConversationMemoryPort();
            }
            return createByVendor(config.getVendor(), config);
        } catch (RuntimeException ex) {
            return new UnconfiguredConversationMemoryPort();
        }
    }

    private static ConversationMemoryPort createByProtocol(ConversationMemoryConfig config) {
        switch (config.getProtocol()) {
            case MEM0:
                return createMem0(config);
            case VECTOR:
                return createVector(config);
            case IN_MEMORY:
                return new InMemoryConversationMemoryPort(null);
            default:
                throw new ConversationMemoryException("unsupported conversation memory protocol: " + config.getProtocol());
        }
    }

    private static ConversationMemoryPort createByVendor(ConversationMemoryVendor vendor, ConversationMemoryConfig config) {
        switch (vendor) {
            case MEM0:
                return createMem0(config);
            case VECTOR:
                return createVector(config);
            case IN_MEMORY:
                return new InMemoryConversationMemoryPort(null);
            default:
                throw new ConversationMemoryException("unsupported conversation memory vendor: " + vendor);
        }
    }

    private static ConversationMemoryPort createMem0(ConversationMemoryConfig config) {
        if (config.getBaseUrl() == null || config.getBaseUrl().trim().isEmpty()) {
            throw new ConversationMemoryException("mem0 memory provider requires baseUrl");
        }
        return new Mem0ServerConversationMemoryPort(config.getBaseUrl(), config.getApiKey(), config.getAuthScheme(),
                config.getConnectTimeoutMillis(), config.getReadTimeoutMillis(), null);
    }

    private static ConversationMemoryPort createVector(ConversationMemoryConfig config) {
        if (config.getEmbeddingPort() == null || config.getVectorStorePort() == null) {
            throw new ConversationMemoryException(
                    "VECTOR memory provider requires both embeddingPort and vectorStorePort");
        }
        return new VectorConversationMemoryPort(config.getEmbeddingPort(), config.getVectorStorePort(),
                null);
    }
}
