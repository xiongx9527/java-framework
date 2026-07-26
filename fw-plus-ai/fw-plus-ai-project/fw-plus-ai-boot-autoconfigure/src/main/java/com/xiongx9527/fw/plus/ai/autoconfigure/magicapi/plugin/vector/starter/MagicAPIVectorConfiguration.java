package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.vector.starter;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.vector.module.VectorModule;
import com.xiongx9527.fw.plus.ai.langchain4j.store.embeding.EmbeddingStoreRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.ssssssss.magicapi.core.config.MagicPluginConfiguration;
import org.ssssssss.magicapi.core.model.Plugin;

@Configuration
@ConditionalOnClass(EmbeddingStoreRegistry.class)
@ConditionalOnBean(EmbeddingStoreRegistry.class)
@ConditionalOnProperty(prefix = "magic-api.plugin.vector", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MagicAPIVectorConfiguration implements MagicPluginConfiguration {

    @Bean
    public VectorModule vectorModule(EmbeddingStoreRegistry registry) {
        return new VectorModule(registry);
    }

    @Override
    public Plugin plugin() {
        return new Plugin("vector", "MagicVector", "magic-vector.1.0.0.iife.js");
    }
}
