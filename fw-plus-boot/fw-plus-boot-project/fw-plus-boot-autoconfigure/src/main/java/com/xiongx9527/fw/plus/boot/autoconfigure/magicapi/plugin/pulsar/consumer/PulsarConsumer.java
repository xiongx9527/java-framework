package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.pulsar.consumer;

import com.alibaba.fastjson2.JSON;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.common.MagicAPIConfigUtils;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.pulsar.common.BaseService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.pulsar.model.MagicDynamicPulsarDataSource;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.pulsar.model.PulsarDataSourceInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.pulsar.model.PulsarSubscribeInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.pulsar.model.PulsarSubscribeScriptInfo;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.pulsar.client.api.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 消费者
 *
 * @author zzy
 */
@Slf4j
public class PulsarConsumer implements BaseService {

    private static final String CONFIG_TOPIC_NAMES = "topicNames";

    private static final String CONFIG_SUBSCRIPTION_NAME = "subscriptionName";

    /** 应用启动等待超时时间（秒） */
    private static final long STARTUP_WAIT_TIMEOUT_SECONDS = 60;

    /** 默认最大重试投递次数 */
    private static final int DEFAULT_MAX_REDELIVER_COUNT = 3;

    /** 默认消费者接收队列大小 */
    private static final int DEFAULT_RECEIVER_QUEUE_SIZE = 1000;

    /** 默认是否启用重试 */
    private static final boolean DEFAULT_ENABLE_RETRY = true;

    private final String id;

    private final String key;

    private final String name;

    private final PulsarDataSourceInfo dataSourceInfo;

    private final MagicDynamicPulsarDataSource magicDynamicPulsarDataSource;

    /** Pulsar Consumer 实例映射表，外层key为topic名，内层key为订阅组名 */
    private final Map<String, Map<String, Consumer<byte[]>>> pulsarConsumerMap = new ConcurrentHashMap<>();

    /** 正在消费中的消息数，key 为 topic:group 格式 */
    private final Map<String, AtomicInteger> consumingCountMap = new ConcurrentHashMap<>();

    /** 消费者操作锁，保护 unsubscribe / createPulsarConsumer / close 的并发安全 */
    private final Object consumerLock = new Object();

    /** 正在关闭中标记 */
    private volatile boolean closing = false;

    /** 关闭标记 */
    private volatile boolean closed = false;

    public PulsarConsumer(MagicDynamicPulsarDataSource magicDynamicPulsarDataSource,
                          PulsarDataSourceInfo dataSourceInfo,
                          List<PulsarSubscribeScriptInfo> subscribeScriptInfos) {
        this.magicDynamicPulsarDataSource = magicDynamicPulsarDataSource;
        this.dataSourceInfo = dataSourceInfo;
        this.id = dataSourceInfo.getId();
        this.name = dataSourceInfo.getName();
        this.key = dataSourceInfo.getKey();
        createPulsarConsumer(subscribeScriptInfos);
    }

    /**
     * 尝试创建Pulsar消费者
     * <p>
     * 该方法会为指定的主题和订阅组创建消费者。如果已存在旧消费者，会先关闭以防止连接泄漏。
     * 根据应用启动状态选择不同的创建策略：应用完全启动时直接创建，否则等待启动完成后再创建。
     * </p>
     *
     * @param subscribeScriptInfo 订阅脚本信息，包含消费者配置参数
     * @param topicName 主题名称
     * @param group 订阅组名称
     */
    private void tryCreateConsumer(PulsarSubscribeScriptInfo subscribeScriptInfo, String topicName, String group) {
        String consumeKey = topicName + ":" + group;
        try {
            Map<String, Consumer<byte[]>> consumerTopicMap =
                    this.pulsarConsumerMap.computeIfAbsent(topicName, k -> new ConcurrentHashMap<>());

            // 先创建新消费者，成功后再关闭旧的，防止新消费者创建失败导致订阅丢失
            Consumer<byte[]> newConsumer;
            if (SpringUtils.isApplicationFullyStarted()) {
                newConsumer = createConsumer(subscribeScriptInfo, consumeKey);
            } else {
                newConsumer = waitForStartupAndCreate(subscribeScriptInfo, topicName, group, consumeKey);
            }
            if (newConsumer == null) {
                log.warn("创建消费者失败，保持现有消费者不变，topic: {}, group: {}", topicName, group);
                return;
            }

            Consumer<byte[]> oldConsumer = consumerTopicMap.put(group, newConsumer);
            if (oldConsumer != null) {
                closeConsumer(oldConsumer, topicName + ":" + group);
            }
        } catch (Exception e) {
            log.error("Failed to create Pulsar consumer for topic: {}, group: {}", topicName, group, e);
        }
    }

