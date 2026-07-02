package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.kafka.consumer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.kafka.model.KafkaDataSourceInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.kafka.model.KafkaSubscribeInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.kafka.model.KafkaSubscribeScriptInfo;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.*;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.ssssssss.magicapi.utils.JsonUtils;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 消费者
 *
 * @author zzy
 */
@Slf4j
public class KafkaConsumer {

    /**
     * 重试次数header名称常量
     */
    private static final String RETRY_COUNT_HEADER = "retry_count";

    /**
     * Kafka消费者类
     * 负责初始化消费者实例并处理来自Kafka的消息
     */
    private final String id;

    /**
     * 唯一键值，用于标识数据源
     */
    private final String key;

    /**
     * 数据源名称
     */
    private final String name;

    /**
     * Kafka消费者服务接口，定义了与Kafka消费者交互的方法
     */
    private final KafkaConsumerService kafkaConsumerService;

    /**
     * Kafka数据源信息，包含了连接Kafka所需的配置信息
     */
    private final KafkaDataSourceInfo dataSourceInfo;

    /**
     * 消费者实例
     */
    private final Map<String,Map<String,ConcurrentMessageListenerContainer>> consumerMap = new ConcurrentHashMap<>();

    /**
     * 构造函数，用于创建Kafka消费者的实例
     *
     * @param kafkaConsumerService Kafka消费者服务接口，提供消费者操作的服务层逻辑
     * @param dataSourceInfo Kafka数据源信息，包括了连接Kafka的必要配置
     * @param subscribeScriptInfos 订阅脚本信息列表，指定了消费者订阅的主题和处理逻辑
     */
    public KafkaConsumer(KafkaConsumerService kafkaConsumerService,
                         KafkaDataSourceInfo dataSourceInfo,
                         List<KafkaSubscribeScriptInfo> subscribeScriptInfos) {
        this.kafkaConsumerService = kafkaConsumerService;
        //this.consumer = createConsumer(kafkaConsumerService, dataSourceInfo, subscribeScriptInfos);
        this.dataSourceInfo = dataSourceInfo;
        this.id = dataSourceInfo.getId();
        this.name = dataSourceInfo.getName();
        this.key = dataSourceInfo.getKey();
        createConsumer(subscribeScriptInfos);
    }

    /**
     * 创建Kafka消费者工厂
     *
     * @param kafkaSubscribeInfo Kafka订阅信息，包含消费者组ID、服务器地址等配置信息
     * @return 返回一个配置好的Kafka消费者工厂
     */
    private ConsumerFactory createConsumerFactory(KafkaSubscribeInfo kafkaSubscribeInfo) {
        Map<String, Object> propsMap = new HashMap<>(16);

        propsMap.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        propsMap.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);

