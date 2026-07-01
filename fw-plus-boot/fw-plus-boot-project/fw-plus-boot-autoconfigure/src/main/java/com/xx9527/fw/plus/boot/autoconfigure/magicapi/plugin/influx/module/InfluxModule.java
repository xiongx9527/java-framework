package com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.influx.module;

import com.influxdb.client.InfluxDBClient;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.influx.model.MagicDynamicInfluxDataSource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.ssssssss.magicapi.core.annotation.MagicModule;
import org.ssssssss.magicapi.core.model.Options;
import org.ssssssss.magicapi.modules.DynamicModule;
import org.ssssssss.script.MagicScriptContext;
import org.ssssssss.script.functions.DynamicAttribute;

@Slf4j
@MagicModule("influx")
public class InfluxModule implements DynamicAttribute<InfluxModule, InfluxModule>, DynamicModule<InfluxModule> {

    /**
     * InfluxDB客户端实例，用于与InfluxDB数据库进行交互操作
     */
    private final com.influxdb.client.InfluxDBClient InfluxDBClient;

    /**
     * 动态Influx数据源实例，用于管理动态的数据源配置和连接
     */
    private final MagicDynamicInfluxDataSource magicDynamicInfluxDataSource;


    /**
     * 构造函数，初始化InfluxModule实例
     * @param influxDBClient InfluxDB客户端实例，用于与InfluxDB数据库进行交互
     * @param magicDynamicInfluxDataSource 动态数据源配置，用于管理数据源连接信息
     */
    public InfluxModule(com.influxdb.client.InfluxDBClient influxDBClient, MagicDynamicInfluxDataSource magicDynamicInfluxDataSource) {
        InfluxDBClient = influxDBClient;
        this.magicDynamicInfluxDataSource = magicDynamicInfluxDataSource;
    }

    /**
     * 获取InfluxDB客户端实例
     * @return 返回当前模块使用的InfluxDB客户端对象
     */
    public InfluxDBClient getInfluxDBClient() {
        return this.InfluxDBClient;
    }


    /**
     * 获取动态模块实例
     *
     * @param context 脚本上下文对象，用于获取数据源配置信息
     * @return 返回InfluxModule实例，如果未指定数据源则返回当前实例，否则返回基于指定数据源的新实例
     */
    @Override
    public InfluxModule getDynamicModule(MagicScriptContext context) {
        // 从上下文中获取数据源键值
        String dataSourceKey = context.getString(Options.DEFAULT_DATA_SOURCE.getValue());
        // 如果数据源键值为空，则返回当前模块实例
        if (StringUtils.isEmpty(dataSourceKey)) return this;
        // 根据数据源键值创建新的模块实例
        return new InfluxModule(this.magicDynamicInfluxDataSource.getInfluxDBClient(dataSourceKey), this.magicDynamicInfluxDataSource);
    }


    /**
     * 根据键值获取动态属性对应的模块实例
     *
     * @param key 数据源键值，用于标识特定的数据源配置
     * @return 返回基于指定键值对应数据源的新InfluxModule实例
     */
    @Override
    public InfluxModule getDynamicAttribute(String key) {
        // 根据键值创建新的模块实例
        return new InfluxModule(this.magicDynamicInfluxDataSource.getInfluxDBClient(key), this.magicDynamicInfluxDataSource);
    }



}
