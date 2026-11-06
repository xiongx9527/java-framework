package com.xiongx9527.fw.plus.ai.autoconfigure.model;

import com.xiongx9527.fw.plus.ai.base.model.LlmPort;
import com.xiongx9527.fw.plus.ai.nativeimpl.llm.LlmModelConfig;
import com.xiongx9527.fw.plus.ai.nativeimpl.llm.LlmPortResolver;
import com.xiongx9527.fw.plus.ai.nativeimpl.llm.LlmProtocol;
import com.xiongx9527.fw.plus.ai.nativeimpl.llm.LlmVendor;
import com.xiongx9527.fw.plus.ai.nativeimpl.llm.UnconfiguredLlmPort;
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
 * LLM 模型自动装配（多实例）：绑定 {@link LlmModelProperties}，暴露一个 {@code @Primary} 的默认
 * {@link LlmPort}（仅当默认实例配置了厂商或协议时创建）与全部实例的 {@code Map<String, LlmPort>}
 * （key 含 {@code default} 与各命名实例）。未配置时由 {@link LlmPortResolver} 决定具体端口，
 * 应用缺省不创建 Bean 即可正常启动。
 */
@Configuration
@ConditionalOnClass(LlmPort.class)
@ConditionalOnProperty(prefix = "fw-plus.ai.llm", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(LlmModelProperties.class)
public class LlmModelAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(LlmModelAutoConfiguration.class);

    private final LlmModelProperties properties;

    public LlmModelAutoConfiguration(LlmModelProperties properties) {
        this.properties = properties;
    }

    @Bean
    @Primary
    @ConditionalOnMissingBean(LlmPort.class)
    public LlmPort llmPort() {
        LlmModelInstance instance = properties.getDefault();
        if (!isConfigured(instance)) {
            log.info("llm model default instance not configured; exposing an unconfigured placeholder port");
            return new UnconfiguredLlmPort();
        }
        return resolve(instance);
    }

    @Bean
    @ConditionalOnMissingBean(name = "llmPorts")
    public Map<String, LlmPort> llmPorts() {
        Map<String, LlmPort> map = new LinkedHashMap<String, LlmPort>();
        LlmModelInstance def = properties.getDefault();
        map.put("default", isConfigured(def) ? resolve(def) : new UnconfiguredLlmPort());
        for (Map.Entry<String, LlmModelInstance> e : properties.getInstances().entrySet()) {
            map.put(e.getKey(), isConfigured(e.getValue()) ? resolve(e.getValue()) : new UnconfiguredLlmPort());
        }
        return map;
    }

    private LlmPort resolve(LlmModelInstance instance) {
        LlmModelConfig.Builder builder = LlmModelConfig.builder()
                .baseUrl(instance.getBaseUrl())
                .apiKey(instance.getApiKey())
                .defaultModel(instance.getDefaultModel())
                .apiVersion(instance.getApiVersion())
                .connectTimeoutMillis(instance.getConnectTimeoutMillis())
                .readTimeoutMillis(instance.getReadTimeoutMillis());
        if (StringUtils.hasText(instance.getVendor())) {
            builder.vendor(LlmVendor.valueOf(instance.getVendor().trim().toUpperCase(Locale.ROOT)));
        }
        if (StringUtils.hasText(instance.getProtocol())) {
            builder.protocol(LlmProtocol.valueOf(instance.getProtocol().trim().toUpperCase(Locale.ROOT)));
        }
        return LlmPortResolver.create(builder.build());
    }

    private static boolean isConfigured(LlmModelInstance instance) {
        return instance != null
                && (StringUtils.hasText(instance.getVendor()) || StringUtils.hasText(instance.getProtocol()));
    }
}
