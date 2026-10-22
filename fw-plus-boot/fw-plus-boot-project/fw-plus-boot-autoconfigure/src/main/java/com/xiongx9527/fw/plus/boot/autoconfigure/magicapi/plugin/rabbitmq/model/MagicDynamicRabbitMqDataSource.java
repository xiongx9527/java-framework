package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rabbitmq.model;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.lock.LocalLockModule;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.lock.Lock;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rabbitmq.consumer.RabbitMQConsumer;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rabbitmq.consumer.RabbitMqConsumerService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rabbitmq.producer.RabbitMqProducer;
import com.xiongx9527.fw.plus.common.i18n.I18nMessageResolver;
import lombok.extern.slf4j.Slf4j;
import org.ssssssss.magicapi.utils.Assert;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;

@Slf4j
public class MagicDynamicRabbitMqDataSource {

    /**
     * 生产者映射表，用于存储不同配置对应的RabbitMQ生产者实例
     * 使用ConcurrentHashMap保证线程安全
     */
    private final Map<String, RabbitMqProducer> producerMap = new ConcurrentHashMap<>();

    /**
     * 消费者映射表，用于存储不同配置对应的RabbitMQ消费者实例
     * 使用ConcurrentHashMap保证线程安全
     */
    private final Map<String, RabbitMQConsumer> consumerMap = new ConcurrentHashMap();

    /**
     * RabbitMQ消费者服务实例，用于管理消费者的生命周期
     */
    private final RabbitMqConsumerService rabbitMqConsumerService;

    /**
     * 用于加锁，防止消费者重复创建
     */
    private final Lock lock  = new LocalLockModule();

    /**
     * 构造函数，初始化MagicDynamicRabbitMqDataSource实例
     *
     * @param rabbitMqConsumerService RabbitMQ消费者服务实例，用于处理消费者的创建和管理
     */
    public MagicDynamicRabbitMqDataSource(RabbitMqConsumerService rabbitMqConsumerService) {
        this.rabbitMqConsumerService = rabbitMqConsumerService;
    }

    /**
     * 注册RabbitMQ生产者
     * 将指定的数据源信息创建为RabbitMQ生产者并存储到生产者映射中
     *
     * @param info RabbitMQ数据源信息，包含连接配置等参数
     * @return 注册成功返回true，失败返回false
     */
    public boolean putProducer(RabbitMqDataSourceInfo info) {
        try {
            this.producerMap.put(info.getKey(), new RabbitMqProducer(info));
            log.info("注册 RabbitMq 消息队列:[{}]完成执行", info.getKey());
            return true;
        } catch (Exception e) {
            log.error("RabbitMq 消息队列:[{}]注册失败", info.getKey());
            return false;
        }
    }

    /**
     * 注册RabbitMQ消费者
     * 根据数据源信息和订阅脚本列表创建RabbitMQ消费者并存储到消费者映射中
     *
     * @param info RabbitMQ数据源信息，包含连接配置等参数
     * @param subscribeScripteInfoList 订阅脚本信息列表，定义消费者需要处理的消息类型
     * @return 注册成功返回true，失败返回false
     */
    public boolean putConsumer(RabbitMqDataSourceInfo info, List<RabbitMqSubscribeScriptInfo> subscribeScripteInfoList) {
        Object lockInfo = null;
        try {
            lockInfo = lock.lock(info.getKey(), 5000, 5000);
            RabbitMQConsumer rabbitMQConsumer = this.consumerMap.get(info.getKey());
            if (rabbitMQConsumer != null) {
                rabbitMQConsumer.createConsumer(subscribeScripteInfoList);
            } else {
                rabbitMQConsumer = new RabbitMQConsumer(this.rabbitMqConsumerService, info, subscribeScripteInfoList);
                this.consumerMap.put(info.getKey(), rabbitMQConsumer);
            }
            log.info("注册 RabbitMq 消息队列:[{}]完成执行", info.getKey());
        } catch (Exception e) {
            log.error("RabbitMq 消息队列:[{}]注册失败", info.getKey());
            return false;
        } finally {
            if (lockInfo != null) {
                lock.release(lockInfo);
            }
        }
        return true;
    }

    /**
     * 删除指定的RabbitMQ生产者
     * 从生产者映射中移除指定数据源键对应的生产者，并关闭其连接资源
     *
     * @param datasourceKey 数据源键，用于标识要删除的生产者
     */
    public void deleteProducer(String datasourceKey) {
        // 验证数据源键的有效性
        if (datasourceKey != null && !datasourceKey.isEmpty()) {
            // 从映射中移除并获取生产者实例
            RabbitMqProducer producer = this.producerMap.remove(datasourceKey);
            // 关闭生产者的资源连接
            if (producer != null) {
                producer.close();
            }
        }
    }

    /**
     * 获取所有生产者节点的集合
     *
     * @return 包含所有生产者节点的集合
     */
    public Collection<RabbitMqProducer> producerNodes() {
        return this.producerMap.values();
    }

    /**
     * 根据数据源键获取指定的生产者
     *
     * @param datasourceKey 数据源键
     * @return 对应的RabbitMqProducer实例
     * @throws IllegalArgumentException 当找不到对应的数据源时抛出异常
     */
    public RabbitMqProducer getProducer(String datasourceKey) {
        RabbitMqProducer dataSourceNode = this.producerMap.get(datasourceKey);
        Assert.isNotNull(dataSourceNode, I18nMessageResolver.resolveKey("fw.boot.magicapi.rabbitmq.producerDatasourceNotFound", datasourceKey));
        return dataSourceNode;
    }

    /**
     * 根据主机地址、端口和虚拟主机查找生产者
     *
     * @param host 主机地址
     * @param port 端口号
     * @param virtualHost 虚拟主机名
     * @return 匹配的RabbitMqProducer实例，如果未找到则返回null
     */
    public RabbitMqProducer getProducer(String host, int port, String virtualHost) {
        Optional<RabbitMqProducer> producerOptional = this.producerMap.values().stream().filter((x) -> {
            return StringUtils.equals(x.getProducer().getConnectionFactory().getHost(), host) && Objects.equals(x.getProducer().getConnectionFactory().getPort(), port) && StringUtils.equals(x.getProducer().getConnectionFactory().getVirtualHost(), virtualHost);
        }).findFirst();
        return producerOptional.isPresent() ? producerOptional.get() : null;
    }

    /**
     * 获取所有消费者的数据源键列表
     *
     * @return 包含所有消费者数据源键的列表
     */
    public List<String> consumers() {
        return new ArrayList(this.producerMap.keySet());
    }

    /**
     * 判断消费者映射是否为空
     *
     * @return 如果消费者映射为空则返回true，否则返回false
     */
    public boolean isConsumerEmpty() {
        return this.consumerMap.isEmpty();
    }

    /**
     * 获取所有消费者节点的集合
     *
     * @return 包含所有消费者节点的集合
     */
    public Collection<RabbitMQConsumer> consumerNodes() {
        return this.consumerMap.values();
    }

    /**
     * 根据数据源键获取指定的消费者
     *
     * @param datasourceKey 数据源键
     * @return 对应的RabbitMQConsumer实例，如果未找到则返回null
     */
    public RabbitMQConsumer getConsumer(String datasourceKey) {
        RabbitMQConsumer dataSourceNode = this.consumerMap.get(datasourceKey);
        return dataSourceNode;
    }


}
