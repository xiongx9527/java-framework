package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.pulsar.module;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.pulsar.model.MagicDynamicPulsarDataSource;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.pulsar.producer.PulsarProducer;
import org.apache.commons.lang3.StringUtils;
import org.apache.pulsar.client.api.Producer;
import org.ssssssss.magicapi.core.annotation.MagicModule;
import org.ssssssss.magicapi.core.model.Options;
import org.ssssssss.magicapi.modules.DynamicModule;
import org.ssssssss.script.MagicScriptContext;
import org.ssssssss.script.functions.DynamicAttribute;

import java.beans.Transient;
import java.util.concurrent.TimeUnit;


/**
 * Pulsar模块
 */
@MagicModule("pulsar")
public class PulsarModule implements DynamicAttribute<PulsarModule, PulsarModule> , DynamicModule<PulsarModule> {

    /** Pulsar动态数据源，用于管理Pulsar连接 */
    private final MagicDynamicPulsarDataSource magicDynamicPulsarDataSource;

    /** Pulsar生产者实例 */
    private PulsarProducer producer;

    /**
     * 构造函数，初始化PulsarModule实例（无默认生产者）
     *
     * @param magicDynamicPulsarDataSource Pulsar动态数据源，用于管理Pulsar连接
     */
    public PulsarModule(MagicDynamicPulsarDataSource magicDynamicPulsarDataSource) {
        this.magicDynamicPulsarDataSource = magicDynamicPulsarDataSource;
    }
    
    /**
     * 构造函数，初始化PulsarModule实例（包含默认生产者）
     *
     * @param magicDynamicPulsarDataSource Pulsar动态数据源，用于管理Pulsar连接
     * @param producer                     Pulsar生产者实例，用于创建默认生产者
     */
    public PulsarModule(MagicDynamicPulsarDataSource magicDynamicPulsarDataSource, Producer<byte[]> producer) {
        this.magicDynamicPulsarDataSource = magicDynamicPulsarDataSource;
        if (producer != null) {
            this.producer = new PulsarProducer(magicDynamicPulsarDataSource, producer);
        }
    }

    /**
     * PulsarModule构造函数
     *
     * @param magicDynamicPulsarDataSource 动态Pulsar数据源实例
     * @param producer Pulsar生产者实例
     */
    private PulsarModule(MagicDynamicPulsarDataSource magicDynamicPulsarDataSource , PulsarProducer producer) {
        this.magicDynamicPulsarDataSource = magicDynamicPulsarDataSource;
        this.producer = producer;
    }


    /**
     * 根据键名获取动态Pulsar生产者属性
     *
     * @param key Pulsar数据源键名
     * @return PulsarProducer 对应键名的Pulsar生产者实例
     */
    @Override
    @Transient
    public PulsarModule getDynamicAttribute(String key) {
        return new PulsarModule(this.magicDynamicPulsarDataSource, this.magicDynamicPulsarDataSource.getProducer(key));
    }

    /**
     * 获取动态Pulsar生产者模块
     * 根据上下文中的数据源键获取对应的Pulsar生产者，如果未指定数据源键则返回默认生产者
     *
     * @param context 脚本执行上下文，包含数据源配置信息
     * @return PulsarProducer Pulsar生产者实例
     */
    @Override
    public PulsarModule getDynamicModule(MagicScriptContext context) {
        String dataSourceKey = context.getString(Options.DEFAULT_DATA_SOURCE.getValue());
        if (StringUtils.isEmpty(dataSourceKey)) {
            return this;
        }
        return new PulsarModule(this.magicDynamicPulsarDataSource,this.magicDynamicPulsarDataSource.getProducer(dataSourceKey));
    }

    /**
     * 发送消息到指定的主题
     *
     * @param topic 消息要发送到的主题名称
     * @param partition 指定分区编号，如果为null则使用默认分区策略
     * @param key 消息键，用于分区路由和消息标识
     * @param delay 延迟时间，如果为null则不延迟发送
     * @param unit 延迟时间的时间单位，与delay参数配合使用
     * @param msg 要发送的消息对象
     * @param async 是否异步发送，true表示异步发送，false表示同步发送
     * @return 消息发送是否成功，同步发送时返回实际发送结果，异步发送时通常返回true
     */
    public boolean send(String topic, Integer partition, String key, Long delay, TimeUnit unit, Object msg , boolean async) {
        return this.producer.send(topic, partition, key, delay, unit, msg, async);
    }

    /**
     * 异步发送消息到指定主题，并在指定延迟时间后投递。
     *
     * @param topic 消息要发送的主题名称
     * @param delay 延迟的时间长度
     * @param unit  延迟时间的单位
     * @param msg   要发送的消息对象
     * @return 发送是否成功
     */
    public boolean sendAsyncDeliverAfter(String topic,Long delay, TimeUnit unit,Object msg) {
        return this.producer.send(topic, null, null, delay, unit, msg, true);
    }

