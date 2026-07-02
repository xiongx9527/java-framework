package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rabbitmq.module;

import org.apache.commons.lang3.StringUtils;
import org.ssssssss.magicapi.core.annotation.MagicModule;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rabbitmq.model.MagicDynamicRabbitMqDataSource;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rabbitmq.producer.RabbitMqProducer;

import org.ssssssss.magicapi.core.model.Options;
import org.ssssssss.magicapi.modules.DynamicModule;
import org.ssssssss.script.MagicScriptContext;
import org.ssssssss.script.functions.DynamicAttribute;

@MagicModule("rabbitmq")
public class RabbitMqModule implements DynamicAttribute<RabbitMqModule, RabbitMqModule> , DynamicModule<RabbitMqModule> {

    private final RabbitMqProducer producer;

    private final MagicDynamicRabbitMqDataSource magicDynamicRabbitMqDataSource;

    public RabbitMqModule(RabbitMqProducer producer, MagicDynamicRabbitMqDataSource magicDynamicRabbitMqDataSource) {
        this.producer = producer;
        this.magicDynamicRabbitMqDataSource = magicDynamicRabbitMqDataSource;
    }


    /**
     * 获取RabbitMQ生产者实例
     *
     * @return RabbitMqProducer RabbitMQ生产者对象
     */
    public RabbitMqProducer getProducer() {
        return this.producer;
    }

    /**
     * 根据上下文获取动态RabbitMQ模块
     *
     * @param context MagicScript上下文对象，用于获取数据源配置
     * @return RabbitMqModule 返回当前模块或基于指定数据源新建的模块
     */
    @Override
    public RabbitMqModule getDynamicModule(MagicScriptContext context) {
        // 从上下文中获取数据源键值
        String dataSourceKey = context.getString(Options.DEFAULT_DATA_SOURCE.getValue());
        // 如果数据源键值为空，则返回当前模块实例
        if (StringUtils.isEmpty(dataSourceKey)) {
            return this;
        }
        // 根据数据源键值创建新的RabbitMQ模块实例
        return new RabbitMqModule( this.magicDynamicRabbitMqDataSource.getProducer(dataSourceKey),this.magicDynamicRabbitMqDataSource);

    }

    /**
     * 根据键值获取动态RabbitMQ模块属性
     *
     * @param key 数据源键值
     * @return RabbitMqModule 基于指定键值创建的RabbitMQ模块实例
     */
    @Override
    public RabbitMqModule getDynamicAttribute(String key) {
        return new RabbitMqModule( this.magicDynamicRabbitMqDataSource.getProducer(key),this.magicDynamicRabbitMqDataSource);
    }


}