    /**
     * 等待应用启动完成后创建消费者，带超时保护。
     */
    private Consumer<byte[]> waitForStartupAndCreate(PulsarSubscribeScriptInfo info, String topicName, String group, String consumeKey) {
        long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(STARTUP_WAIT_TIMEOUT_SECONDS);
        try {
            while (!SpringUtils.isApplicationFullyStarted()) {
                if (System.currentTimeMillis() > deadline) {
                    log.error("Timed out waiting for application startup, topic: {}, group: {}", topicName, group);
                    return null;
                }
                Thread.sleep(100);
            }
            Consumer<byte[]> newConsumer = createConsumer(info, consumeKey);
            log.info("Pulsar consumer created asynchronously for topic: {}, group: {}", topicName, group);
            return newConsumer;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while waiting for application startup", e);
            return null;
        } catch (Exception e) {
            log.error("Failed to create Pulsar consumer asynchronously for topic: {}, group: {}", topicName, group, e);
            return null;
        }
    }

    /**
     * 创建Pulsar消费者实例
     * <p>
     * 该方法根据订阅脚本信息和数据源配置创建并配置Pulsar消费者，包括：
     * 1. 构建主题名称和订阅名称（支持广播模式）
     * 2. 解析并合并额外配置参数
     * 3. 配置死信策略、重试机制、批量确认等高级特性
     * 4. 注册消息监听器处理消息消费和确认
     * </p>
     *
     * @param subscribeScriptInfo 订阅脚本信息，包含主题、订阅组、重试配置等参数
     * @return Consumer&lt;byte[]&gt; 创建完成的Pulsar消费者实例
     * @throws PulsarClientException 当Pulsar客户端不存在或创建失败时抛出
     */
    private Consumer<byte[]> createConsumer(PulsarSubscribeScriptInfo subscribeScriptInfo, String consumeKey) throws PulsarClientException {
        String tenant = MagicAPIConfigUtils.getPulsarConfig(this.dataSourceInfo.getKey(), "tenant", this.dataSourceInfo.getTenant());
        String namespace = MagicAPIConfigUtils.getPulsarConfig(this.dataSourceInfo.getKey(), "namespace", this.dataSourceInfo.getNamespace());
        String topic = createTopicName(tenant, namespace, subscribeScriptInfo.getTopic());

        String subscriptionName = MagicAPIConfigUtils.getPulsarSubscribeConfig(
                this.dataSourceInfo.getKey(), subscribeScriptInfo.getTopic(), "group", subscribeScriptInfo.getGroup());

        Map<String, Object> config = new HashMap<>();
        config.put(CONFIG_TOPIC_NAMES, Collections.singleton(topic));
        config.put(CONFIG_SUBSCRIPTION_NAME, subscriptionName);

        if (subscribeScriptInfo.isBroadcast()) {
            String broadcastSubscriptionName = subscriptionName + "-broadcast-" + getUniqueId();
            config.put(CONFIG_SUBSCRIPTION_NAME, broadcastSubscriptionName);
        }

        String other = MagicAPIConfigUtils.getPulsarSubscribeConfig(
                    this.dataSourceInfo.getKey(), subscribeScriptInfo.getTopic(), "other", subscribeScriptInfo.getOther());
        if (StringUtils.isNotBlank(other)) {
            config.putAll(JSON.parseObject(other, new com.alibaba.fastjson2.TypeReference<Map<String, Object>>() {}));
        }

        // 从配置中提取消费者参数，支持通过 other JSON 覆盖默认值
        int maxRedeliverCount = getConfigValueAsInt(config, "maxRedeliverCount", DEFAULT_MAX_REDELIVER_COUNT);
        int receiverQueueSize = getConfigValueAsInt(config, "receiverQueueSize", DEFAULT_RECEIVER_QUEUE_SIZE);
        boolean enableRetry = getConfigValueAsBoolean(config, "enableRetry", DEFAULT_ENABLE_RETRY);
        String deadLetterTopic = getConfigValueAsString(config, "deadLetterTopic", null);

        DeadLetterPolicy.DeadLetterPolicyBuilder deadLetterBuilder = DeadLetterPolicy.builder()
                .maxRedeliverCount(maxRedeliverCount);
        if (StringUtils.isNotBlank(deadLetterTopic)) {
            deadLetterBuilder.deadLetterTopic(deadLetterTopic);
        }

        boolean batchIndexAcknowledgmentEnabled = subscribeScriptInfo.isBatchIndexAcknowledgmentEnabled();
        String batchAckStr = MagicAPIConfigUtils.getPulsarSubscribeConfig(
                this.dataSourceInfo.getKey(), subscribeScriptInfo.getTopic(),
                "batchIndexAcknowledgmentEnabled", String.valueOf(batchIndexAcknowledgmentEnabled));
        try {
            batchIndexAcknowledgmentEnabled = Boolean.parseBoolean(batchAckStr);
        } catch (Exception e) {
            log.error("Error parsing batchIndexAcknowledgmentEnabled property", e);
        }

        PulsarClient pulsarClient = this.magicDynamicPulsarDataSource.getPulsarClient(key);
        if (pulsarClient == null) {
            throw new PulsarClientException("PulsarClient not found for key: " + key);
        }

        final boolean finalBatchAck = batchIndexAcknowledgmentEnabled;
        return pulsarClient.newConsumer()
                .receiverQueueSize(receiverQueueSize)
                .enableRetry(enableRetry)
                .deadLetterPolicy(deadLetterBuilder.build())
                .subscriptionType(SubscriptionType.Failover)
                .loadConf(config)
                .enableBatchIndexAcknowledgment(finalBatchAck)
                .consumerName(subscriptionName)
                .messageListener((MessageListener<byte[]>) (consumer, msg) -> {
                    incrementConsuming(consumeKey);
                    try {
                        if (this.closed || this.closing) {
                            consumer.negativeAcknowledge(msg);
                            return;
                        }
                        boolean success = this.magicDynamicPulsarDataSource
                                .getPulsarConsumerService().consume(dataSourceInfo.getKey(), msg);
                        if (success) {
                            if (finalBatchAck) {
                                consumer.acknowledgeCumulativeAsync(msg);
                            } else {
                                consumer.acknowledgeAsync(msg);
                            }
                        } else {
                            consumer.negativeAcknowledge(msg);
                        }
                    } catch (Throwable e) {
                        log.warn("Error consuming message from topic: {}, subscription: {}",
                                msg.getTopicName(), consumer.getSubscription(), e);
                        consumer.negativeAcknowledge(msg);
                    } finally {
                        decrementConsuming(consumeKey);
                    }
                }).subscribe();
    }

