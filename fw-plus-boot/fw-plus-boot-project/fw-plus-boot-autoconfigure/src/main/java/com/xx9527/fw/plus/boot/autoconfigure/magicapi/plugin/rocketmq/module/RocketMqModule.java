package com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rocketmq.module;

import org.apache.commons.lang.ObjectUtils;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.apache.rocketmq.tools.admin.DefaultMQAdminExt;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rocketmq.model.MagicDynamicRocketMqDataSource;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rocketmq.producer.RocketMqProducer;
import org.apache.commons.lang3.StringUtils;
import org.ssssssss.magicapi.core.annotation.MagicModule;
import org.ssssssss.magicapi.core.model.Options;
import org.ssssssss.magicapi.modules.DynamicModule;
import org.ssssssss.script.MagicScriptContext;
import org.ssssssss.script.functions.DynamicAttribute;

import java.util.Objects;

/**
 * rocketMq模块
 *
 * @author zzy
 */
@MagicModule("rocketmq")
public class RocketMqModule implements DynamicAttribute<RocketMqModule, RocketMqModule> , DynamicModule<RocketMqModule> {

    /**
     * RocketMQ生产者实例
     */
    private final RocketMqProducer producer;

    /**
     * 动态RocketMQ数据源管理器
     */
    private final MagicDynamicRocketMqDataSource magicDynamicRocketMqDataSource;

    /**
     * 数据源键构造函数，初始化RocketMQ模块
     */
    private String key ;

    /**
     * 构造函数，初始化RocketMQ模块
     *
     * @param producer RocketMQ生产者实例
     * @param magicDynamicRocketMqDataSource 动态RocketMQ数据源管理器
     */
    public RocketMqModule(RocketMqProducer producer, MagicDynamicRocketMqDataSource magicDynamicRocketMqDataSource) {
        this.producer = producer;
        this.magicDynamicRocketMqDataSource = magicDynamicRocketMqDataSource;
    }

    /**
     * 根据脚本上下文获取动态模块实例
     * 如果上下文中指定了数据源键，则返回对应数据源的模块实例；否则返回当前实例
     *
     * @param context 脚本执行上下文，包含数据源配置信息
     * @return RocketMQ模块实例
     */
    @Override
    public RocketMqModule getDynamicModule(MagicScriptContext context) {
        String dataSourceKey = context.getString(Options.DEFAULT_DATA_SOURCE.getValue());
        if (StringUtils.isEmpty(dataSourceKey)) {
            return this;
        }
        RocketMqModule module = new RocketMqModule( this.magicDynamicRocketMqDataSource.getProducer(dataSourceKey),this.magicDynamicRocketMqDataSource);
        module.key = dataSourceKey;
        return module;
    }

    /**
     * 根据指定的键获取动态属性模块实例
     *
     * @param key 数据源键，用于标识特定的RocketMQ生产者配置
     * @return RocketMQ模块实例
     */
    @Override
    public RocketMqModule getDynamicAttribute(String key) {
        if (Objects.isNull(key) || Options.DEFAULT_DATA_SOURCE.getValue().equalsIgnoreCase(key)
                || "default".equalsIgnoreCase(key)) {
            return this;
        }
        RocketMqModule module = new RocketMqModule( this.magicDynamicRocketMqDataSource.getProducer(key),this.magicDynamicRocketMqDataSource);
        module.key = key;
        return module;
    }

    /**
     * 获取RocketMQ生产者实例
     *
     * @return RocketMQ生产者实例
     */
    public RocketMqProducer getProducer() {
        return this.producer;
    }


    /**
     * 修改消费者组的消费偏移量到当前系统时间位置
     *
     * @param topic 主题名称
     * @param group 消费者组名称
     * @throws MQBrokerException 当Broker响应异常时抛出
     * @throws RemotingException 当远程通信失败时抛出
     * @throws InterruptedException 当线程被中断时抛出
     * @throws MQClientException 当RocketMQ客户端操作失败时抛出
     */
    public void alterConsumerGroupOffsets(String topic, String group) throws MQBrokerException, RemotingException, InterruptedException, MQClientException {
        this.alterConsumerGroupOffsets(topic, group, null);
    }

    /**
     * 修改消费者组的消费偏移量到指定时间戳位置
     *
     * @param topic 主题名称
     * @param group 消费者组名称
     * @param targetTimestamp 目标时间戳，如果为null则使用当前系统时间
     * @throws MQClientException 当RocketMQ客户端操作失败时抛出
     * @throws MQBrokerException 当Broker响应异常时抛出
     * @throws RemotingException 当远程通信失败时抛出
     * @throws InterruptedException 当线程被中断时抛出
     * @throws IllegalArgumentException 当topic或group为空时抛出
     * @throws IllegalStateException 当NameServer地址未配置时抛出
     */
    public void alterConsumerGroupOffsets(String topic, String group, Long  targetTimestamp) throws MQClientException, MQBrokerException, RemotingException, InterruptedException {
        if (StringUtils.isBlank(topic)) {
            throw new IllegalArgumentException("Topic cannot be blank");
        }
        if (StringUtils.isBlank(group)) {
            throw new IllegalArgumentException("Consumer group cannot be blank");
        }

        String nameServer = this.producer.getProducer().getNamesrvAddr();

        DefaultMQAdminExt admin = new DefaultMQAdminExt();
        admin.setNamesrvAddr(nameServer);

        try {
            admin.start();
            // 重置消费者组偏移量到指定时间戳
            admin.resetOffsetByTimestamp(topic, group, (Long) ObjectUtils.defaultIfNull(targetTimestamp, System.currentTimeMillis()), true);
        } finally {
            // 确保Admin客户端正确关闭
            admin.shutdown();
        }
    }

}