        // 优化后的配置调整
        propsMap.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, 100); // 减少等待时间从500ms到100ms
        propsMap.put(ConsumerConfig.REQUEST_TIMEOUT_MS_CONFIG, 30000); // 保持30秒
        propsMap.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 600000); // 保持10分钟
        propsMap.put(ConsumerConfig.RETRY_BACKOFF_MS_CONFIG, 100); // 重试退避时间100ms
        propsMap.put(ConsumerConfig.RECONNECT_BACKOFF_MAX_MS_CONFIG, 1000); // 重连最大退避时间1000ms
        propsMap.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 10000); // 心跳间隔10000ms
        propsMap.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30000); // 会话超时30000ms

        if (StringUtils.isNotBlank(kafkaSubscribeInfo.getOther())) {
            Map<String, Object> otherProps = JsonUtils.readValue(kafkaSubscribeInfo.getOther(),
                    new TypeReference<Map<String, Object>>() {});
            propsMap.putAll(otherProps);
        }

        String servers = dataSourceInfo.getServers();
        servers = SpringUtils.getAppConfig(String.format("magic.api.kafka.%s.servers",dataSourceInfo.getKey()),servers);
        propsMap.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, servers);
        propsMap.put(ConsumerConfig.GROUP_ID_CONFIG, kafkaSubscribeInfo.getGroup());

        if (kafkaSubscribeInfo.isBroadcast()) {
            propsMap.put(ConsumerConfig.GROUP_ID_CONFIG, kafkaSubscribeInfo.getGroup() + "-" + System.currentTimeMillis() + "-" + java.util.UUID.randomUUID().toString().substring(0, 8));
        }

        // 使用 ErrorHandlingDeserializer 包装 key 和 value 反序列化器
        propsMap.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        propsMap.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);

        // 配置 ErrorHandlingDeserializer 的实际反序列化器
        propsMap.put("spring.deserializer.key.delegate.class", "org.apache.kafka.common.serialization.StringDeserializer");
        propsMap.put("spring.deserializer.value.delegate.class", "org.springframework.kafka.support.serializer.JsonDeserializer");

        // 设置 JsonDeserializer 的配置
        propsMap.put("spring.json.value.default.type", "java.lang.Object");
        propsMap.put("spring.json.trusted.packages", "*");

        return new DefaultKafkaConsumerFactory(propsMap);
    }


    /**
     * 创建Kafka消费者
     *
     * 此方法负责根据提供的订阅信息列表初始化和启动Kafka消费者每个消费者都关联到特定的主题和消费者组
     * 如果已经存在对应的消费者，会先停止并销毁现有的消费者，然后创建新的消费者并启动
     *
     * @param subscribeScriptInfos 包含订阅信息的列表，用于创建消费者
     */
    public void createConsumer(List<KafkaSubscribeScriptInfo> subscribeScriptInfos) {
        if (CollectionUtils.isEmpty(subscribeScriptInfos)) {
            return;
        }
        // 去重相同的 topic + group 组合
        Map<String, KafkaSubscribeScriptInfo> uniqueSubscribers = new LinkedHashMap<>();
        for (KafkaSubscribeScriptInfo info : subscribeScriptInfos) {
            if (Objects.isNull(info) || !info.isEnabled()) {
                continue;
            }
            String key = info.getTopic() + ":" + info.getGroup();
            uniqueSubscribers.put(key, info);
        }

        for (KafkaSubscribeScriptInfo subscribeScriptInfo : uniqueSubscribers.values()) {
            String topicName = subscribeScriptInfo.getTopic();
            String group = subscribeScriptInfo.getGroup();

            if (StringUtils.isBlank(topicName) || StringUtils.isBlank(group)) {
                continue;
            }

            Map<String, ConcurrentMessageListenerContainer> consumerTopicMap = this.consumerMap.get(topicName);
            if (consumerTopicMap == null) {
                consumerTopicMap = new HashMap<>(4);
            }

            ConcurrentMessageListenerContainer oldKafkaMessageListenerContainer = consumerTopicMap.get(group);
            if (oldKafkaMessageListenerContainer != null) {
                try {
                    if (oldKafkaMessageListenerContainer.isRunning()) {
                        oldKafkaMessageListenerContainer.stop();
                    }
                    oldKafkaMessageListenerContainer.destroy();
                } catch (Exception e) {
                    log.error("Failed to stop Kafka consumer", e);
                } finally {
                    consumerTopicMap.remove(group);
                }
            }
            ContainerProperties containerProperties = new ContainerProperties(topicName);
            containerProperties.setAckMode(ContainerProperties.AckMode.BATCH);
            String ackMode = subscribeScriptInfo.getAckMode();
            if ("BATCH".equalsIgnoreCase(ackMode)) {
                containerProperties.setAckMode(ContainerProperties.AckMode.BATCH);
            } else if ("RECORD".equalsIgnoreCase(ackMode)) {
                containerProperties.setAckMode(ContainerProperties.AckMode.RECORD);
            } else if ("COUNT".equalsIgnoreCase(ackMode)) {
                containerProperties.setAckMode(ContainerProperties.AckMode.COUNT);
                if (subscribeScriptInfo.getAckCount() != null) {
                    containerProperties.setAckCount(subscribeScriptInfo.getAckCount());
                }
            } else if ("TIME".equalsIgnoreCase(ackMode)) {
                containerProperties.setAckMode(ContainerProperties.AckMode.TIME);
                if (subscribeScriptInfo.getAckTime() != null) {
                    containerProperties.setAckTime(subscribeScriptInfo.getAckTime());
                }
            } else if ("COUNT_TIME".equalsIgnoreCase(ackMode)) {
                containerProperties.setAckMode(ContainerProperties.AckMode.COUNT_TIME);
                if (subscribeScriptInfo.getAckCount() != null) {
                    containerProperties.setAckCount(subscribeScriptInfo.getAckCount());
                }
                if (subscribeScriptInfo.getAckTime() != null) {
                    containerProperties.setAckTime(subscribeScriptInfo.getAckTime());
                }
            }  else if ("MANUAL".equalsIgnoreCase(ackMode)) {
                containerProperties.setAckMode(ContainerProperties.AckMode.MANUAL);
            } else if ("MANUAL_IMMEDIATE".equalsIgnoreCase(ackMode)) {
                containerProperties.setAckMode(ContainerProperties.AckMode.MANUAL);
            }
            containerProperties.setMessageListener(new MessageListenerAdapter(dataSourceInfo.getKey(), kafkaConsumerService,subscribeScriptInfo));

            ConsumerFactory consumerFactory;
            try {
                consumerFactory = createConsumerFactory(subscribeScriptInfo);
            } catch (Exception e) {
                log.error("Failed to create consumer factory for topic: {}, group: {}", topicName, group, e);
                return;
            }

            ConcurrentMessageListenerContainer newContainer = new ConcurrentMessageListenerContainer(consumerFactory, containerProperties);
            // 获取并发数配置，如果为空则使用默认值
            Integer concurrency = subscribeScriptInfo.getConcurrency();
            if (concurrency == null || concurrency <= 0) {
                // 可以根据实际情况设置默认并发数
                concurrency = Runtime.getRuntime().availableProcessors(); // 或其他合适的默认值
            }
            newContainer.setConcurrency(concurrency);
            if (SpringUtils.isApplicationFullyStarted()) {
                newContainer.start();
            } else {
                try {
                    while (!SpringUtils.isApplicationFullyStarted()) {
                        Thread.sleep(100);
                    }
                    newContainer.start();
                    log.info("Kafka consumer started asynchronously for topic: {}", topicName);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("Interrupted while waiting for application startup", e);
                } catch (Exception e) {
                    log.error("Failed to start Kafka consumer asynchronously for topic: {}", topicName, e);
                }
            }

            consumerTopicMap.put(group, newContainer);
            this.consumerMap.put(topicName, consumerTopicMap);
        }
    }

    /**
     * 关闭所有Kafka消费者
     *
     * 此方法遍历存储所有消费者容器的映射表，对于每个消费者容器，如果其处于运行状态，
     * 则停止并销毁该容器，以确保消费者能够优雅地关闭
     * 如果在关闭过程中遇到任何错误，将记录错误信息
     */
    public void close() {
        if (MapUtils.isEmpty(consumerMap)) {
            return;
        }

        for (Map.Entry<String, Map<String, ConcurrentMessageListenerContainer>> entry : consumerMap.entrySet()) {
            String topicKey = entry.getKey();
            Map<String, ConcurrentMessageListenerContainer> containerMap = entry.getValue();

            if (MapUtils.isEmpty(containerMap)) {
                continue;
            }

            closeConsumersForTopic(topicKey, containerMap);
        }

        consumerMap.clear();
    }

    /**
     * 关闭指定主题的所有Kafka消费者容器
     *
     * @param topicKey 主题键值，用于标识需要关闭消费者的主题
     * @param containerMap 消费者容器映射表，包含所有需要关闭的消费者容器
     */
    private void closeConsumersForTopic(String topicKey, Map<String, ConcurrentMessageListenerContainer> containerMap) {
        // 遍历所有消费者容器并逐个关闭
        for (Map.Entry<String, ConcurrentMessageListenerContainer> innerEntry : containerMap.entrySet()) {
            String containerKey = innerEntry.getKey();
            ConcurrentMessageListenerContainer container = innerEntry.getValue();

            if (Objects.isNull(container)) {
                continue;
            }

            try {
                // 如果容器正在运行，则先停止容器
                if (container.isRunning()) {
                    // 使用 CompletableFuture 实现带超时的 stop 操作
                    CompletableFuture<Void> stopFuture = CompletableFuture.runAsync(() -> {
                        container.stop();
                    });

                    // 设置超时时间，例如10秒
                    stopFuture.get(10, TimeUnit.SECONDS);
                    log.info("Stopped Kafka consumer for topic: {}, container key: {}", topicKey, containerKey);
                }
                // 销毁容器资源
                container.destroy();
            } catch (TimeoutException e) {
                log.warn("Timeout while stopping Kafka consumer for topic: {}, container key: {}. Forcing destroy.",
                        topicKey, containerKey);
                // 超时时直接销毁容器
                forceDestroyContainer(container);
            } catch (Exception e) {
                log.error("Failed to close Kafka consumer for topic: {}, container key: {}", topicKey, containerKey, e);
                // 异常时也尝试强制销毁
                forceDestroyContainer(container);
            }
        }
    }


    /**
     * 强制销毁容器
     */
    private void forceDestroyContainer(ConcurrentMessageListenerContainer container) {
        try {
            container.destroy();
        } catch (Exception destroyException) {
            log.error("Failed to force destroy Kafka consumer container", destroyException);
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
     * @return Kafka数据源信息对象
     */
    public KafkaDataSourceInfo getDataSourceInfo() {
        return dataSourceInfo;
    }



    /**
     * 取消订阅队列
     *
     * @param subscribeScriptInfo 订阅信息
     */
    public void unsubscribe(KafkaSubscribeInfo subscribeScriptInfo) {
        String topic = subscribeScriptInfo.getTopic();
        String group = subscribeScriptInfo.getGroup();
        // 检查参数是否为空
        if (StringUtils.isBlank(topic) || StringUtils.isBlank(group)) {
            return;
        }
        // 检查消费者映射是否存在对应主题
        Map<String, ConcurrentMessageListenerContainer> topicMap = consumerMap.get(topic);
        if (topicMap == null) {
            return;
        }

        // 检查是否存在对应组的消费者
        ConcurrentMessageListenerContainer consumerWrapper = topicMap.get(group);
        if (consumerWrapper == null) {
            return;
        }

        try {
            // 停止并销毁消费者
            if (consumerWrapper.isRunning()) {
                // 先暂停消费再停止，更优雅地关闭
                consumerWrapper.pause();
                consumerWrapper.stop();
            }
            consumerWrapper.destroy();
        } catch (Exception e) {
            log.warn("Failed to close Kafka consumer for topic: {}, group: {}. This might be due to coordinator not aware of member.", topic, group, e);
            // 忽略此类错误，因为消费者可能已经被协调器移除
        }

        // 移除组级别的消费者
        topicMap.remove(group);

        // 如果该主题下已无消费者组，则移除整个主题
        if (topicMap.isEmpty()) {
            consumerMap.remove(topic);
        }
    }

    // 提取监听器为独立类，便于测试和复用
    private static class MessageListenerAdapter implements AcknowledgingMessageListener<Object, Object> {

        /** 数据源键值，用于标识特定的数据源配置 */
        private final String dataSourceKey;

        /** Kafka消费者服务实例，用于处理Kafka消息消费逻辑 */
        private final KafkaConsumerService kafkaConsumerService;
        /**
         * 确认标志位
         *
         * 这是一个布尔类型的确认标志，用于标识某个操作或状态是否需要确认
         * 当前初始化为null，表示尚未设置确认状态
         *
         * 注意：由于是Boolean包装类型且初始化为null，使用时需要进行空值判断
         */
        private final Boolean acknowledgeFlag;

        /** 最大重试次数，当操作失败时允许的最大重试次数 */
        private final int maxRetryTimes;

        /** 重试退避时间（毫秒），每次重试之间的等待时间间隔 */
        private final long retryBackoffMs;

        /**
         * 构造函数，初始化MessageListenerAdapter实例
         *
         * @param dataSourceKey 数据源键值，用于标识数据源
         * @param kafkaConsumerService Kafka消费者服务，用于处理Kafka消息消费逻辑
         * @param subscribeScriptInfo 订阅脚本信息，包含订阅相关的配置信息
         */
        public MessageListenerAdapter(String dataSourceKey, KafkaConsumerService kafkaConsumerService, KafkaSubscribeScriptInfo subscribeScriptInfo) {
            this.dataSourceKey = dataSourceKey;
            this.kafkaConsumerService = kafkaConsumerService;
            String ackMode = subscribeScriptInfo.getAckMode();
            boolean tmpAcknowledgeFlag = true;
            if (!"MANUAL".equalsIgnoreCase(ackMode) && !"MANUAL_IMMEDIATE".equalsIgnoreCase(ackMode)) {
                tmpAcknowledgeFlag = false;
            }
            if (tmpAcknowledgeFlag) {
                if (StringUtils.isNotBlank(subscribeScriptInfo.getOther())) {
                    Map<String, Object> otherProps = JsonUtils.readValue(subscribeScriptInfo.getOther(),
                            new TypeReference<Map<String, Object>>() {});
                    Object enableAutoCommitObj = otherProps.get(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG);
                    if (enableAutoCommitObj != null && enableAutoCommitObj instanceof Boolean) {
                        tmpAcknowledgeFlag = !((Boolean) enableAutoCommitObj);
                    }
                }
            }
            acknowledgeFlag = tmpAcknowledgeFlag;
            // 从 subscribeScriptInfo.getOther() 中获取重试配置
            int maxRetryTimes = 3;  // 默认值
            long retryBackoffMs = 5000;  // 默认值 5秒

            // 解析 other 配置中的重试参数
            if (StringUtils.isNotBlank(subscribeScriptInfo.getOther())) {
                try {
                    Map<String, Object> otherConfig = JsonUtils.readValue(subscribeScriptInfo.getOther(),
                            new TypeReference<Map<String, Object>>() {});

                    // 获取最大重试次数
                    Object maxRetriesObj = otherConfig.get("maxRetryTimes");
                    if (maxRetriesObj != null) {
                        if (maxRetriesObj instanceof Number) {
                            maxRetryTimes = ((Number) maxRetriesObj).intValue();
                        } else {
                            maxRetryTimes = Integer.parseInt(maxRetriesObj.toString());
                        }
                    }

                    // 获取重试间隔
                    Object backoffObj = otherConfig.get("retryBackoffMs");
                    if (backoffObj != null) {
                        if (backoffObj instanceof Number) {
                            retryBackoffMs = ((Number) backoffObj).longValue();
                        } else {
                            retryBackoffMs = Long.parseLong(backoffObj.toString());
                        }
                    }
                } catch (Exception e) {
                    log.warn("Failed to parse retry configuration from other config, using defaults", e);
                }
            }

            this.maxRetryTimes = maxRetryTimes;
            this.retryBackoffMs = retryBackoffMs;
        }

        /**
         * 获取消息的重试次数
         *
         * @param msg 消费者记录对象，包含消息头信息
         * @return 返回消息已重试的次数，如果无法获取则返回0
         */
        private int getRetryCount(ConsumerRecord<Object, Object> msg) {
            // 获取已重试次数（从 header 中读取）
            Headers headers = msg.headers();
            Header retryHeader = headers.lastHeader(RETRY_COUNT_HEADER);
            // 解析重试次数
            if (retryHeader != null) {
                try {
                    // 使用ByteBuffer正确解析4字节整数
                    ByteBuffer buffer = ByteBuffer.wrap(retryHeader.value());
                    return buffer.getInt();
                } catch (Exception e) {
                    // 记录解析异常，但不影响主流程
                    log.warn("Failed to parse retry count from header", e);
                }
            }
            return 0;
        }

        /**
         * 重置消息的重试次数
         *
         * @param msg 消费者记录对象，包含消息头信息
         * @param retryCount 新的重试次数值
         */
        private void resetRetryCount(ConsumerRecord<Object, Object> msg, int retryCount) {
            // 参数验证
            if (retryCount < 0) {
                throw new IllegalArgumentException("retryCount must be non-negative");
            }

            // 重置已重试次数
            Headers headers = msg.headers();
            // 清除旧的retry_count header并添加新的
            headers.remove(RETRY_COUNT_HEADER);
            headers.add(RETRY_COUNT_HEADER, ByteBuffer.allocate(Integer.BYTES).putInt(retryCount).array());
        }

        /**
         * 当接收到消息时调用的方法
         *
         * @param msg 消息记录，包含消息的元数据和实际值
         * @param acknowledgment 用于确认消息已成功处理或发送失败的接口
         */
        @Override
        public void onMessage(ConsumerRecord<Object, Object> msg, Acknowledgment acknowledgment) {
            try {
                // 记录消息的来源主题和偏移量，用于调试和监控
                log.debug("Consuming message from topic: {}, offset: {}", msg.topic(), msg.offset());
                // 调用Kafka消费者服务来处理消息
                // 这里的dataSourceKey是消息来源的标识，用于特定数据源的配置和识别
                boolean success = kafkaConsumerService.consume(dataSourceKey, msg);
                // 根据消息处理结果进行确认或否定确认
                if (success) {
                    if (acknowledgeFlag) {
                        acknowledgment.acknowledge();
                    }
                } else {
                    throw new RuntimeException("Consumption failed for message");
                }
            } catch (Exception e) {
                log.error("Error consuming message from topic: {}, partition: {}, offset: {}",
                        msg.topic(), msg.partition(), msg.offset(), e);
                if (acknowledgeFlag) {
                    int retryCount = getRetryCount(msg);
                    if (retryCount < maxRetryTimes) {
                        resetRetryCount(msg, retryCount);
                        acknowledgment.nack(Duration.ofDays(retryBackoffMs));
                    } else {
                        log.warn("Max retry attempts reached for message. Acknowledging to avoid infinite retry. Topic: {}, Partition: {}, Offset: {}",
                                msg.topic(), msg.partition(), msg.offset());
                        acknowledgment.acknowledge();
                    }
                }
            }
        }
    }

}
