package com.xiongx9527.fw.plus.ai.autoconfigure.model;

import com.xiongx9527.fw.plus.ai.base.model.EmbeddingPort;
import com.xiongx9527.fw.plus.ai.nativeimpl.embedding.EmbeddingModelConfig;
import com.xiongx9527.fw.plus.ai.nativeimpl.embedding.EmbeddingPortResolver;
import com.xiongx9527.fw.plus.ai.nativeimpl.embedding.EmbeddingProtocol;
import com.xiongx9527.fw.plus.ai.nativeimpl.embedding.EmbeddingVendor;
import com.xiongx9527.fw.plus.ai.nativeimpl.embedding.UnconfiguredEmbeddingPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * 向量（Embedding）模型自动装配（多实例）：绑定 {@link EmbeddingModelProperties}，暴露一个
 * {@code @Primary} 的默认 {@link EmbeddingPort} 与全部实例的 {@code Map<String, EmbeddingPort>}
 * （key 含 {@code default} 与各命名实例）。未配置时不创建 Bean。
 */
@Configuration
@ConditionalOnClass(EmbeddingPort.class)
@ConditionalOnProperty(prefix = "fw-plus.ai.embedding", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(EmbeddingModelProperties.class)
public class EmbeddingModelAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingModelAutoConfiguration.class);

    private final EmbeddingModelProperties properties;

    public EmbeddingModelAutoConfiguration(EmbeddingModelProperties properties) {
        this.properties = properties;
    }

    @Bean
    @Primary
    @ConditionalOnMissingBean(EmbeddingPort.class)
    public EmbeddingPort embeddingPort() {
        EmbeddingModelInstance instance = properties.getDefault();
        if (!isConfigured(instance)) {
            log.info("embedding model default instance not configured; exposing an unconfigured placeholder port");
            return new UnconfiguredEmbeddingPort();
        }
        return resolve(instance);
    }

    @Bean
    @ConditionalOnMissingBean(name = "embeddingPorts")
    public Map<String, EmbeddingPort> embeddingPorts() {
        Map<String, EmbeddingPort> map = new LinkedHashMap<String, EmbeddingPort>();
        EmbeddingModelInstance def = properties.getDefault();
        map.put("default", isConfigured(def) ? resolve(def) : new UnconfiguredEmbeddingPort());
        for (Map.Entry<String, EmbeddingModelInstance> e : properties.getInstances().entrySet()) {
            map.put(e.getKey(), isConfigured(e.getValue()) ? resolve(e.getValue()) : new UnconfiguredEmbeddingPort());
        }
        return map;
    }

    private EmbeddingPort resolve(EmbeddingModelInstance instance) {
        EmbeddingModelConfig.Builder builder = EmbeddingModelConfig.builder()
                .baseUrl(instance.getBaseUrl())
                .apiKey(instance.getApiKey())
                .defaultModel(instance.getDefaultModel())
                .dimension(instance.getDimension())
                .connectTimeoutMillis(instance.getConnectTimeoutMillis())
                .readTimeoutMillis(instance.getReadTimeoutMillis());
        if (StringUtils.hasText(instance.getVendor())) {
            builder.vendor(EmbeddingVendor.valueOf(instance.getVendor().trim().toUpperCase(Locale.ROOT)));
        }
        if (StringUtils.hasText(instance.getProtocol())) {
            builder.protocol(EmbeddingProtocol.valueOf(instance.getProtocol().trim().toUpperCase(Locale.ROOT)));
        }
        return EmbeddingPortResolver.create(builder.build());
    }

    private static boolean isConfigured(EmbeddingModelInstance instance) {
        return instance != null
                && (StringUtils.hasText(instance.getVendor()) || StringUtils.hasText(instance.getProtocol()));
    }
}
