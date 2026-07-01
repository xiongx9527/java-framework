package com.xx9527.fw.plus.ai.autoconfigure.magicapi.plugin.qdrant.starter;

import com.xx9527.fw.plus.ai.autoconfigure.magicapi.plugin.qdrant.model.MagicDynamicQdrantDataSource;
import com.xx9527.fw.plus.ai.autoconfigure.magicapi.plugin.qdrant.module.QdrantModule;
import com.xx9527.fw.plus.ai.autoconfigure.magicapi.plugin.qdrant.service.QdrantDataSourceInfoMagicResourceStorage;
import com.xx9527.fw.plus.ai.autoconfigure.magicapi.plugin.qdrant.service.QdrantDataSourceMagicDynamicRegistry;
import com.xx9527.fw.plus.ai.autoconfigure.magicapi.plugin.qdrant.web.MagicQdrantController;
import dev.langchain4j.model.embedding.EmbeddingModel;
import io.qdrant.client.QdrantClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.ssssssss.magicapi.core.config.MagicAPIProperties;
import org.ssssssss.magicapi.core.config.MagicPluginConfiguration;
import org.ssssssss.magicapi.core.model.Plugin;
import org.ssssssss.magicapi.core.web.MagicControllerRegister;

@Slf4j
@ConditionalOnProperty(prefix = "magic-api.plugin.qdrant", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnClass(name = {"io.qdrant.client.QdrantClient","dev.langchain4j.store.embedding.qdrant.QdrantEmbeddingStore"})
@EnableConfigurationProperties(MagicAPIQdrantConfig.class)
@Configuration
public class MagicAPIQdrantConfiguration implements MagicPluginConfiguration {

    /** MagicAPI配置属性 */
    private final MagicAPIProperties properties;

    /**
     * 构造函数
     * 
     * @param properties MagicAPI配置属性
     */
    public MagicAPIQdrantConfiguration(MagicAPIProperties properties) {
        this.properties = properties;
    }

    /**
     * 创建动态Qdrant数据源Bean
     * 
     * @return 动态Qdrant数据源实例
     */
    @Bean
    @ConditionalOnMissingBean
    public MagicDynamicQdrantDataSource magicDynamicQdrantDataSource() {
        return new MagicDynamicQdrantDataSource();
    }

    /**
     * 创建Qdrant模块Bean
     * 
     * @param magicDynamicQdrantDataSource 动态Qdrant数据源
     * @param qdrantClient Qdrant客户端（可选）
     * @return Qdrant模块实例
     */
    @Bean
    @DependsOn(value={"magicDynamicQdrantDataSource"})
    public QdrantModule qdrantModule(MagicDynamicQdrantDataSource magicDynamicQdrantDataSource,
                                     @Autowired(required = false) QdrantClient qdrantClient,
                                     @Autowired(required = false) EmbeddingModel embeddingModel) {
        return new QdrantModule(magicDynamicQdrantDataSource, qdrantClient, embeddingModel);
    }

    /**
     * 创建Qdrant数据源信息资源存储Bean
     * 
     * @return Qdrant数据源信息资源存储实例
     */
    @Bean
    public QdrantDataSourceInfoMagicResourceStorage qdrantDataSourceInfoMagicResourceStorage() {
        return new QdrantDataSourceInfoMagicResourceStorage();
    }

    /**
     * 创建Qdrant数据源动态注册器Bean
     * 
     * @param qdrantDataSourceInfoMagicResourceStorage Qdrant数据源信息资源存储
     * @param magicDynamicQdrantDataSource 动态Qdrant数据源
     * @return Qdrant数据源动态注册器实例
     */
    @Bean
    @ConditionalOnMissingBean
    public QdrantDataSourceMagicDynamicRegistry qdrantDataSourceMagicDynamicRegistry(QdrantDataSourceInfoMagicResourceStorage qdrantDataSourceInfoMagicResourceStorage,
                                                                                     MagicDynamicQdrantDataSource magicDynamicQdrantDataSource) {
        return new QdrantDataSourceMagicDynamicRegistry(qdrantDataSourceInfoMagicResourceStorage,
                magicDynamicQdrantDataSource);
    }

    /**
     * 获取插件信息
     * 
     * @return 插件信息实例
     */
    @Override
    public Plugin plugin() {
        return new Plugin("qdrant", "MagicQdrant", "magic-qdrant.1.0.0.iife.js");
    }

    /**
     * 获取控制器注册器
     * 
     * @return 控制器注册器实例
     */
    @Override
    public MagicControllerRegister controllerRegister() {
        return (mapping, configuration) -> mapping.registerController(new MagicQdrantController(configuration));
    }

}
