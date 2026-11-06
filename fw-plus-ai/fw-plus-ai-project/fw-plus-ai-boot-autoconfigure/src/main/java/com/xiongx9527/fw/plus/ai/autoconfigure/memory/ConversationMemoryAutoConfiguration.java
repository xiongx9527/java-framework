package com.xiongx9527.fw.plus.ai.autoconfigure.memory;

import com.xiongx9527.fw.plus.ai.base.memory.ConversationMemoryPort;
import com.xiongx9527.fw.plus.ai.base.model.EmbeddingPort;
import com.xiongx9527.fw.plus.ai.base.store.vector.VectorStorePort;
import com.xiongx9527.fw.plus.ai.nativeimpl.memory.ConversationMemoryConfig;
import com.xiongx9527.fw.plus.ai.nativeimpl.memory.ConversationMemoryPortResolver;
import com.xiongx9527.fw.plus.ai.nativeimpl.memory.ConversationMemoryProtocol;
import com.xiongx9527.fw.plus.ai.nativeimpl.memory.ConversationMemoryVendor;
import com.xiongx9527.fw.plus.ai.nativeimpl.memory.MemoryAuthScheme;
import com.xiongx9527.fw.plus.ai.nativeimpl.memory.UnconfiguredConversationMemoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 对话记忆自动装配（多实例）：绑定 {@link ConversationMemoryProperties}，暴露一个 {@code @Primary} 的默认
 * {@link ConversationMemoryPort}（未配置时为占位端口）与全部实例的 {@code Map<String, ConversationMemoryPort>}
 * （key 含 {@code default} 与各命名实例）。
 *
 * <p>{@code VECTOR} 提供商需要 {@link EmbeddingPort} 与 {@link VectorStorePort} 默认 Bean（由对应的
 * 自动装配提供）；两者缺失时该实例回退为占位端口而非抛错，保证应用缺省可启动。</p>
 */
@Configuration
@ConditionalOnClass(ConversationMemoryPort.class)
@ConditionalOnProperty(prefix = "fw-plus.ai.memory", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(ConversationMemoryProperties.class)
public class ConversationMemoryAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(ConversationMemoryAutoConfiguration.class);

    private final ConversationMemoryProperties properties;
    private final ObjectProvider<EmbeddingPort> embeddingPortProvider;
    private final ObjectProvider<VectorStorePort> vectorStorePortProvider;

    public ConversationMemoryAutoConfiguration(ConversationMemoryProperties properties,
            ObjectProvider<EmbeddingPort> embeddingPortProvider,
            ObjectProvider<VectorStorePort> vectorStorePortProvider) {
        this.properties = properties;
        this.embeddingPortProvider = embeddingPortProvider;
        this.vectorStorePortProvider = vectorStorePortProvider;
    }

    @Bean
    @Primary
    @ConditionalOnMissingBean(ConversationMemoryPort.class)
    public ConversationMemoryPort conversationMemoryPort() {
        return resolve(properties.getDefault());
    }

    @Bean
    @ConditionalOnMissingBean(name = "conversationMemoryPorts")
    public Map<String, ConversationMemoryPort> conversationMemoryPorts() {
        Map<String, ConversationMemoryPort> map = new LinkedHashMap<String, ConversationMemoryPort>();
        map.put("default", resolve(properties.getDefault()));
        for (Map.Entry<String, ConversationMemoryInstance> e : properties.getInstances().entrySet()) {
            map.put(e.getKey(), resolve(e.getValue()));
        }
        return map;
    }

    private ConversationMemoryPort resolve(ConversationMemoryInstance instance) {
        if (instance == null) {
            return new UnconfiguredConversationMemoryPort();
        }
        ConversationMemoryConfig config = toConfig(instance);
        try {
            return ConversationMemoryPortResolver.create(config);
        } catch (RuntimeException ex) {
            log.info("conversation memory instance not fully configured ({}); exposing a placeholder port",
                    ex.getMessage());
            return new UnconfiguredConversationMemoryPort();
        }
    }

    private ConversationMemoryConfig toConfig(ConversationMemoryInstance instance) {
        String vendor = instance.getVendor();
        String protocol = instance.getProtocol();
        ConversationMemoryConfig.Builder builder = ConversationMemoryConfig.builder()
                .baseUrl(instance.getBaseUrl())
                .apiKey(instance.getApiKey())
                .connectTimeoutMillis(instance.getConnectTimeoutMillis())
                .readTimeoutMillis(instance.getReadTimeoutMillis());
        if (StringUtils.hasText(vendor)) {
            builder.vendor(ConversationMemoryVendor.valueOf(vendor.trim().toUpperCase(Locale.ROOT)));
        }
        if (StringUtils.hasText(protocol)) {
            builder.protocol(ConversationMemoryProtocol.valueOf(protocol.trim().toUpperCase(Locale.ROOT)));
        }
        if (StringUtils.hasText(instance.getAuthScheme())) {
            builder.authScheme(MemoryAuthScheme.valueOf(instance.getAuthScheme().trim().toUpperCase(Locale.ROOT)));
        }
        boolean vector = "VECTOR".equalsIgnoreCase(protocol) || "VECTOR".equalsIgnoreCase(vendor);
        if (vector) {
            builder.embeddingPort(embeddingPortProvider.getIfAvailable());
            builder.vectorStorePort(vectorStorePortProvider.getIfAvailable());
        }
        return builder.build();
    }
}
