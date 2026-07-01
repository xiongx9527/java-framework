package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.influx.starter;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.influx.model.MagicDynamicInfluxDataSource;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.influx.module.InfluxModule;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.influx.service.InfluxDataSourceInfoMagicResourceStorage;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.influx.service.InfluxDataSourceMagicDynamicRegistry;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.influx.web.MagicInfluxController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Primary;
import org.ssssssss.magicapi.core.config.MagicAPIProperties;
import org.ssssssss.magicapi.core.config.MagicPluginConfiguration;
import org.ssssssss.magicapi.core.model.Plugin;
import org.ssssssss.magicapi.core.web.MagicControllerRegister;

@ConditionalOnProperty(prefix = "magic-api.plugin.influx", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnClass(name = {"com.influxdb.client.InfluxDBClient"})
@Configuration
public class MagicAPIInfluxConfiguration implements MagicPluginConfiguration {

    /**
     * MagicAPI属性配置对象
     */
    private final MagicAPIProperties properties;

    /**
     * 构造函数，用于创建MagicAPIInfluxConfiguration实例
     *
     * @param properties MagicAPI的配置属性对象，包含相关的配置信息
     */
    public MagicAPIInfluxConfiguration(MagicAPIProperties properties) {
        this.properties = properties;
    }


    /**
     * 创建MagicDynamicInfluxDataSource实例的Bean
     * 当容器中不存在该Bean时才会创建
     *
     * @return MagicDynamicInfluxDataSource实例
     */
    @Bean
    @ConditionalOnMissingBean
    public MagicDynamicInfluxDataSource magicDynamicInfluxDataSource() {
        MagicDynamicInfluxDataSource magicDynamicInfluxDataSource = new MagicDynamicInfluxDataSource();
        return magicDynamicInfluxDataSource;
    }

    /**
     * 创建InfluxModule实例的Bean，并设置为主要Bean
     * 依赖于magicDynamicInfluxDataSource Bean的初始化
     *
     * @param magicDynamicNebulaDataSource 动态数据源实例
     * @param InfluxDBClient InfluxDB客户端实例（可选）
     * @return InfluxModule实例
     */
    @Primary
    @Bean
    @DependsOn(value={"magicDynamicInfluxDataSource"})
    public InfluxModule influxModule(MagicDynamicInfluxDataSource magicDynamicNebulaDataSource,
                                    @Autowired(required =false) com.influxdb.client.InfluxDBClient InfluxDBClient) {
        return new InfluxModule(InfluxDBClient,magicDynamicNebulaDataSource);
    }

    /**
     * 创建InfluxDataSourceInfoMagicResourceStorage实例的Bean
     * 用于存储Influx数据源信息的资源存储器
     *
     * @return InfluxDataSourceInfoMagicResourceStorage实例
     */
    @Bean
    public InfluxDataSourceInfoMagicResourceStorage influxDataSourceInfoMagicResourceStorage() {
        return new InfluxDataSourceInfoMagicResourceStorage();
    }

    /**
     * 创建InfluxDataSourceMagicDynamicRegistry实例的Bean
     * 当容器中不存在该Bean时才会创建
     * 用于注册和管理Influx数据源的动态注册器
     *
     * @param influxDataSourceInfoMagicResourceStorage 数据源信息资源存储器
     * @param magicDynamicInfluxDataSource 动态Influx数据源实例
     * @return InfluxDataSourceMagicDynamicRegistry实例
     */
    @Bean
    @ConditionalOnMissingBean
    public InfluxDataSourceMagicDynamicRegistry influxDataSourceMagicDynamicRegistry(InfluxDataSourceInfoMagicResourceStorage influxDataSourceInfoMagicResourceStorage,
                                                                                     MagicDynamicInfluxDataSource magicDynamicInfluxDataSource) {
        return new InfluxDataSourceMagicDynamicRegistry(influxDataSourceInfoMagicResourceStorage,
                magicDynamicInfluxDataSource);
    }

    /**
     * 获取插件信息
     * 返回当前Influx插件的基本配置信息
     *
     * @return Plugin插件实例，包含插件名称、显示名称和JavaScript文件路径
     */
    @Override
    public Plugin plugin() {
        return new Plugin("influx", "MagicInflux", "magic-influx.1.0.0.iife.js");
    }

    /**
     * 获取控制器注册器
     * 提供MagicInfluxController的注册功能
     *
     * @return MagicControllerRegister控制器注册器实例
     */
    @Override
    public MagicControllerRegister controllerRegister() {
        return (mapping, configuration) -> mapping.registerController(new MagicInfluxController(configuration));
    }


}
