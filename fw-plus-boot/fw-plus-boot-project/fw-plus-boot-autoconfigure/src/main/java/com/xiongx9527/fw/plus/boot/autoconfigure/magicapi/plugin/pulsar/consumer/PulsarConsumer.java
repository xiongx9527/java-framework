package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.pulsar.consumer;

import com.alibaba.fastjson2.JSON;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.pulsar.common.BaseService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.pulsar.model.MagicDynamicPulsarDataSource;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.pulsar.model.PulsarDataSourceInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.pulsar.model.PulsarSubscribeInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.pulsar.model.PulsarSubscribeScriptInfo;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.pulsar.client.api.*;
import org.apache.pulsar.shade.com.fasterxml.jackson.core.type.TypeReference;
import org.apache.pulsar.shade.com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 消费者
 *
 * @author zzy
 */
@Slf4j
public class PulsarConsumer implements BaseService {

    /**
     * ObjectMapper实例，用于JSON序列化和反序列化操作
     * 使用Pulsar shaded版本的Jackson库避免依赖冲突
     */
    private static final ObjectMapper shadedMapper = new org.apache.pulsar.shade.com.fasterxml.jackson.databind.ObjectMapper();

    /**
     * 配置键名常量，表示主题名称配置项
     */
    private static final String CONFIG_TOPIC_NAMES = "topicNames";

    /**
     * 配置键名常量，表示订阅名称配置项
     */
    private static final String CONFIG_SUBSCRIPTION_NAME = "subscriptionName";

    /**
     * 配置键名常量，表示订阅模式配置项
     */
    private static final String CONFIG_SUBSCRIPTION_MODE = "subscriptionMode";

    // Pulsar 数据源信息的唯一标识
    private final String id;
    // Pulsar 数据源信息的密钥
    private final String key;
    // Pulsar 数据源信息的名称
    private final String name;

    /**
     * Pulsar 数据源信息对象，包含连接和认证信息
     */
    private final PulsarDataSourceInfo dataSourceInfo;

    /**
     * 创建 Pulsar 客户端
     */
    private final MagicDynamicPulsarDataSource magicDynamicPulsarDataSource ;


    /**
     * Pulsar Consumer 实例映射表
     * 用于存储不同主题和订阅名称的 Consumer 实例
     */
    private Map<String,Map<String,Consumer<byte[]>>> pulsarConsumerMap = new ConcurrentHashMap<>();

    /**
     * 构造函数
     * 用于初始化 PulsarConsumer 对象，并根据订阅脚本信息创建 Pulsar Consumer
     *
     * @param magicDynamicPulsarDataSource Pulsar 客户端实例
     * @param dataSourceInfo Pulsar 数据源信息
     * @param subscribeScriptInfos 订阅脚本信息列表，用于创建 Consumer
     */
    public PulsarConsumer(MagicDynamicPulsarDataSource magicDynamicPulsarDataSource ,
                          PulsarDataSourceInfo dataSourceInfo,
                          List<PulsarSubscribeScriptInfo> subscribeScriptInfos)  {
        this.magicDynamicPulsarDataSource = magicDynamicPulsarDataSource;
        this.dataSourceInfo = dataSourceInfo;
        this.id = dataSourceInfo.getId();
        this.name = dataSourceInfo.getName();
        this.key = dataSourceInfo.getKey();
        createPulsarConsumer(subscribeScriptInfos);
    }

