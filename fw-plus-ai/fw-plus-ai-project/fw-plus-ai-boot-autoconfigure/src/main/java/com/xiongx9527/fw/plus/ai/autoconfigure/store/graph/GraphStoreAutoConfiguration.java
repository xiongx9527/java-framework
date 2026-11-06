package com.xiongx9527.fw.plus.ai.autoconfigure.store.graph;

import com.xiongx9527.fw.plus.ai.autoconfigure.store.AbstractStoreAutoConfiguration;
import com.xiongx9527.fw.plus.ai.autoconfigure.store.AbstractStoreInstance;
import com.xiongx9527.fw.plus.ai.base.store.graph.GraphStorePort;
import com.xiongx9527.fw.plus.ai.nativeimpl.store.graph.GraphStoreConfig;
import com.xiongx9527.fw.plus.ai.nativeimpl.store.graph.GraphStoreProtocol;
import com.xiongx9527.fw.plus.ai.nativeimpl.store.graph.GraphStoreResolver;
import com.xiongx9527.fw.plus.ai.nativeimpl.store.graph.GraphStoreVendor;
import com.xiongx9527.fw.plus.ai.nativeimpl.store.graph.UnconfiguredGraphStorePort;
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
 * 图库自动装配（多实例）：绑定 {@link GraphStoreProperties}，暴露一个 {@code @Primary} 的默认
 * {@link GraphStorePort}（未配置时为占位端口）与全部实例的 {@code Map<String, GraphStorePort>}
 * （key 含 {@code default} 与各命名实例）。健康检查由 {@code StoreHealthConfiguration} 统一附加。
 */
@Configuration
@ConditionalOnClass(GraphStorePort.class)
@ConditionalOnProperty(prefix = "fw-plus.ai.graph-store", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(GraphStoreProperties.class)
public class GraphStoreAutoConfiguration extends AbstractStoreAutoConfiguration<GraphStorePort> {

    private static final Logger log = LoggerFactory.getLogger(GraphStoreAutoConfiguration.class);

    private final GraphStoreProperties properties;

    public GraphStoreAutoConfiguration(GraphStoreProperties properties) {
        this.properties = properties;
    }

    @Bean
    @Primary
    @ConditionalOnMissingBean(GraphStorePort.class)
    public GraphStorePort graphStorePort() {
        return resolve(properties.getDefaultInstance());
    }

    @Bean
    @ConditionalOnMissingBean(name = "graphStorePorts")
    public Map<String, GraphStorePort> graphStorePorts() {
        Map<String, GraphStorePort> map = new LinkedHashMap<String, GraphStorePort>();
        map.put("default", resolve(properties.getDefaultInstance()));
        for (Map.Entry<String, GraphStoreInstance> e : properties.getInstances().entrySet()) {
            map.put(e.getKey(), resolve(e.getValue()));
        }
        return map;
    }

    @Override
    protected GraphStorePort buildPlaceholder(AbstractStoreInstance instance) {
        log.info("graph store instance not configured; exposing an unconfigured placeholder port");
        return new UnconfiguredGraphStorePort();
    }

    @Override
    protected GraphStorePort buildConfigured(AbstractStoreInstance instance) {
        GraphStoreInstance g = (GraphStoreInstance) instance;
        GraphStoreConfig.Builder builder = GraphStoreConfig.builder().common(g.commonBuilder().build());
        if (StringUtils.hasText(g.getVendor())) {
            builder.vendor(GraphStoreVendor.valueOf(g.getVendor().trim().toUpperCase(Locale.ROOT)));
        }
        if (StringUtils.hasText(g.getProtocol())) {
            builder.protocol(GraphStoreProtocol.valueOf(g.getProtocol().trim().toUpperCase(Locale.ROOT)));
        }
        GraphStorePort port = GraphStoreResolver.create(builder.build());
        log.info("graph store configured: vendor={}, protocol={}, baseUrl={}",
                g.getVendor(), g.getProtocol(), g.getBaseUrl());
        return port;
    }
}
