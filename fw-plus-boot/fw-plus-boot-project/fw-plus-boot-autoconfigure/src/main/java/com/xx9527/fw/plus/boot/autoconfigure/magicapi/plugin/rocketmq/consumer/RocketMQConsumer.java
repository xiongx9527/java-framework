package com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rocketmq.consumer;

import com.alibaba.fastjson2.JSON;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.common.MagicAPIConfigUtils;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rocketmq.common.BaseService;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rocketmq.model.RocketMqDataSourceInfo;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rocketmq.model.RocketMqSubscribeInfo;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rocketmq.model.RocketMqSubscribeScriptInfo;
import com.xx9527.fw.plus.common.util.NetUtils;
import com.xx9527.fw.plus.common.util.SpringUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.acl.common.AclClientRPCHook;
import org.apache.rocketmq.acl.common.SessionCredentials;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.ServiceState;
import org.apache.rocketmq.common.message.MessageExt;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 消费者
 *
 * @author zzy
 */
@Slf4j
public class RocketMQConsumer implements BaseService {

    /**
     * 数据源唯一标识符
     */
    private final String id;

    /**
     * 数据源访问密钥
     */
    private final String key;

    /**
     * 数据源名称
     */
    private final String name;

    /**
     * RocketMQ数据源信息配置
     */
    private final RocketMqDataSourceInfo dataSourceInfo;

    /**
     * RocketMQ消费者服务实例
     */
    private final RocketMqConsumerService rocketMqConsumerService;


    /**
     * 消费者实例
     */
    private final Map<String, Map<String, DefaultMQPushConsumer>> consumerMap = new ConcurrentHashMap<>();

    /**
     * RocketMQ消费者构造函数
     *
     * @param rocketMqConsumerService RocketMQ消费者服务实例，用于提供消费者相关的服务功能
     * @param dataSourceInfo 数据源信息，包含连接RocketMQ所需的基本配置信息
     * @param mqSubscribeScriptInfos 订阅脚本信息列表，定义了需要订阅的主题和相关配置
     * @throws MQClientException 当MQ客户端初始化失败时抛出此异常
     */
    public RocketMQConsumer(RocketMqConsumerService rocketMqConsumerService,
                            RocketMqDataSourceInfo dataSourceInfo,
                            List<RocketMqSubscribeScriptInfo> mqSubscribeScriptInfos) throws MQClientException {
        this.rocketMqConsumerService = rocketMqConsumerService;
        this.dataSourceInfo = dataSourceInfo;
        this.id = dataSourceInfo.getId();
        this.name = dataSourceInfo.getName();
        this.key = dataSourceInfo.getKey();
        createConsumer(rocketMqConsumerService, dataSourceInfo, mqSubscribeScriptInfos);
    }