    /**
     * 创建一个Pulsar消费者
     *
     * @param subscribeScriptInfo 订阅脚本信息，包含消费所需的配置
     * @return 返回一个消费者对象，用于消费指定主题的消息
     * @throws PulsarClientException 当创建消费者过程中出现错误时抛出此异常
     */
    private Consumer<byte[]> createConsumer(PulsarSubscribeScriptInfo subscribeScriptInfo) throws PulsarClientException {
        // 构造主题名称
        String topic = createTopicName(dataSourceInfo.getTenant(), dataSourceInfo.getNamespace(), subscribeScriptInfo.getTopic());
        // 获取订阅名称
        String subscriptionName = subscribeScriptInfo.getGroup();

        // 初始化配置映射
        Map<String, Object> config = new HashMap<>();
        // 添加主题名称到配置中（使用不可变单元素集合）
        config.put(CONFIG_TOPIC_NAMES, Collections.singleton(topic));
        // 添加订阅名称到配置中
        config.put(CONFIG_SUBSCRIPTION_NAME, subscriptionName);
        if (subscribeScriptInfo.isBroadcast()) {
            // 如果是广播模式，给subscriptionName添加一个随机数以确保每个消费者都能接收到消息
            String broadcastSubscriptionName = subscriptionName + "-" + System.currentTimeMillis() + "-" + java.util.UUID.randomUUID().toString().substring(0, 8);
            config.put(CONFIG_SUBSCRIPTION_MODE, broadcastSubscriptionName);
        }
        Map<String, Object> otherMap = new HashMap<>();
        if (StringUtils.isNotBlank(subscribeScriptInfo.getOther())) {
            otherMap.putAll(JSON.parseObject(subscribeScriptInfo.getOther(), new com.alibaba.fastjson2.TypeReference<Map<String, Object>>() {}));
        }
        config.putAll(otherMap);
        boolean batchIndexAcknowledgmentEnabled = subscribeScriptInfo.isBatchIndexAcknowledgmentEnabled();
        // 创建消费者并加载配置
        return magicDynamicPulsarDataSource.getPulsarClient(key).newConsumer()
                .receiverQueueSize(1000)
                .enableRetry(true).deadLetterPolicy(DeadLetterPolicy.builder().maxRedeliverCount(3).build())
                .subscriptionType(SubscriptionType.Failover)
                //.subscriptionMode(SubscriptionMode.Durable)
                .loadConf(config)
                .enableBatchIndexAcknowledgment(batchIndexAcknowledgmentEnabled)
                .consumerName(subscriptionName) // 推荐设置消费者名称
                .messageListener((MessageListener<byte[]>) (consumer, msg) -> {
                    try {
                        boolean success = magicDynamicPulsarDataSource.getPulsarConsumerService().consume(dataSourceInfo.getKey(), msg);
                        if (success) {
                            if (batchIndexAcknowledgmentEnabled) {
                                consumer.acknowledgeCumulativeAsync(msg);
                            } else {
                                consumer.acknowledgeAsync(msg);
                            }
                        } else {
                            throw new RuntimeException("Consumption failed for message");
                        }
                    } catch (Throwable e) {
                        log.error("Error consuming message from topic: {}, subscription: {}",msg.getTopicName(),consumer.getSubscription(),e);
                        consumer.negativeAcknowledge(msg);
                    }
                }).subscribe();
    }

    /**
     * 创建Pulsar消费者
     * 此方法用于初始化或重新初始化Pulsar消费者它遍历一个Pulsar订阅脚本信息列表，
     * 对于每个订阅信息，它会检查主题名称和组是否为空以及订阅是否启用如果消费者已经存在，
     * 它会首先尝试关闭并移除现有的消费者，然后创建并注册新的消费者
     *
     * @param subscribeScriptInfos Pulsar订阅脚本信息列表，包含需要创建消费者的主题和组信息
     */
    public void createPulsarConsumer(List<PulsarSubscribeScriptInfo> subscribeScriptInfos) {
        if (CollectionUtils.isEmpty(subscribeScriptInfos)) {
            return;
        }
        Map<String, PulsarSubscribeScriptInfo> uniqueSubscribers = new LinkedHashMap<>();
        for (PulsarSubscribeScriptInfo info : subscribeScriptInfos) {
            if (Objects.isNull(info) || !info.isEnabled()) {
                continue;
            }
            String key = info.getTopic() + ":" + info.getGroup();
            uniqueSubscribers.put(key, info);
        }
        for (PulsarSubscribeScriptInfo subscribeScriptInfo : uniqueSubscribers.values()) {
            String topicName = subscribeScriptInfo.getTopic();
            String group = subscribeScriptInfo.getGroup();
            if (StringUtils.isBlank(topicName) || StringUtils.isBlank(group)) {
                continue;
            }
            // 创建新消费者
            tryCreateConsumer(subscribeScriptInfo, topicName, group);
        }
    }