    /**
     * 异步发送消息到指定主题。
     *
     * @param topic 消息要发送的主题名称
     * @param msg   要发送的消息对象
     * @return 发送是否成功
     */
    public boolean sendAsync(String topic,Object msg) {
        return this.producer.send(topic, null, null, null, null, msg, true);
    }

    /**
     * 发送消息到指定主题，并在指定延迟时间后投递。
     *
     * @param topic 消息要发送的主题名称
     * @param delay 延迟的时间长度
     * @param unit  延迟时间的单位
     * @param msg   要发送的消息对象
     * @return 发送是否成功
     */
    public boolean sendDeliverAfter(String topic,Long delay, TimeUnit unit,Object msg) {
        return this.producer.send(topic, null, null, delay, unit, msg, false);
    }

    /**
     * 发送消息到指定主题。
     *
     * @param topic 消息要发送的主题名称
     * @param msg   要发送的消息对象
     * @return 发送是否成功
     */
    public boolean send(String topic,Object msg) {
        return this.producer.send(topic, null, null, null, null, msg, false);
    }

    /**
     * 异步发送带键的消息到指定主题，并在指定延迟时间后投递。
     *
     * @param topic 消息要发送的主题名称
     * @param key   消息的键，用于分区或标识
     * @param delay 延迟的时间长度
     * @param unit  延迟时间的单位
     * @param msg   要发送的消息对象
     * @return 发送是否成功
     */
    public boolean sendAsyncDeliverAfter(String topic,String key,Long delay, TimeUnit unit,Object msg) {
        return this.producer.send(topic, null, key, delay, unit, msg, true);
    }

    /**
     * 异步发送带键的消息到指定主题。
     *
     * @param topic 消息要发送的主题名称
     * @param key   消息的键，用于分区或标识
     * @param msg   要发送的消息对象
     * @return 发送是否成功
     */
    public boolean sendAsync(String topic,String key,Object msg) {
        return this.producer.send(topic, null, key, null, null, msg, true);
    }

    /**
     * 发送带键的消息到指定主题，并在指定延迟时间后投递。
     *
     * @param topic 消息要发送的主题名称
     * @param key   消息的键，用于分区或标识
     * @param delay 延迟的时间长度
     * @param unit  延迟时间的单位
     * @param msg   要发送的消息对象
     * @return 发送是否成功
     */
    public boolean sendDeliverAfter(String topic,String key,Long delay, TimeUnit unit,Object msg) {
        return this.producer.send(topic, null, key, delay, unit, msg, false);
    }

    /**
     * 发送带键的消息到指定主题。
     *
     * @param topic 消息要发送的主题名称
     * @param key   消息的键，用于分区或标识
     * @param msg   要发送的消息对象
     * @return 发送是否成功
     */
    public boolean send(String topic,String key,Object msg) {
        return this.producer.send(topic, null, key, null, null, msg, false);
    }

    /**
     * 异步发送带键和分区的消息到指定主题，并在指定延迟时间后投递。
     *
     * @param topic     消息要发送的主题名称
     * @param partition 分区编号（可为空）
     * @param key       消息的键，用于分区或标识
     * @param delay     延迟的时间长度
     * @param unit      延迟时间的单位
     * @param msg       要发送的消息对象
     * @return 发送是否成功
     */
    public boolean sendAsyncDeliverAfter(String topic,Integer partition,String key,Long delay, TimeUnit unit,Object msg) {
        return this.producer.send(topic, partition, key, delay, unit, msg, true);
    }

    /**
     * 异步发送带键和分区的消息到指定主题。
     *
     * @param topic     消息要发送的主题名称
     * @param partition 分区编号（可为空）
     * @param key       消息的键，用于分区或标识
     * @param msg       要发送的消息对象
     * @return 发送是否成功
     */
    public boolean sendAsync(String topic,Integer partition,String key,Object msg) {
        return this.producer.send(topic, partition, key, null, null, msg, true);
    }

    /**
     * 发送带键和分区的消息到指定主题，并在指定延迟时间后投递。
     *
     * @param topic     消息要发送的主题名称
     * @param partition 分区编号（可为空）
     * @param key       消息的键，用于分区或标识
     * @param delay     延迟的时间长度
     * @param unit      延迟时间的单位
     * @param msg       要发送的消息对象
     * @return 发送是否成功
     */
    public boolean sendDeliverAfter(String topic,Integer partition,String key,Long delay, TimeUnit unit,Object msg) {
        return this.producer.send(topic, partition, key, delay, unit, msg, false);
    }

    /**
     * 发送带键和分区的消息到指定主题。
     *
     * @param topic     消息要发送的主题名称
     * @param partition 分区编号（可为空）
     * @param key       消息的键，用于分区或标识
     * @param msg       要发送的消息对象
     * @return 发送是否成功
     */
    public boolean send(String topic,Integer partition,String key,Object msg) {
        return this.producer.send(topic, partition, key, null, null, msg, false);
    }

}
