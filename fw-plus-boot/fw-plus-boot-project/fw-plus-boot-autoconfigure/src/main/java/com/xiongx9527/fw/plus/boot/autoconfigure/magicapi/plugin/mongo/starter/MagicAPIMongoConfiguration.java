package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mongo.starter;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mongo.model.MagicDynamicMongoDataSource;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mongo.module.MongoModule;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mongo.service.MongoDataSourceInfoMagicResourceStorage;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mongo.service.MongoDataSourceMagicDynamicRegistry;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mongo.web.MagicMongoController;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.ssssssss.magicapi.core.config.MagicPluginConfiguration;
import org.ssssssss.magicapi.core.model.Plugin;
import org.ssssssss.magicapi.core.web.MagicControllerRegister;
import org.ssssssss.magicapi.mongo.MongoCollectionExtension;
import org.ssssssss.magicapi.mongo.MongoFindIterableExtension;
import org.ssssssss.magicapi.mongo.MongoFunction;
import org.ssssssss.script.reflection.JavaReflection;

@ConditionalOnProperty(prefix = "magic-api.plugin.mogno", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnClass(name = {"org.springframework.data.mongodb.core.MongoTemplate","org.ssssssss.magicapi.mongo.MongoModule"})
@AutoConfigureBefore(MongoAutoConfiguration.class)
@Configuration
public class MagicAPIMongoConfiguration implements MagicPluginConfiguration {

    /**
     * 覆盖 Spring Boot 自动配置的 MongoClient，设置为懒加载
     * 避免在未配置 MongoDB 时自动连接
     */
    @Bean
    @Lazy
    @ConditionalOnMissingBean(MongoClient.class)
    public MongoClient mongoClient(MongoClientSettings settings) {
        return MongoClients.create(settings);
    }

    /**
     * 创建Mongo数据源动态管理器Bean
     * 用于动态创建、管理和销毁Mongo数据源连接
     *
     * @return MagicDynamicMongoDataSource Mongo数据源动态管理器实例
     */
    @Bean
    public MagicDynamicMongoDataSource magicDynamicMongoDataSource(){
        return new MagicDynamicMongoDataSource();
    }

    /**
     * 创建Mongo数据源动态注册器Bean
     * 用于动态注册和管理Mongo数据源，当容器中不存在该Bean时才会创建
     * 
     * @param mongoDataSourceInfoMagicResourceStorage Mongo数据源信息资源存储器，用于存储数据源配置信息
     * @param magicDynamicMongoDataSource Mongo数据源动态管理器，用于动态创建和管理Mongo数据源连接
     * @return MongoDataSourceMagicDynamicRegistry Mongo数据源动态注册器实例
     */
    @Bean
    @ConditionalOnMissingBean
    public MongoDataSourceMagicDynamicRegistry mongoDataSourceMagicDynamicRegistry(MongoDataSourceInfoMagicResourceStorage mongoDataSourceInfoMagicResourceStorage,
                                                                                   MagicDynamicMongoDataSource magicDynamicMongoDataSource) {
        return new MongoDataSourceMagicDynamicRegistry(mongoDataSourceInfoMagicResourceStorage,
                magicDynamicMongoDataSource);
    }

    /**
     * 创建Mongo数据源信息资源存储器Bean
     * 用于存储和管理Mongo数据源的配置信息
     * 
     * @return MongoDataSourceInfoMagicResourceStorage Mongo数据源信息资源存储器实例
     */
    @Bean
    public MongoDataSourceInfoMagicResourceStorage mongoDataSourceInfoMagicResourceStorage() {
        return new MongoDataSourceInfoMagicResourceStorage();
    }

    /**
     * 定义插件信息
     * 返回当前Redis插件的元数据信息
     *
     * @return Plugin 插件信息对象，包含插件名称、显示名称和JavaScript文件路径
     */
    @Override
    public Plugin plugin() {
        return new Plugin("mongo", "MagicMongo", "magic-mongo.1.0.0.iife.js");
    }

    /**
     * 创建Mongo模块Bean
     * 用于在MagicAPI中提供Mongo数据库操作功能，当容器中不存在该Bean时才会创建
     * 同时注册MongoCollection和FindIterable的扩展方法
     * 
     * @param mongoTemplate Spring Data Mongo模板，用于执行MongoDB操作
     * @param magicDynamicMongoDataSource Mongo数据源动态管理器，用于动态管理Mongo连接
     * @return MongoModule Mongo模块实例
     */
    @Bean
    @ConditionalOnMissingBean
    public MongoModule mongoFunctions(@Lazy MongoTemplate mongoTemplate,MagicDynamicMongoDataSource magicDynamicMongoDataSource) {
        JavaReflection.registerMethodExtension(MongoCollection.class, new MongoCollectionExtension());
        JavaReflection.registerMethodExtension(FindIterable.class, new MongoFindIterableExtension());
        return new MongoModule(mongoTemplate,magicDynamicMongoDataSource);
    }

    /**
     * 创建Mongo函数Bean
     * 用于提供Mongo数据库操作相关的函数功能
     * 
     * @return MongoFunction Mongo函数实例
     */
    @Bean
    public MongoFunction mongoFunction(){
        return new MongoFunction();
    }

    /**
     * 控制器注册方法
     * 用于在MagicAPI中注册MongoDB相关的控制器
     * 
     * @return MagicControllerRegister 控制器注册器，用于注册MagicMongoController控制器
     */
    @Override
    public MagicControllerRegister controllerRegister() {
        return (mapping, configuration) -> mapping.registerController(new MagicMongoController(configuration));
    }

}
