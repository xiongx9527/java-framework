package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.model.starter;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.model.model.MagicDynamicModelDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.model.module.ModelModule;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.model.service.ModelDataSourceInfoMagicResourceStorage;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.model.service.ModelDataSourceMagicDynamicRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.model.web.MagicModelController;
import com.xiongx9527.fw.plus.ai.model.AiModelAdapter;
import com.xiongx9527.fw.plus.ai.model.AiModelFactory;
import com.xiongx9527.fw.plus.ai.model.AiModelRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.ssssssss.magicapi.core.config.MagicPluginConfiguration;
import org.ssssssss.magicapi.core.model.Plugin;
import org.ssssssss.magicapi.core.web.MagicControllerRegister;

import java.util.List;

@Configuration
@ConditionalOnClass(AiModelRegistry.class)
@ConditionalOnBean(AiModelRegistry.class)
@ConditionalOnProperty(prefix = "magic-api.plugin.model", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(MagicAPIModelConfig.class)
public class MagicAPIModelConfiguration implements MagicPluginConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public MagicDynamicModelDataSource magicDynamicModelDataSource(AiModelRegistry registry,
                                                                    AiModelFactory modelFactory,
                                                                    List<AiModelAdapter> providerAdapters) {
        return new MagicDynamicModelDataSource(registry, modelFactory, providerAdapters);
    }

    @Bean
    public ModelModule modelModule(MagicDynamicModelDataSource magicDynamicModelDataSource) {
        return new ModelModule(magicDynamicModelDataSource);
    }

    @Bean
    @ConditionalOnMissingBean
    public ModelDataSourceInfoMagicResourceStorage modelDataSourceInfoMagicResourceStorage() {
        return new ModelDataSourceInfoMagicResourceStorage();
    }

    @Bean
    @ConditionalOnMissingBean
    public ModelDataSourceMagicDynamicRegistry modelDataSourceMagicDynamicRegistry(
            ModelDataSourceInfoMagicResourceStorage storage, MagicDynamicModelDataSource dataSource) {
        return new ModelDataSourceMagicDynamicRegistry(storage, dataSource);
    }

    @Override
    public Plugin plugin() {
        return new Plugin("model", "MagicModel", "magic-model.1.0.0.iife.js");
    }

    @Override
    public MagicControllerRegister controllerRegister() {
        return (mapping, configuration) -> mapping.registerController(new MagicModelController(configuration));
    }
}
