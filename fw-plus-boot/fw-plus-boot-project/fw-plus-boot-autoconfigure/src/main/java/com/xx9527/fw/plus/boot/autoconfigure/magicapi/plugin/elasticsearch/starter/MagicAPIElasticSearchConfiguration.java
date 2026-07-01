package com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.elasticsearch.starter;

import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.elasticsearch.model.MagicDynamicElasticSearchDataSource;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.elasticsearch.module.ElasticSearchModule;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.elasticsearch.service.ElasticsearchDataSourceInfoMagicResourceStorage;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.elasticsearch.service.ElasticsearchDataSourceMagicDynamicRegistry;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.elasticsearch.web.MagicElasticsearchController;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.redis.module.RedisModule;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.redis.web.MagicRedisController;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.ssssssss.magicapi.core.config.MagicAPIProperties;
import org.ssssssss.magicapi.core.config.MagicPluginConfiguration;
import org.ssssssss.magicapi.core.model.Plugin;
import org.ssssssss.magicapi.core.web.MagicControllerRegister;

@ConditionalOnProperty(prefix = "magic-api.plugin.elasticsearch", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnClass(name = {"org.elasticsearch.client.RestClient"})
@Configuration
public class MagicAPIElasticSearchConfiguration implements MagicPluginConfiguration {

    private final MagicAPIProperties properties;

    public MagicAPIElasticSearchConfiguration(MagicAPIProperties properties) {
        this.properties = properties;
    }

    @Bean
    @ConditionalOnMissingBean
    public MagicDynamicElasticSearchDataSource magicDynamicElasticSearchDataSource() {
        MagicDynamicElasticSearchDataSource magicDynamicElasticSearchDataSource = new MagicDynamicElasticSearchDataSource();
        return magicDynamicElasticSearchDataSource;
    }

    /**
     * 创建ElasticSearch模块实例
     *
     * @param magicDynamicElasticSearchDataSource 动态ElasticSearch数据源实例
     * @param restClient                          REST客户端实例
     * @return ElasticSearchModule 模块实例
     */
    @Primary
    @Bean
    @DependsOn(value={"magicDynamicElasticSearchDataSource"})
    public ElasticSearchModule elasticSearchModule(MagicDynamicElasticSearchDataSource magicDynamicElasticSearchDataSource, @Autowired(required =false) RestClient restClient) {
        return new ElasticSearchModule(magicDynamicElasticSearchDataSource,restClient);
    }

    @Bean
    public ElasticsearchDataSourceInfoMagicResourceStorage elasticsearchDataSourceInfoMagicResourceStorage() {
        return new ElasticsearchDataSourceInfoMagicResourceStorage();
    }

    @Bean
    @ConditionalOnMissingBean
    public ElasticsearchDataSourceMagicDynamicRegistry elasticsearchDataSourceMagicDynamicRegistry(ElasticsearchDataSourceInfoMagicResourceStorage elasticsearchDataSourceInfoMagicResourceStorage,
                                                                                                   MagicDynamicElasticSearchDataSource magicDynamicElasticSearchDataSource) {
        return new ElasticsearchDataSourceMagicDynamicRegistry(elasticsearchDataSourceInfoMagicResourceStorage,
                magicDynamicElasticSearchDataSource);
    }


    /**
     * 定义插件信息
     * 返回当前Redis插件的元数据信息
     *
     * @return Plugin 插件信息对象，包含插件名称、显示名称和JavaScript文件路径
     */
    @Override
    public Plugin plugin() {
        return new Plugin("elasticsearch", "MagicElasticsearch", "magic-elasticsearch.1.0.0.iife.js");
    }


    @Override
    public MagicControllerRegister controllerRegister() {
        // 返回一个Lambda表达式，实现MagicControllerRegister接口
        // 该表达式创建新的MagicRedisController实例并将其注册到mapping中
        return (mapping, configuration) -> mapping.registerController(new MagicElasticsearchController(configuration));
    }



}
