package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.vector.starter;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.vector.model.MagicDynamicVectorDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.vector.module.VectorModule;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.vector.service.VectorDataSourceInfoMagicResourceStorage;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.vector.service.VectorDataSourceMagicDynamicRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.vector.web.MagicVectorController;
import com.xiongx9527.fw.plus.ai.langchain4j.store.embeding.EmbeddingStoreFactory;
import com.xiongx9527.fw.plus.ai.langchain4j.store.embeding.EmbeddingStoreProviderAdapter;
import com.xiongx9527.fw.plus.ai.langchain4j.store.embeding.EmbeddingStoreRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.ssssssss.magicapi.core.config.MagicPluginConfiguration;
import org.ssssssss.magicapi.core.model.Plugin;
import org.ssssssss.magicapi.core.web.MagicControllerRegister;

import java.util.List;

@Configuration
@ConditionalOnClass(EmbeddingStoreRegistry.class)
@ConditionalOnBean(EmbeddingStoreRegistry.class)
@ConditionalOnProperty(prefix = "magic-api.plugin.vector", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MagicAPIVectorConfiguration implements MagicPluginConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public MagicDynamicVectorDataSource magicDynamicVectorDataSource(EmbeddingStoreRegistry registry,
                                                                    EmbeddingStoreFactory embeddingStoreFactory,
                                                                    List<EmbeddingStoreProviderAdapter> providerAdapters) {
        return new MagicDynamicVectorDataSource(registry, embeddingStoreFactory, providerAdapters);
    }

    @Bean
    public VectorModule vectorModule(MagicDynamicVectorDataSource magicDynamicVectorDataSource) {
        return new VectorModule(magicDynamicVectorDataSource);
    }

    @Bean
    @ConditionalOnMissingBean
    public VectorDataSourceInfoMagicResourceStorage vectorDataSourceInfoMagicResourceStorage() {
        return new VectorDataSourceInfoMagicResourceStorage();
    }

    @Bean
    @ConditionalOnMissingBean
    public VectorDataSourceMagicDynamicRegistry vectorDataSourceMagicDynamicRegistry(VectorDataSourceInfoMagicResourceStorage storage,
                                                                                   MagicDynamicVectorDataSource dataSource) {
        return new VectorDataSourceMagicDynamicRegistry(storage, dataSource);
    }

    @Override
    public Plugin plugin() {
        return new Plugin("vector", "MagicVector", "magic-vector.1.0.0.iife.js");
    }

    @Override
    public MagicControllerRegister controllerRegister() {
        return (mapping, configuration) -> mapping.registerController(new MagicVectorController(configuration));
    }
}
