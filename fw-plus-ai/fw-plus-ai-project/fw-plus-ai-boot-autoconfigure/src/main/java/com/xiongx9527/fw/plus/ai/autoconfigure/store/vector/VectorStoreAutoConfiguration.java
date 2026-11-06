package com.xiongx9527.fw.plus.ai.autoconfigure.store.vector;

import com.xiongx9527.fw.plus.ai.autoconfigure.store.AbstractStoreAutoConfiguration;
import com.xiongx9527.fw.plus.ai.autoconfigure.store.AbstractStoreInstance;
import com.xiongx9527.fw.plus.ai.base.store.vector.VectorStorePort;
import com.xiongx9527.fw.plus.ai.nativeimpl.store.StoreConfig;
import com.xiongx9527.fw.plus.ai.nativeimpl.store.vector.PointIdStrategy;
import com.xiongx9527.fw.plus.ai.nativeimpl.store.vector.UnconfiguredVectorStorePort;
import com.xiongx9527.fw.plus.ai.nativeimpl.store.vector.VectorStoreConfig;
import com.xiongx9527.fw.plus.ai.nativeimpl.store.vector.VectorStoreProtocol;
import com.xiongx9527.fw.plus.ai.nativeimpl.store.vector.VectorStoreResolver;
import com.xiongx9527.fw.plus.ai.nativeimpl.store.vector.VectorStoreVendor;
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
 * 向量库自动装配（多实例）：绑定 {@link VectorStoreProperties}，暴露一个 {@code @Primary} 的默认
 * {@link VectorStorePort}（未配置时为占位端口）与全部实例的 {@code Map<String, VectorStorePort>}
 * （key 含 {@code default} 与各命名实例）。健康检查由 {@code StoreHealthConfiguration} 统一附加。
 */
@Configuration
@ConditionalOnClass(VectorStorePort.class)
@ConditionalOnProperty(prefix = "fw-plus.ai.vector-store", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(VectorStoreProperties.class)
public class VectorStoreAutoConfiguration extends AbstractStoreAutoConfiguration<VectorStorePort> {

    private static final Logger log = LoggerFactory.getLogger(VectorStoreAutoConfiguration.class);

    private final VectorStoreProperties properties;

    public VectorStoreAutoConfiguration(VectorStoreProperties properties) {
        this.properties = properties;
    }

    @Bean
    @Primary
    @ConditionalOnMissingBean(VectorStorePort.class)
    public VectorStorePort vectorStorePort() {
        return resolve(properties.getDefault());
    }

    @Bean
    @ConditionalOnMissingBean(name = "vectorStorePorts")
    public Map<String, VectorStorePort> vectorStorePorts() {
        Map<String, VectorStorePort> map = new LinkedHashMap<String, VectorStorePort>();
        map.put("default", resolve(properties.getDefault()));
        for (Map.Entry<String, VectorStoreInstance> e : properties.getInstances().entrySet()) {
            map.put(e.getKey(), resolve(e.getValue()));
        }
        return map;
    }

    @Override
    protected VectorStorePort buildPlaceholder(AbstractStoreInstance instance) {
        log.info("vector store instance not configured; exposing an unconfigured placeholder port");
        return new UnconfiguredVectorStorePort();
    }

    @Override
    protected VectorStorePort buildConfigured(AbstractStoreInstance instance) {
        VectorStoreInstance v = (VectorStoreInstance) instance;
        StoreConfig common = v.commonBuilder().build();
        VectorStoreConfig.Builder builder = VectorStoreConfig.builder()
                .common(common)
                .pointIdStrategy(v.resolvePointIdStrategy());
        if (StringUtils.hasText(v.getVendor())) {
            builder.vendor(VectorStoreVendor.valueOf(v.getVendor().trim().toUpperCase(Locale.ROOT)));
        }
        if (StringUtils.hasText(v.getProtocol())) {
            builder.protocol(VectorStoreProtocol.valueOf(v.getProtocol().trim().toUpperCase(Locale.ROOT)));
        }
        VectorStorePort port = VectorStoreResolver.create(builder.build());
        log.info("vector store configured: vendor={}, protocol={}, baseUrl={}",
                v.getVendor(), v.getProtocol(), v.getBaseUrl());
        return port;
    }
}
