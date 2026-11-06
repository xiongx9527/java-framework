package com.xiongx9527.fw.plus.ai.autoconfigure.model;

import com.xiongx9527.fw.plus.ai.base.model.RerankPort;
import com.xiongx9527.fw.plus.ai.nativeimpl.rerank.RerankModelConfig;
import com.xiongx9527.fw.plus.ai.nativeimpl.rerank.RerankPortResolver;
import com.xiongx9527.fw.plus.ai.nativeimpl.rerank.RerankProtocol;
import com.xiongx9527.fw.plus.ai.nativeimpl.rerank.RerankVendor;
import com.xiongx9527.fw.plus.ai.nativeimpl.rerank.UnconfiguredRerankPort;
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
 * 重排（Rerank）模型自动装配（多实例）：绑定 {@link RerankModelProperties}，暴露一个
 * {@code @Primary} 的默认 {@link RerankPort} 与全部实例的 {@code Map<String, RerankPort>}
 * （key 含 {@code default} 与各命名实例）。未配置时不创建 Bean。
 */
@Configuration
@ConditionalOnClass(RerankPort.class)
@ConditionalOnProperty(prefix = "fw-plus.ai.rerank", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(RerankModelProperties.class)
public class RerankModelAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(RerankModelAutoConfiguration.class);

    private final RerankModelProperties properties;

    public RerankModelAutoConfiguration(RerankModelProperties properties) {
        this.properties = properties;
    }

    @Bean
    @Primary
    @ConditionalOnMissingBean(RerankPort.class)
    public RerankPort rerankPort() {
        RerankModelInstance instance = properties.getDefault();
        if (!isConfigured(instance)) {
            log.info("rerank model default instance not configured; exposing an unconfigured placeholder port");
            return new UnconfiguredRerankPort();
        }
        return resolve(instance);
    }

    @Bean
    @ConditionalOnMissingBean(name = "rerankPorts")
    public Map<String, RerankPort> rerankPorts() {
        Map<String, RerankPort> map = new LinkedHashMap<String, RerankPort>();
        RerankModelInstance def = properties.getDefault();
        map.put("default", isConfigured(def) ? resolve(def) : new UnconfiguredRerankPort());
        for (Map.Entry<String, RerankModelInstance> e : properties.getInstances().entrySet()) {
            map.put(e.getKey(), isConfigured(e.getValue()) ? resolve(e.getValue()) : new UnconfiguredRerankPort());
        }
        return map;
    }

    private RerankPort resolve(RerankModelInstance instance) {
        RerankModelConfig.Builder builder = RerankModelConfig.builder()
                .endpoint(instance.getBaseUrl())
                .apiKey(instance.getApiKey())
                .defaultModel(instance.getDefaultModel())
                .connectTimeoutMillis(instance.getConnectTimeoutMillis())
                .readTimeoutMillis(instance.getReadTimeoutMillis());
        if (StringUtils.hasText(instance.getVendor())) {
            builder.vendor(RerankVendor.valueOf(instance.getVendor().trim().toUpperCase(Locale.ROOT)));
        }
        if (StringUtils.hasText(instance.getProtocol())) {
            builder.protocol(RerankProtocol.valueOf(instance.getProtocol().trim().toUpperCase(Locale.ROOT)));
        }
        return RerankPortResolver.create(builder.build());
    }

    private static boolean isConfigured(RerankModelInstance instance) {
        return instance != null
                && (StringUtils.hasText(instance.getVendor()) || StringUtils.hasText(instance.getProtocol()));
    }
}
