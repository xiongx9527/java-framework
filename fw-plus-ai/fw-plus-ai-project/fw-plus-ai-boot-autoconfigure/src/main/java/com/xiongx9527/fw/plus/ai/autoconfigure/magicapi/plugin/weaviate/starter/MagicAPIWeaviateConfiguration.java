package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.weaviate.starter;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.weaviate.model.MagicDynamicWeaviateDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.weaviate.module.WeaviateModule;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.weaviate.service.WeaviateDataSourceInfoMagicResourceStorage;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.weaviate.service.WeaviateDataSourceMagicDynamicRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.weaviate.web.MagicWeaviateController;
import com.xiongx9527.fw.plus.ai.langchain4j.store.embeding.EmbeddingStoreRegistry;
import dev.langchain4j.model.embedding.EmbeddingModel;
import io.weaviate.client.WeaviateClient;
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
@ConditionalOnProperty(prefix = "magic-api.plugin.weaviate", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnClass(name = {"io.weaviate.client.WeaviateClient","dev.langchain4j.store.embedding.weaviate.WeaviateEmbeddingStore"})
@EnableConfigurationProperties(MagicAPIWeaviateConfig.class)
@Configuration
@Deprecated
public class MagicAPIWeaviateConfiguration implements MagicPluginConfiguration {

    /** MagicAPI配置属性 */
    private final MagicAPIProperties properties;

    /**
     * 构造函数
     * 
     * @param properties MagicAPI配置属性
     */
    public MagicAPIWeaviateConfiguration(MagicAPIProperties properties) {
        this.properties = properties;
    }


    /**
     * 创建MagicDynamicWeaviateDataSource实例的Bean工厂方法
     *
     * 该方法用于创建并注册MagicDynamicWeaviateDataSource的Bean实例。
     * 当Spring容器中不存在同类型的Bean时，才会创建新的实例。
     *
     * @return MagicDynamicWeaviateDataSource 返回新创建的数据源实例
     */
    @Bean
    @ConditionalOnMissingBean
    public MagicDynamicWeaviateDataSource magicDynamicWeaviateDataSource() {
        return new MagicDynamicWeaviateDataSource();
    }

    /**
     * 创建并配置WeaviateModule实例的工厂方法
     *
     * @param magicDynamicWeaviateDataSource Weaviate数据源，用于连接和操作Weaviate数据库
     * @param weaviateClient Weaviate客户端，用于与Weaviate服务进行通信（可选）
     * @param embeddingModel 嵌入模型，用于文本向量化处理（可选）
     * @return 配置好的WeaviateModule实例
     */
    @Bean
    @DependsOn(value={"magicDynamicWeaviateDataSource"})
    public WeaviateModule weaviateModule(MagicDynamicWeaviateDataSource magicDynamicWeaviateDataSource,
                                         @Autowired(required = false) WeaviateClient weaviateClient,
                                         @Autowired(required = false) EmbeddingModel embeddingModel,
                                         @Autowired(required = false) EmbeddingStoreRegistry embeddingStoreRegistry) {
        return new WeaviateModule(magicDynamicWeaviateDataSource, weaviateClient, embeddingModel, embeddingStoreRegistry);
    }

    /**
     * 创建并配置Weaviate数据源信息魔法资源存储的Bean实例
     *
     * @return WeaviateDataSourceInfoMagicResourceStorage实例，用于处理Weaviate数据源的资源存储操作
     */
    @Bean
    public WeaviateDataSourceInfoMagicResourceStorage weaviateDataSourceInfoMagicResourceStorage() {
        return new WeaviateDataSourceInfoMagicResourceStorage();
    }

    /**
     * 创建Weaviate数据源动态注册器Bean
     *
     * @param weaviateDataSourceInfoMagicResourceStorage Weaviate数据源信息存储器
     * @param magicDynamicWeaviateDataSource 动态Weaviate数据源实例
     * @return Weaviate数据源动态注册器实例
     */
    @Bean
    @ConditionalOnMissingBean
    public WeaviateDataSourceMagicDynamicRegistry weaviateDataSourceMagicDynamicRegistry(WeaviateDataSourceInfoMagicResourceStorage weaviateDataSourceInfoMagicResourceStorage,
                                                                                         MagicDynamicWeaviateDataSource magicDynamicWeaviateDataSource) {
        return new WeaviateDataSourceMagicDynamicRegistry(weaviateDataSourceInfoMagicResourceStorage,
                magicDynamicWeaviateDataSource);
    }

    /**
     * 获取插件信息
     * 
     * @return 插件信息实例
     */
    @Override
    public Plugin plugin() {
        return new Plugin("weaviate", "MagicWeaviate", "magic-weaviate.1.0.0.iife.js");
    }

    /**
     * 获取控制器注册器
     * 
     * @return 控制器注册器实例
     */
    @Override
    public MagicControllerRegister controllerRegister() {
        return (mapping, configuration) -> mapping.registerController(new MagicWeaviateController(configuration));
    }

}