    /**
     * 创建消息消费者
     *
     * @param mqSubscribeScriptInfos RocketMQ订阅脚本信息列表，包含消费者订阅的topic、tag等配置信息
     */
    public void createConsumer(List<RocketMqSubscribeScriptInfo> mqSubscribeScriptInfos){
        try {
            // 调用具体的消费者创建方法
            this.createConsumer(rocketMqConsumerService, dataSourceInfo, mqSubscribeScriptInfos);
        } catch (MQClientException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 配置RocketMQ消费者的各项参数。
     * <p>
     * 该方法根据传入的订阅脚本信息（subscribeScriptInfo）中的"other"字段解析出一个JSON对象，
     * 并基于其中的配置项设置DefaultMQPushConsumer的相关属性。若某些配置未提供，则使用推荐的默认值。
     * </p>
     *
     * @param consumer 消息消费者实例，用于接收并处理消息
     * @param subscribeScriptInfo 订阅脚本信息对象，包含自定义配置字符串"other"
     */
    private void configureConsumer(DefaultMQPushConsumer consumer, RocketMqSubscribeScriptInfo subscribeScriptInfo) {
        String other = subscribeScriptInfo.getOther();
        other = MagicAPIConfigUtils.getRocketMQSubscribeConfig(dataSourceInfo.getKey(), subscribeScriptInfo.getTopic(), "other", other);
        Map<String, Object> otherMap = new HashMap<>();

        if (StringUtils.isNotBlank(other)) {
            otherMap = JSON.parseObject(other);
        }

        // 设置消费线程数：控制并发消费能力，默认最小10，最大20
        int consumeThreadMin = ((Number) otherMap.getOrDefault("consumeThreadMin", 10)).intValue();
        int consumeThreadMax = ((Number) otherMap.getOrDefault("consumeThreadMax", 20)).intValue();
        consumer.setConsumeThreadMin(consumeThreadMin);
        consumer.setConsumeThreadMax(consumeThreadMax);

        // 设置拉取消息相关配置：影响从Broker拉取消息的行为
        int pullThresholdForQueue = ((Number) otherMap.getOrDefault("pullThresholdForQueue", 1000)).intValue();
        int pullBatchSize = ((Number) otherMap.getOrDefault("pullBatchSize", 32)).intValue();
        long pullInterval = ((Number) otherMap.getOrDefault("pullInterval", 0L)).longValue();
        int pullThresholdSizeForQueue = ((Number) otherMap.getOrDefault("pullThresholdSizeForQueue", 100)).intValue();

        consumer.setPullThresholdForQueue(pullThresholdForQueue);
        consumer.setPullBatchSize(pullBatchSize);
        consumer.setPullInterval(pullInterval);
        consumer.setPullThresholdSizeForQueue(pullThresholdSizeForQueue);

        // 设置每次消费的最大批量大小，默认为1条消息
        int consumeMessageBatchMaxSize = ((Number) otherMap.getOrDefault("consumeMessageBatchMaxSize", 1)).intValue();
        consumer.setConsumeMessageBatchMaxSize(consumeMessageBatchMaxSize);

        // 设置单条消息消费超时时间，默认为15分钟
        long consumeTimeout = ((Number) otherMap.getOrDefault("consumeTimeout", 15L)).longValue();
        consumer.setConsumeTimeout(consumeTimeout);

        // 设置最大重新消费次数，默认-1表示使用系统默认值（即16次）
        int maxReconsumeTimes = ((Number) otherMap.getOrDefault("maxReconsumeTimes", -1)).intValue();
        consumer.setMaxReconsumeTimes(maxReconsumeTimes);

        // 设置消费起始位置策略，默认从第一个偏移量开始消费
        String consumeFromWhere = (String) otherMap.getOrDefault("consumeFromWhere", "CONSUME_FROM_FIRST_OFFSET");
        switch (consumeFromWhere) {
            case "CONSUME_FROM_FIRST_OFFSET":
                consumer.setConsumeFromWhere(org.apache.rocketmq.common.consumer.ConsumeFromWhere.CONSUME_FROM_FIRST_OFFSET);
                break;
            case "CONSUME_FROM_TIMESTAMP":
                consumer.setConsumeFromWhere(org.apache.rocketmq.common.consumer.ConsumeFromWhere.CONSUME_FROM_TIMESTAMP);
                break;
            default:
                consumer.setConsumeFromWhere(org.apache.rocketmq.common.consumer.ConsumeFromWhere.CONSUME_FROM_FIRST_OFFSET);
                break;
        }

        // 设置消息模型：集群模式或广播模式，默认为集群模式
        String messageModel = (String) otherMap.getOrDefault("messageModel", "CLUSTERING");
        if ("BROADCASTING".equals(messageModel)) {
            consumer.setMessageModel(org.apache.rocketmq.remoting.protocol.heartbeat.MessageModel.BROADCASTING);
        } else {
            consumer.setMessageModel(org.apache.rocketmq.remoting.protocol.heartbeat.MessageModel.CLUSTERING);
        }

        // 其他辅助性配置项，采用默认值进行初始化
        long suspendCurrentQueueTimeMillis = ((Number) otherMap.getOrDefault("suspendCurrentQueueTimeMillis", 1000L)).longValue();
        consumer.setSuspendCurrentQueueTimeMillis(suspendCurrentQueueTimeMillis);

        int consumeConcurrentlyMaxSpan = ((Number) otherMap.getOrDefault("consumeConcurrentlyMaxSpan", 2000)).intValue();
        consumer.setConsumeConcurrentlyMaxSpan(consumeConcurrentlyMaxSpan);

        boolean postSubscriptionWhenPull = (Boolean) otherMap.getOrDefault("postSubscriptionWhenPull", false);
        consumer.setPostSubscriptionWhenPull(postSubscriptionWhenPull);
    }


    /**
     * 创建并启动 RocketMQ 消费者实例。
     * <p>
     * 该方法根据传入的数据源信息和订阅脚本信息列表，构建唯一的消费者组（基于 topic 和 group），
     * 并确保每个消费者只被创建一次。如果已有正在运行的同名消费者，则会先暂停、取消订阅、恢复并关闭它，
     * 再重新创建新的消费者实例。
     * </p>
     * <p>
     * 若配置了 ACL 认证信息（accessKey 和 secretKey），则使用带认证的消费者；否则使用默认消费者。
     * 同时注册消息监听器，并在 Spring 容器完全启动后异步启动消费者。
     * </p>
     *
     * @param rocketMqConsumerService 用于处理接收到的消息的服务接口，不可为 null
     * @param dataSourceInfo          数据源连接信息，包括 NameServer 地址及可选的 ACL 配置
     * @param mqSubscribeScriptInfos  订阅脚本信息列表，包含 topic、group、tags 及是否启用等信息
     * @throws MQClientException 当消费者初始化或启动过程中发生 RocketMQ 相关异常时抛出
     */
    private void createConsumer(RocketMqConsumerService rocketMqConsumerService, RocketMqDataSourceInfo dataSourceInfo,
                                                List<RocketMqSubscribeScriptInfo> mqSubscribeScriptInfos) throws MQClientException {
        // 去重：以 topic:group 作为唯一标识保留有效的订阅信息
        Map<String, RocketMqSubscribeScriptInfo> uniqueSubscribers = new LinkedHashMap<>();
        for (RocketMqSubscribeScriptInfo info : mqSubscribeScriptInfos) {
            Boolean enable = enable(info);
            if (!Boolean.TRUE.equals(enable)) {
                continue;
            }
            String group = info.getGroup();
            group = MagicAPIConfigUtils.getRocketMQSubscribeConfig(dataSourceInfo.getKey(), info.getTopic(), "group", group);
            String key = info.getTopic() + ":" + group;
            uniqueSubscribers.put(key, info);
        }

        // 遍历去重后的订阅信息，逐个创建消费者
        for (RocketMqSubscribeScriptInfo info : uniqueSubscribers.values()) {
            String topicName = info.getTopic();
            String group = info.getGroup();
            group = MagicAPIConfigUtils.getRocketMQSubscribeConfig(dataSourceInfo.getKey(), topicName, "group", group);

            if (StringUtils.isBlank(topicName) || StringUtils.isBlank(group)) {
                continue;
            }

            // 获取当前 topic 下已有的消费者映射表
            Map<String, DefaultMQPushConsumer> consumerTopicMap = this.consumerMap.get(topicName);
            if (consumerTopicMap == null) {
                consumerTopicMap = new HashMap<>(4);
            }

            // 如果存在旧消费者且处于运行状态，则尝试安全地关闭它
            DefaultMQPushConsumer oldConsumer = consumerTopicMap.get(group);
            if (oldConsumer != null
                    && oldConsumer.getDefaultMQPushConsumerImpl().getServiceState() == ServiceState.RUNNING) {
                try {
                    oldConsumer.suspend();
                } catch (Exception e) {
                    log.error("Error suspend consumer for topic: {}", topicName, e);
                }
                try {
                    oldConsumer.unsubscribe(topicName);
                } catch (Exception e) {
                    log.error("Error unsubscribe consumer for topic: {}", topicName, e);
                }
                try {
                    oldConsumer.resume();
                } catch (Exception e) {
                    log.error("Error resume consumer for topic: {}", topicName, e);
                }
                try {
                    oldConsumer.shutdown();
                } catch (Exception e) {
                    log.error("Error shutting down consumer for topic: {}", topicName, e);
                }
            }

            // 根据是否有 ACL 凭据决定创建哪种类型的消费者
            String accessKey = dataSourceInfo.getAccessKey();
            accessKey = MagicAPIConfigUtils.getRocketMQConfig(dataSourceInfo.getKey(), "accessKey", accessKey);
            String secretKey = dataSourceInfo.getSecretKey();
            secretKey = MagicAPIConfigUtils.getRocketMQConfig(dataSourceInfo.getKey(), "secretKey", secretKey);
            String nameServer = dataSourceInfo.getNameServer();
            nameServer = MagicAPIConfigUtils.getRocketMQConfig(dataSourceInfo.getKey(), "nameServer", nameServer);
            DefaultMQPushConsumer consumer;
            if (StringUtils.isBlank(secretKey) || StringUtils.isBlank(accessKey)) {
                consumer = new DefaultMQPushConsumer();
            } else {
                consumer = new DefaultMQPushConsumer(
                        new AclClientRPCHook(new SessionCredentials(accessKey, secretKey)));
            }

            // 设置消费者基本属性
            consumer.setNamesrvAddr(nameServer);
            consumer.setConsumerGroup(group);

            // 如果是广播模式，则动态生成一个唯一的消费者组名称避免冲突
            if (info.isBroadcast()) {
                String serviceName = SpringUtils.getApplicationName();
                String serverPort = SpringUtils.getServerPort();
                String macAddress = NetUtils.getMacAddress();
                consumer.setConsumerGroup(info.getGroup() + "-broadcast-" + serviceName + "-" + serverPort + "-" + macAddress);
            }

            // 执行实际的订阅操作
            consumer.subscribe(topicName, info.getTags());
            configureConsumer(consumer, info);

            // 注册消息监听器逻辑
            consumer.registerMessageListener((MessageListenerConcurrently) (msgs, context) -> {
                MessageExt msg = msgs.get(0);
                log.info("Received message: " + new String(msg.getBody()));
                if (Objects.isNull(rocketMqConsumerService)) {
                    return ConsumeConcurrentlyStatus.RECONSUME_LATER;
                } else if (!rocketMqConsumerService.consume(mqSubscribeScriptInfos, msg)) {
                    return ConsumeConcurrentlyStatus.RECONSUME_LATER;
                }
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            });

            // 在 Spring 应用上下文完全加载后再启动消费者
            if (SpringUtils.isApplicationFullyStarted()) {
                consumer.start();
            } else {
                try {
                    while (!SpringUtils.isApplicationFullyStarted()) {
                        Thread.sleep(100);
                    }
                    consumer.start();
                    log.info("rocketmq consumer started asynchronously for topic: {}", topicName);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("Interrupted while waiting for application startup", e);
                } catch (Exception e) {
                    log.error("Failed to start rocketmq consumer asynchronously for topic: {}", topicName, e);
                }
            }

            // 将新创建的消费者保存到内存中以便后续管理
            consumerTopicMap.put(group, consumer);
            this.consumerMap.put(topicName, consumerTopicMap);
        }
    }


    /**
     * 关闭所有消费者并清理资源
     * <p>
     * 该方法会遍历所有的消费者映射表，对每个消费者执行以下操作：
     * 1. 暂停消费者
     * 2. 取消订阅主题
     * 3. 恢复消费者状态
     * 4. 关闭消费者
     * 最后清空所有容器和映射表
     */
    public void close() {
        // 检查消费者映射表是否为空，如果为空则直接返回
        if (MapUtils.isEmpty(consumerMap)) {
            return;
        }

        // 遍历所有主题键值对
        for (Map.Entry<String, Map<String, DefaultMQPushConsumer>> entry : consumerMap.entrySet()) {
            String topicKey = entry.getKey();
            Map<String, DefaultMQPushConsumer> containerMap = entry.getValue();

            // 检查容器映射表是否为空，如果为空则跳过
            if (MapUtils.isEmpty(containerMap)) {
                continue;
            }

            // 遍历容器中的所有消费者
            for (Map.Entry<String, DefaultMQPushConsumer> e1 : containerMap.entrySet()) {
                DefaultMQPushConsumer consumerWrapper = e1.getValue();
                // 检查消费者是否不为空且处于运行状态
                if (consumerWrapper != null
                        && consumerWrapper.getDefaultMQPushConsumerImpl().getServiceState() == ServiceState.RUNNING) {
                    try {
                        consumerWrapper.suspend();
                    } catch (Exception e) {
                        log.error("Error suspend consumer for topic: {}", topicKey, e);
                    }
                    try {
                        consumerWrapper.unsubscribe(topicKey);
                    } catch (Exception e) {
                        log.error("Error unsubscribe consumer for topic: {}", topicKey, e);
                    }
                    try {
                        consumerWrapper.resume();
                    } catch (Exception e) {
                        log.error("Error resume consumer for topic: {}", topicKey, e);
                    }
                    try {
                        consumerWrapper.shutdown();
                    } catch (Exception e) {
                        log.error("Error shutting down consumer for topic: {}", topicKey, e);
                    }
                }
            }
            // 清空容器映射表
            containerMap.clear();
        }

        // 清空消费者映射表
        consumerMap.clear();
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

    public RocketMqDataSourceInfo getDataSourceInfo() {
        return dataSourceInfo;
    }

    /**
     * 取消订阅主题
     *
     * @param subscribeScriptInfo 订阅信息
     */
    public void unsubscribe(RocketMqSubscribeInfo subscribeScriptInfo) {
        String topic = subscribeScriptInfo.getTopic();
        String group = subscribeScriptInfo.getGroup();
        group = MagicAPIConfigUtils.getRocketMQSubscribeConfig(dataSourceInfo.getKey(), subscribeScriptInfo.getTopic(), "group", group);
        // 检查参数是否为空
        if (StringUtils.isBlank(topic) || StringUtils.isBlank(group)) {
            return;
        }
        // 检查消费者映射是否存在对应主题
        Map<String, DefaultMQPushConsumer> topicMap = consumerMap.get(topic);
        if (topicMap == null) {
            return;
        }

        // 检查是否存在对应组的消费者
        DefaultMQPushConsumer consumerWrapper = topicMap.get(group);
        if (consumerWrapper == null) {
            return;
        }

        if (consumerWrapper.getDefaultMQPushConsumerImpl().getServiceState() == ServiceState.RUNNING) {
            try {
                consumerWrapper.suspend();
            } catch (Exception e) {
                log.error("Error suspend consumer for topic: {}", topic, e);
            }
            try {
                consumerWrapper.unsubscribe(subscribeScriptInfo.getTopic());
            } catch (Exception e) {
                log.error("Error unsubscribe consumer for topic: {}", topic, e);
            }
            try {
                consumerWrapper.resume();
            } catch (Exception e) {
                log.error("Error resume consumer for topic: {}", topic, e);
            }
            try {
                consumerWrapper.shutdown();
            } catch (Exception e) {
                log.error("Error shutting down consumer for topic: {}", topic, e);
            }
        }
        // 移除组级别的消费者
        topicMap.remove(group);
        // 如果该主题下已无消费者组，则移除整个主题
        if (topicMap.isEmpty()) {
            consumerMap.remove(topic);
        }
    }
}
