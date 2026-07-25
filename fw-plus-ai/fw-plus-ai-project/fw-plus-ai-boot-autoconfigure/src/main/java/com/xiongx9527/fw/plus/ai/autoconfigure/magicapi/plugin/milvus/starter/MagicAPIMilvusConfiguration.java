package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.milvus.starter;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.milvus.model.MagicDynamicMilvusDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.milvus.module.MilvusModule;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.milvus.service.MilvusDataSourceInfoMagicResourceStorage;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.milvus.service.MilvusDataSourceMagicDynamicRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.milvus.web.MagicMilvusController;
import com.xiongx9527.fw.plus.ai.langchain4j.store.embeding.EmbeddingStoreRegistry;
import dev.langchain4j.model.embedding.EmbeddingModel;
import io.milvus.v2.client.MilvusClientV2;
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
@ConditionalOnProperty(prefix = "magic-api.plugin.milvus", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnClass(name = {"io.milvus.v2.client.MilvusClientV2","dev.langchain4j.store.embedding.milvus.MilvusEmbeddingStore"})
@EnableConfigurationProperties(MagicAPIMilvueConfig.class)
@Configuration
@Deprecated
public class MagicAPIMilvusConfiguration implements MagicPluginConfiguration {

    /** 
     * MagicAPI配置属性
     * 用于获取MagicAPI相关的配置信息
     */
    private final MagicAPIProperties properties;

    /**
     * 构造函数，初始化MagicAPI配置属性
     *
     * @param properties MagicAPI配置属性对象
     */
    public MagicAPIMilvusConfiguration(MagicAPIProperties properties) {
        this.properties = properties;
    }


    /**
     * 创建MagicDynamicMilvusDataSource Bean实例
     * 当容器中不存在MagicDynamicMilvusDataSource类型的Bean时，创建并返回一个新的实例
     *
     * @return MagicDynamicMilvusDataSource Milvus数据源管理器实例
     */
    @Bean
    @ConditionalOnMissingBean
    public MagicDynamicMilvusDataSource magicDynamicMilvusDataSource() {
        return new MagicDynamicMilvusDataSource();
    }

    /**
     * 创建Milvus模块的Bean实例
     * 该方法用于初始化并配置MilvusModule，依赖于动态数据源和其他可选组件
     *
     * @param magicDynamicMilvusDataSource Milvus动态数据源，用于管理多个Milvus连接
     * @param milvusClientV2 Milvus客户端V2版本，可选依赖，用于与Milvus服务进行交互
     * @param embeddingModel 嵌入模型，用于向量化处理
     * @return 配置好的MilvusModule实例
     */
    @Bean
    @DependsOn(value={"magicDynamicMilvusDataSource"})
    public MilvusModule milvusModule(MagicDynamicMilvusDataSource magicDynamicMilvusDataSource,
                                     @Autowired(required = false) MilvusClientV2 milvusClientV2,
                                     @Autowired(required = false) EmbeddingModel embeddingModel,
                                     @Autowired(required = false) EmbeddingStoreRegistry embeddingStoreRegistry) {
        return new MilvusModule(magicDynamicMilvusDataSource, milvusClientV2, embeddingModel, embeddingStoreRegistry);
    }


    /**
     * 创建Milvus数据源信息存储Bean实例
     * 用于存储和管理Milvus数据源的配置信息
     *
     * @return MilvusDataSourceInfoMagicResourceStorage Milvus数据源信息存储实例
     */
    @Bean
    public MilvusDataSourceInfoMagicResourceStorage milvusDataSourceInfoMagicResourceStorage() {
        return new MilvusDataSourceInfoMagicResourceStorage();
    }


    /**
     * 创建并配置Milvus数据源动态注册器Bean实例
     * 用于动态注册和管理Milvus数据源，当容器中不存在该类型Bean时创建新的实例
     *
     * @param milvusDataSourceInfoMagicResourceStorage Milvus数据源信息存储实例
     * @param magicDynamicMilvusDataSource Milvus数据源管理器实例
     * @return MilvusDataSourceMagicDynamicRegistry Milvus数据源动态注册器实例
     */
    @Bean
    @ConditionalOnMissingBean
    public MilvusDataSourceMagicDynamicRegistry milvusDataSourceMagicDynamicRegistry(MilvusDataSourceInfoMagicResourceStorage milvusDataSourceInfoMagicResourceStorage,
                                                                                     MagicDynamicMilvusDataSource magicDynamicMilvusDataSource) {
        return new MilvusDataSourceMagicDynamicRegistry(milvusDataSourceInfoMagicResourceStorage,
                magicDynamicMilvusDataSource);
    }


    /**
     * 定义插件信息
     * 返回当前Milvus插件的元数据信息
     *
     * @return Plugin 插件信息对象，包含插件名称、显示名称和JavaScript文件路径
     */
    @Override
    public Plugin plugin() {
        return new Plugin("milvus", "MagicMilvus", "magic-milvus.1.0.0.iife.js");
    }


    /**
     * 获取控制器注册器
     * 用于注册Milvus相关的控制器到MagicAPI中
     *
     * @return MagicControllerRegister 控制器注册器，负责将MagicMilvusController注册到系统中
     */
    @Override
    public MagicControllerRegister controllerRegister() {
        return (mapping, configuration) -> mapping.registerController(new MagicMilvusController(configuration));
    }

}