    /**
     * 创建/刷新Pulsar消费者。
     * 会关闭不在新订阅列表中的旧消费者，防止连接泄漏。
     */
    public void createPulsarConsumer(List<PulsarSubscribeScriptInfo> subscribeScriptInfos) {
        if (CollectionUtils.isEmpty(subscribeScriptInfos)) {
            return;
        }

        // 构建目标订阅集合
        Map<String, PulsarSubscribeScriptInfo> targetSubscribers = new LinkedHashMap<>();
        for (PulsarSubscribeScriptInfo info : subscribeScriptInfos) {
            if (!Boolean.TRUE.equals(enable(info))) {
                continue;
            }
            String subscriptionName = MagicAPIConfigUtils.getPulsarSubscribeConfig(
                    this.dataSourceInfo.getKey(), info.getTopic(), "group", info.getGroup());
            String topicName = info.getTopic();
            if (StringUtils.isBlank(topicName) || StringUtils.isBlank(subscriptionName)) {
                continue;
            }
            targetSubscribers.putIfAbsent(topicName + ":" + subscriptionName, info);
        }

        synchronized (this.consumerLock) {
            if (this.closing || this.closed) {
                log.warn("Pulsar 消费者正在关闭，跳过创建新消费者，key: {}", key);
                return;
            }
            // 关闭不再需要的旧消费者
            for (Map.Entry<String, Map<String, Consumer<byte[]>>> topicEntry : this.pulsarConsumerMap.entrySet()) {
                String existingTopic = topicEntry.getKey();
                Map<String, Consumer<byte[]>> existingGroups = topicEntry.getValue();
                if (existingGroups == null) {
                    continue;
                }
                Iterator<Map.Entry<String, Consumer<byte[]>>> groupIter = existingGroups.entrySet().iterator();
                while (groupIter.hasNext()) {
                    Map.Entry<String, Consumer<byte[]>> groupEntry = groupIter.next();
                    String existingGroup = groupEntry.getKey();
                    String compositeKey = existingTopic + ":" + existingGroup;
                    if (!targetSubscribers.containsKey(compositeKey)) {
                        Consumer<byte[]> oldConsumer = groupEntry.getValue();
                        closeConsumer(oldConsumer, compositeKey);
                        this.consumingCountMap.remove(compositeKey);
                        groupIter.remove();
                    }
                }
                if (existingGroups.isEmpty()) {
                    this.pulsarConsumerMap.remove(existingTopic);
                }
            }

            // 创建新的消费者
            for (PulsarSubscribeScriptInfo info : targetSubscribers.values()) {
                String topicName = info.getTopic();
                String group = MagicAPIConfigUtils.getPulsarSubscribeConfig(
                        this.dataSourceInfo.getKey(), info.getTopic(), "group", info.getGroup());
                tryCreateConsumer(info, topicName, group);
            }
        }
    }

