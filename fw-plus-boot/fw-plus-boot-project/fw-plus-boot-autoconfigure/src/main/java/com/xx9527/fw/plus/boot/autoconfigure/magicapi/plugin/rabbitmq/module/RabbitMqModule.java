package com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rabbitmq.module;

import org.apache.commons.lang3.StringUtils;
import org.ssssssss.magicapi.core.annotation.MagicModule;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rabbitmq.model.MagicDynamicRabbitMqDataSource;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rabbitmq.producer.RabbitMqProducer;

import org.ssssssss.magicapi.core.model.Options;
import org.ssssssss.magicapi.modules.DynamicModule;
import org.ssssssss.script.MagicScriptContext;
import org.ssssssss.script.functions.DynamicAttribute;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.TimeoutException;

@MagicModule("rabbitmq")
public class RabbitMqModule implements DynamicAttribute<RabbitMqModule, RabbitMqModule> , DynamicModule<RabbitMqModule> {

    /**
     * RabbitMQ生产者实例
     */
    private final RabbitMqProducer producer;

    /**
     * 动态RabbitMQ数据源管理器
     */
    private final MagicDynamicRabbitMqDataSource magicDynamicRabbitMqDataSource;

    /**
     * 数据源键值
     */
    private String key ;

    /**
     * 构造函数，初始化RabbitMQ模块
     *
     * @param producer RabbitMQ生产者实例
     * @param magicDynamicRabbitMqDataSource 动态RabbitMQ数据源管理器
     */
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
        if (Objects.isNull(key) || Options.DEFAULT_DATA_SOURCE.getValue().equalsIgnoreCase(key)
                || "default".equalsIgnoreCase(key)) {
            return this;
        }
        RabbitMqModule module = new RabbitMqModule( this.magicDynamicRabbitMqDataSource.getProducer(key),this.magicDynamicRabbitMqDataSource);
        module.key = key;
        return module;
    }


    /**
     * 清空指定队列中的所有消息
     * 注意：RabbitMQ 没有消费者组偏移量的概念，此方法直接清空队列中的所有消息
     *
     * @param topic 队列名称
     * @throws IOException 当 IO 操作失败时抛出
     * @throws TimeoutException 当连接超时时抛出
     * @throws IllegalArgumentException 当队列为空时抛出
     * @throws IllegalStateException 当 RabbitMQ 连接配置缺失时抛出
     */
    public void alterConsumerGroupOffsets(String topic) throws IOException, TimeoutException {
        if (StringUtils.isBlank(topic)) {
            throw new IllegalArgumentException("Queue name cannot be blank");
        }

        // 使用 RabbitTemplate 的 execute 方法复用连接和通道，避免频繁创建连接
        try {
            this.producer.getProducer().execute(channel -> {
                // 清空队列中的所有消息
                channel.queuePurge(topic);
                return null;
            });
        } catch (Exception e) {
            // 将运行时异常转换为声明的受检异常
            if (e instanceof IOException) {
                throw (IOException) e;
            } else if (e instanceof TimeoutException) {
                throw (TimeoutException) e;
            } else {
                throw new IOException("Failed to purge queue: " + topic, e);
            }
        }
    }


}