    /**
     * 尝试创建Pulsar消费者并将其添加到消费者映射中
     *
     * @param subscribeScriptInfo 订阅脚本信息，用于创建消费者
     * @param topicName 主题名称，用于日志记录
     * @param group 消费者组名称，用作映射的键
     */
    private void tryCreateConsumer(PulsarSubscribeScriptInfo subscribeScriptInfo,String topicName, String group) {
        try {
            Map<String, Consumer<byte[]>> consumerTopicMap = this.pulsarConsumerMap.get(topicName);
            if (consumerTopicMap == null) {
                consumerTopicMap = new HashMap<>(4);
            }
            if (SpringUtils.isApplicationFullyStarted()) {
                // 创建新的消费者实例并添加到映射中
                Consumer<byte[]> newConsumer = createConsumer(subscribeScriptInfo);
                consumerTopicMap.put(group, newConsumer);
            } else {
                try {
                    while (!SpringUtils.isApplicationFullyStarted()) {
                        Thread.sleep(100);
                    }
                    Consumer<byte[]> newConsumer = createConsumer(subscribeScriptInfo);
                    consumerTopicMap.put(group, newConsumer);
                    log.info("Pulsar consumer created asynchronously for topic: {}, group: {}", topicName, group);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("Interrupted while waiting for application startup", e);
                } catch (Exception e) {
                    log.error("Failed to create Pulsar consumer asynchronously for topic: {}, group: {}", topicName, group, e);
                }
            }
            this.pulsarConsumerMap.put(topicName, consumerTopicMap);
        } catch (Exception e) {
            log.error("Failed to create Pulsar consumer for topic: {}, group: {}", topicName, group, e);
        }
    }


    /**
     * 关闭所有Pulsar消费者和客户端连接
     *
     * 此方法遍历pulsarConsumerMap中的所有消费者，并尝试关闭它们
     * 如果消费者处于连接状态，则将其关闭，如果关闭过程中出现异常，则记录错误日志
     * 最后，如果客户端对象不为空，也尝试关闭它
     */
    public void close() {
        // 检查pulsarConsumerMap是否为空，为空则不执行关闭操作
        if (pulsarConsumerMap != null) {
            // 遍历pulsarConsumerMap中的每个消费者组
            pulsarConsumerMap.forEach((groupKey, consumerMap) -> {
                if (consumerMap != null) {
                    // 遍历当前消费者组中的每个消费者
                    consumerMap.forEach((consumerKey, consumer) -> {
                        closeConsumer(consumer, consumerKey);
                    });
                }
            });
        }
    }

    /**
     * 关闭单个Pulsar消费者
     */
    private void closeConsumer(Consumer<?> consumer, Object consumerKey) {
        if (consumer != null) {
            try {
                // 检查消费者是否处于连接状态，是则关闭它
                if (consumer.isConnected()) {
                    consumer.close();
                }
            } catch (PulsarClientException e) {
                // 记录关闭消费者时出现的异常，并带上消费者标识
                log.error("Failed to close Pulsar consumer [{}]", consumerKey, e);
            }
        }
    }

    /**
     * 获取ID
     * @return ID字符串
     */
    public String getId() {
        return id;
    }

    /**
     * 获取名称
     * @return 名称字符串
     */
    public String getName() {
        return name;
    }

    /**
     * 获取键值
     * @return 键值字符串
     */
    public String getKey() {
        return key;
    }

    /**
     * 获取数据源信息
     * @return Pulsar数据源信息对象
     */
    public PulsarDataSourceInfo getDataSourceInfo() {
        return dataSourceInfo;
    }



    /**
     * 取消订阅Pulsar主题
     *
     * 本方法通过移除订阅信息和关闭相关的Pulsar消费者来实现取消订阅的操作
     *
     * @param subscribeScriptInfo 订阅脚本信息，包含取消订阅所需的关键字、主题和组信息
     */
    public void unsubscribe(PulsarSubscribeInfo subscribeScriptInfo) {
        String topic = subscribeScriptInfo.getTopic();
        String group = subscribeScriptInfo.getGroup();

        Map<String, Consumer<byte[]>> groupConsumerMap = pulsarConsumerMap.get(topic);
        if (groupConsumerMap != null) {
            Consumer<byte[]> consumer = groupConsumerMap.get(group);
            if (consumer != null) {
                try {
                    if (consumer.isConnected()) {
                        consumer.unsubscribe();
                        consumer.close();
                    }
                } catch (Exception e) {
                    log.error("Failed to safely close consumer", e);
                }
            }
            // 仅当 consumer 存在时才移除
            if (consumer != null) {
                groupConsumerMap.remove(group);
            }

            // 如果该主题下没有其他消费者，则从消费者映射中移除整个主题
            if (groupConsumerMap.isEmpty()) {
                pulsarConsumerMap.remove(topic);
            }
        }
    }

}