    /**
     * 判断当前是否有消息正在消费中
     */
    public boolean isConsuming() {
        for (AtomicInteger count : this.consumingCountMap.values()) {
            if (count.get() > 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断指定 consumer 是否有消息正在消费中
     *
     * @param consumeKey topic:group
     */
    public boolean isConsuming(String consumeKey) {
        AtomicInteger count = this.consumingCountMap.get(consumeKey);
        return count != null && count.get() > 0;
    }

    /**
     * 增加指定消费键的正在处理消息计数
     * <p>
     * 该方法用于在开始处理消息时，原子性地增加对应消费键的并发处理计数器。
     * 如果计数器不存在，则会自动创建一个新的计数器。
     * </p>
     *
     * @param consumeKey 消费键标识，用于定位或创建需要增加计数的计数器
     */
    private void incrementConsuming(String consumeKey) {
        this.consumingCountMap.computeIfAbsent(consumeKey, k -> new AtomicInteger()).incrementAndGet();
    }

    /**
     * 减少指定消费键的正在处理消息计数
     * <p>
     * 该方法用于在消息消费完成后，原子性地减少对应消费键的并发处理计数器。
     * 如果计数器不存在，则不执行任何操作。
     * </p>
     *
     * @param consumeKey 消费键标识，用于定位需要减少计数的计数器
     */
    private void decrementConsuming(String consumeKey) {
        AtomicInteger count = this.consumingCountMap.get(consumeKey);
        if (count != null) {
            count.decrementAndGet();
        }
    }

    /**
     * 判断当前消费者是否正在关闭
     * @return
     */
    public boolean isClosing() {
        return this.closing;
    }

    /**
     * 判断当前消费者是否已经关闭
     * @return
     */
    public boolean isClosed() {
        return this.closed;
    }

    /**
     * 等待正在消费中的消息处理完成，最多等待30秒
     */
    private void waitForConsumingComplete() {
        long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(30);
        while (isConsuming()) {
            if (System.currentTimeMillis() > deadline) {
                log.warn("等待消费者处理完成超时，强制关闭，key: {}", key);
                break;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    /**
     * 等待指定 consumer 的消息处理完成，最多等待30秒
     *
     * @param consumeKey topic:group
     */
    private void waitForConsumingComplete(String consumeKey) {
        long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(30);
        while (isConsuming(consumeKey)) {
            if (System.currentTimeMillis() > deadline) {
                log.warn("等待消费者处理完成超时，强制取消订阅，key: {}, consumeKey: {}", key, consumeKey);
                break;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    /**
     * 关闭所有Pulsar消费者并清理资源
     * <p>
     * 该方法会遍历所有主题的消费者，先收集到列表中再统一关闭，
     * 避免在迭代过程中修改集合导致并发异常。最后清空消费者映射表。
     * </p>
     */
    public void close() {
        if (this.closed || this.closing) {
            return;
        }
        this.closing = true;

        // 等待正在消费中的消息处理完成，再关闭连接
        waitForConsumingComplete();
        synchronized (this.consumerLock) {
            if (this.pulsarConsumerMap.isEmpty()) {
                this.closed = true;
                this.closing = false;
                return;
            }

            List<Map.Entry<String, Consumer<byte[]>>> allConsumers = new ArrayList<>();
            for (Map.Entry<String, Map<String, Consumer<byte[]>>> topicEntry : this.pulsarConsumerMap.entrySet()) {
                Map<String, Consumer<byte[]>> consumerMap = topicEntry.getValue();
                if (consumerMap != null) {
                    allConsumers.addAll(consumerMap.entrySet());
                }
            }

            for (Map.Entry<String, Consumer<byte[]>> entry : allConsumers) {
                closeConsumer(entry.getValue(), entry.getKey());
            }
            this.pulsarConsumerMap.clear();
            this.consumingCountMap.clear();
            this.closed = true;
            this.closing = false;
        }
    }

    /**
     * 关闭Pulsar消费者连接并释放资源。
     * 关闭失败时会记录错误日志，但不会抛出异常。
     *
     * @param consumer 待关闭的Pulsar消费者实例
     * @param consumerKey 消费者的标识键，用于日志记录
     */
    private void closeConsumer(Consumer<?> consumer, Object consumerKey) {
        if (consumer == null) {
            return;
        }
        try {
            consumer.close();
        } catch (PulsarClientException e) {
            log.warn("Failed to close Pulsar consumer: {}", consumerKey, e);
        }
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getKey() {
        return key;
    }

    public PulsarDataSourceInfo getDataSourceInfo() {
        return dataSourceInfo;
    }

    /**
     * 移除Pulsar订阅并释放消费者资源。
     * 从消费者映射表中移除指定消费者并关闭连接，由 Pulsar 服务端自动清理订阅。
     *
     * @param subscribeScriptInfo 订阅信息对象，包含以下关键属性：
     *                            - key: 数据源配置键，用于获取Pulsar配置
     *                            - topic: 主题名称，用于定位消费者
     *                            - group: 订阅组名称，用于在主题内定位具体消费者
     */
    public void unsubscribe(PulsarSubscribeInfo subscribeScriptInfo) {
        String topic = subscribeScriptInfo.getTopic();
        String group = MagicAPIConfigUtils.getPulsarSubscribeConfig(
                subscribeScriptInfo.getKey(), subscribeScriptInfo.getTopic(), "group", subscribeScriptInfo.getGroup());
        if (StringUtils.isBlank(topic) || StringUtils.isBlank(group)) {
            return;
        }
        String consumeKey = topic + ":" + group;

        // 等待该 consumer 正在处理的消息完成
        waitForConsumingComplete(consumeKey);

        synchronized (this.consumerLock) {
            if (this.closing || this.closed) {
                log.warn("Pulsar 消费者正在关闭，跳过取消订阅，key: {}", key);
                return;
            }
            this.consumingCountMap.remove(consumeKey);
            Map<String, Consumer<byte[]>> groupConsumerMap = this.pulsarConsumerMap.get(topic);
            if (groupConsumerMap != null && !groupConsumerMap.isEmpty()) {
                Consumer<byte[]> consumer = groupConsumerMap.remove(group);
                if (consumer != null) {
                    closeConsumer(consumer, topic + ":" + group);
                }
            }
            if (groupConsumerMap == null || groupConsumerMap.isEmpty()) {
                this.pulsarConsumerMap.remove(topic);
            }
        }
    }
}
