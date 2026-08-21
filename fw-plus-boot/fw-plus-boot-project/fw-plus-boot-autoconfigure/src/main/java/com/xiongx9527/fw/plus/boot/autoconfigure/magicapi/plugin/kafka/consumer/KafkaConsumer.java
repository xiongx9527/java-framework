package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.kafka.consumer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.common.MagicAPIConfigUtils;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.kafka.common.BaseService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.kafka.model.KafkaDataSourceInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.kafka.model.KafkaSubscribeInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.kafka.model.KafkaSubscribeScriptInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.kafka.model.MagicDynamicKafkaDataSource;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.AcknowledgingMessageListener;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.ssssssss.magicapi.utils.JsonUtils;

import java.time.Duration;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * 消费者
 *
 * @author zzy
 */
@Slf4j
public class KafkaConsumer implements BaseService {

    /** 消息重试次数的Header键名 */
    private static final String RETRY_COUNT_HEADER = "retry_count";

    /** 应用启动等待超时时间（秒） */
    private static final long STARTUP_WAIT_TIMEOUT_SECONDS = 60;

    /** 默认最大重试次数 */
    static final int DEFAULT_MAX_RETRY_TIMES = 3;

    /** 默认重试退避时间（毫秒） */
    static final long DEFAULT_RETRY_BACKOFF_MS = 5000;

    /** Ack模式映射表，将字符串模式名映射为ContainerProperties配置操作 */
    private static final Map<String, Consumer<ContainerProperties>> ACK_MODE_MAP = new HashMap<>(8);
    static {
        ACK_MODE_MAP.put("BATCH", p -> p.setAckMode(ContainerProperties.AckMode.BATCH));
        ACK_MODE_MAP.put("RECORD", p -> p.setAckMode(ContainerProperties.AckMode.RECORD));
        ACK_MODE_MAP.put("COUNT", p -> p.setAckMode(ContainerProperties.AckMode.COUNT));
        ACK_MODE_MAP.put("TIME", p -> p.setAckMode(ContainerProperties.AckMode.TIME));
        ACK_MODE_MAP.put("COUNT_TIME", p -> p.setAckMode(ContainerProperties.AckMode.COUNT_TIME));
        ACK_MODE_MAP.put("MANUAL", p -> p.setAckMode(ContainerProperties.AckMode.MANUAL));
        ACK_MODE_MAP.put("MANUAL_IMMEDIATE", p -> p.setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE));
    }

    /**
     * 唯一标识符
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

    /** 正在消费中的消息数，key 为 topic:group 格式 */
    private final Map<String, AtomicInteger> consumingCountMap = new ConcurrentHashMap<>();

    /** 消费者操作锁，保护 create / unsubscribe / close 的并发安全 */
    private final Object consumerLock = new Object();

    /** 动态 Kafka 数据源引用，用于 DLT 发送 */
    private final MagicDynamicKafkaDataSource magicDynamicKafkaDataSource;

    /** 正在关闭中标记 */
    private volatile boolean closing = false;

    /** 关闭标记 */
    private volatile boolean closed = false;

    /**
     * 构造函数，用于创建Kafka消费者的实例
     *
     * @param kafkaConsumerService Kafka消费者服务接口，提供消费者操作的服务层逻辑
     * @param dataSourceInfo Kafka数据源信息，包括了连接Kafka的必要配置
     * @param subscribeScriptInfos 订阅脚本信息列表，指定了消费者订阅的主题和处理逻辑
     */
    public KafkaConsumer(KafkaConsumerService kafkaConsumerService,
                         KafkaDataSourceInfo dataSourceInfo,
                         List<KafkaSubscribeScriptInfo> subscribeScriptInfos,
                         MagicDynamicKafkaDataSource magicDynamicKafkaDataSource) {
        this.kafkaConsumerService = kafkaConsumerService;
        this.dataSourceInfo = dataSourceInfo;
        this.magicDynamicKafkaDataSource = magicDynamicKafkaDataSource;
        this.id = dataSourceInfo.getId();
        this.name = dataSourceInfo.getName();
        this.key = dataSourceInfo.getKey();
        this.createConsumer(subscribeScriptInfos);
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
        propsMap.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30000);
        String group = kafkaSubscribeInfo.getGroup();
        group = MagicAPIConfigUtils.getKafkaSubscribeConfig(this.dataSourceInfo.getKey(), kafkaSubscribeInfo.getTopic(), "group", group);

        String broadcastStr = MagicAPIConfigUtils.getKafkaSubscribeConfig(this.dataSourceInfo.getKey(), kafkaSubscribeInfo.getTopic(), "broadcast", String.valueOf(kafkaSubscribeInfo.isBroadcast()));
        boolean broadcast = false;
        try {
            broadcast = Boolean.parseBoolean(broadcastStr);
        } catch (Exception ignored) {
        }
        if (broadcast) {
            group = group + "-broadcast-" + getUniqueId();
        }

        String other = kafkaSubscribeInfo.getOther();
        other = MagicAPIConfigUtils.getKafkaSubscribeConfig(this.dataSourceInfo.getKey(), kafkaSubscribeInfo.getTopic(), "other", other);
        if (StringUtils.isNotBlank(other)) {
            Map<String, Object> otherProps = JsonUtils.readValue(other,new TypeReference<Map<String, Object>>() {});
            propsMap.putAll(otherProps);
        }

        String servers = this.dataSourceInfo.getServers();
        servers = MagicAPIConfigUtils.getKafkaConfig(this.dataSourceInfo.getKey(), "servers", servers);
        propsMap.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, servers);
        propsMap.put(ConsumerConfig.GROUP_ID_CONFIG, group);

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
     * 创建/刷新Kafka消费者。
     * 会关闭不在新订阅列表中的旧消费者，防止连接泄漏。
     */
    public void createConsumer(List<KafkaSubscribeScriptInfo> subscribeScriptInfos) {
        if (CollectionUtils.isEmpty(subscribeScriptInfos)) {
            return;
        }

        // 构建目标订阅集合
        Map<String, KafkaSubscribeScriptInfo> targetSubscribers = new LinkedHashMap<>();
        for (KafkaSubscribeScriptInfo info : subscribeScriptInfos) {
            if (!Boolean.TRUE.equals(enable(info))) {
                continue;
            }
            String subscriptionName = MagicAPIConfigUtils.getKafkaSubscribeConfig(
                    this.dataSourceInfo.getKey(), info.getTopic(), "group", info.getGroup());
            String topicName = info.getTopic();
            if (StringUtils.isBlank(topicName) || StringUtils.isBlank(subscriptionName)) {
                continue;
            }
            targetSubscribers.putIfAbsent(topicName + ":" + subscriptionName, info);
        }

        synchronized (this.consumerLock) {
            if (this.closing || this.closed) {
                log.warn("Kafka 消费者正在关闭，跳过创建新消费者，key: {}", key);
                return;
            }
            // 关闭不再需要的旧消费者
            for (Map.Entry<String, Map<String, ConcurrentMessageListenerContainer>> topicEntry : this.consumerMap.entrySet()) {
                String existingTopic = topicEntry.getKey();
                Map<String, ConcurrentMessageListenerContainer> existingGroups = topicEntry.getValue();
                if (existingGroups == null) {
                    continue;
                }
                Iterator<Map.Entry<String, ConcurrentMessageListenerContainer>> groupIter = existingGroups.entrySet().iterator();
                while (groupIter.hasNext()) {
                    Map.Entry<String, ConcurrentMessageListenerContainer> groupEntry = groupIter.next();
                    String existingGroup = groupEntry.getKey();
                    String compositeKey = existingTopic + ":" + existingGroup;
                    if (!targetSubscribers.containsKey(compositeKey)) {
                        ConcurrentMessageListenerContainer oldContainer = groupEntry.getValue();
                        closeContainer(oldContainer, compositeKey);
                        this.consumingCountMap.remove(compositeKey);
                        groupIter.remove();
                    }
                }
                if (existingGroups.isEmpty()) {
                    this.consumerMap.remove(existingTopic);
                }
            }

            // 创建新的消费者
            for (KafkaSubscribeScriptInfo info : targetSubscribers.values()) {
                String topicName = info.getTopic();
                String group = MagicAPIConfigUtils.getKafkaSubscribeConfig(
                        this.dataSourceInfo.getKey(), info.getTopic(), "group", info.getGroup());
                tryCreateConsumer(info, topicName, group);
            }
        }
    }

    /**
     * 尝试创建Kafka消费者，先创建新容器再替换旧容器，防止创建失败导致订阅丢失
     */
    private void tryCreateConsumer(KafkaSubscribeScriptInfo subscribeScriptInfo, String topicName, String group) {
        try {
            Map<String, ConcurrentMessageListenerContainer> consumerTopicMap =
                    this.consumerMap.computeIfAbsent(topicName, k -> new ConcurrentHashMap<>());

            ConcurrentMessageListenerContainer newContainer;
            if (SpringUtils.isApplicationFullyStarted()) {
                newContainer = createContainer(subscribeScriptInfo, topicName, group);
            } else {
                newContainer = waitForStartupAndCreate(subscribeScriptInfo, topicName, group);
            }
            if (newContainer == null) {
                log.warn("创建消费者失败，保持现有消费者不变，topic: {}, group: {}", topicName, group);
                return;
            }

            ConcurrentMessageListenerContainer oldContainer = consumerTopicMap.put(group, newContainer);
            if (oldContainer != null) {
                closeContainer(oldContainer, topicName + ":" + group);
            }
        } catch (Exception e) {
            log.error("创建 Kafka 消费者失败，topic: {}, group: {}", topicName, group, e);
        }
    }

    /**
     * 等待应用启动完成后创建消费者容器，带超时保护。
     */
    private ConcurrentMessageListenerContainer waitForStartupAndCreate(
            KafkaSubscribeScriptInfo info, String topicName, String group) {
        long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(STARTUP_WAIT_TIMEOUT_SECONDS);
        try {
            while (!SpringUtils.isApplicationFullyStarted()) {
                if (System.currentTimeMillis() > deadline) {
                    log.error("等待应用启动超时，topic: {}, group: {}", topicName, group);
                    return null;
                }
                Thread.sleep(100);
            }
            ConcurrentMessageListenerContainer container = createContainer(info, topicName, group);
            log.info("Kafka 消费者已异步创建，topic: {}, group: {}", topicName, group);
            return container;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("等待应用启动时线程被中断", e);
            return null;
        } catch (Exception e) {
            log.error("异步创建 Kafka 消费者失败，topic: {}, group: {}", topicName, group, e);
            return null;
        }
    }

    /**
     * 创建Kafka消费者容器实例
     */
    private ConcurrentMessageListenerContainer createContainer(
            KafkaSubscribeScriptInfo subscribeScriptInfo, String topicName, String group) {
        String ackMode = subscribeScriptInfo.getAckMode();
        ackMode = MagicAPIConfigUtils.getKafkaSubscribeConfig(
                this.dataSourceInfo.getKey(), subscribeScriptInfo.getTopic(), "ackMode", ackMode);
        if (ackMode == null || ackMode.isEmpty()) {
            ackMode = "BATCH";
        }

        ContainerProperties containerProperties = new ContainerProperties(topicName);
        Consumer<ContainerProperties> ackModeSetter = ACK_MODE_MAP.get(ackMode.toUpperCase());
        if (ackModeSetter != null) {
            ackModeSetter.accept(containerProperties);
            if ("COUNT".equalsIgnoreCase(ackMode) && subscribeScriptInfo.getAckCount() != null) {
                containerProperties.setAckCount(subscribeScriptInfo.getAckCount());
            } else if ("TIME".equalsIgnoreCase(ackMode) && subscribeScriptInfo.getAckTime() != null) {
                containerProperties.setAckTime(subscribeScriptInfo.getAckTime());
            } else if ("COUNT_TIME".equalsIgnoreCase(ackMode)) {
                if (subscribeScriptInfo.getAckCount() != null) {
                    containerProperties.setAckCount(subscribeScriptInfo.getAckCount());
                }
                if (subscribeScriptInfo.getAckTime() != null) {
                    containerProperties.setAckTime(subscribeScriptInfo.getAckTime());
                }
            }
        }
        containerProperties.setMessageListener(
                new MessageListenerAdapter(this.dataSourceInfo.getKey(), kafkaConsumerService, subscribeScriptInfo,
                        topicName + ":" + group, consumingCountMap, this.magicDynamicKafkaDataSource,
                        () -> this.closing || this.closed));

        ConsumerFactory consumerFactory;
        try {
            consumerFactory = createConsumerFactory(subscribeScriptInfo);
        } catch (Exception e) {
            log.error("创建消费者工厂失败，topic: {}, group: {}", topicName, group, e);
            return null;
        }

        ConcurrentMessageListenerContainer newContainer = new ConcurrentMessageListenerContainer(consumerFactory, containerProperties);
        Integer concurrency = subscribeScriptInfo.getConcurrency();
        String concurrencyStr = MagicAPIConfigUtils.getKafkaSubscribeConfig(
                this.dataSourceInfo.getKey(), subscribeScriptInfo.getTopic(), "concurrency",
                String.valueOf(subscribeScriptInfo.getConcurrency()));
        try {
            concurrency = Integer.parseInt(concurrencyStr);
        } catch (Exception e) {
            //ignore
        }
        if (concurrency == null || concurrency <= 0) {
            concurrency = Runtime.getRuntime().availableProcessors();
        }
        newContainer.setConcurrency(concurrency);
        newContainer.start();
        return newContainer;
    }

    /**
     * 停止并销毁Kafka消费者容器
     */
    private void closeContainer(ConcurrentMessageListenerContainer container, Object containerKey) {
        if (container == null) {
            return;
        }
        try {
            if (container.isRunning()) {
                CompletableFuture<Void> stopFuture = CompletableFuture.runAsync(container::stop);
                stopFuture.get(10, TimeUnit.SECONDS);
            }
            container.destroy();
        } catch (TimeoutException e) {
            log.warn("停止 Kafka 消费者超时: {}, 强制销毁", containerKey);
            try { container.destroy(); } catch (Exception ignored) {}
        } catch (Exception e) {
            log.warn("关闭 Kafka 消费者失败: {}", containerKey, e);
            try { container.destroy(); } catch (Exception ignored) {}
        }
    }

    /**
     * 关闭所有Kafka消费者并清理资源
     */
    public void close() {
        if (this.closed || this.closing) {
            return;
        }
        this.closing = true;

        // 等待正在消费中的消息处理完成，再关闭连接
        waitForConsumingComplete();
        synchronized (this.consumerLock) {
            if (this.consumerMap.isEmpty()) {
                this.closed = true;
                this.closing = false;
                return;
            }

            List<Map.Entry<String, ConcurrentMessageListenerContainer>> allContainers = new ArrayList<>();
            for (Map.Entry<String, Map<String, ConcurrentMessageListenerContainer>> topicEntry : this.consumerMap.entrySet()) {
                Map<String, ConcurrentMessageListenerContainer> containerMap = topicEntry.getValue();
                if (containerMap != null) {
                    allContainers.addAll(containerMap.entrySet());
                }
            }

            for (Map.Entry<String, ConcurrentMessageListenerContainer> entry : allContainers) {
                closeContainer(entry.getValue(), entry.getKey());
            }
            this.consumerMap.clear();
            this.consumingCountMap.clear();
            this.closed = true;
            this.closing = false;
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
        return this.dataSourceInfo;
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
     * 判断当前消费者是否正在关闭
     */
    public boolean isClosing() {
        return this.closing;
    }

    /**
     * 判断当前消费者是否已经关闭
     */
    public boolean isClosed() {
        return this.closed;
    }



    /**
     * 取消订阅队列
     *
     * @param subscribeScriptInfo 订阅信息
     */
    public void unsubscribe(KafkaSubscribeInfo subscribeScriptInfo) {
        String topic = subscribeScriptInfo.getTopic();
        String group = MagicAPIConfigUtils.getKafkaSubscribeConfig(
                subscribeScriptInfo.getKey(), subscribeScriptInfo.getTopic(), "group", subscribeScriptInfo.getGroup());
        String consumeKey = topic + ":" + group;
        if (StringUtils.isBlank(topic) || StringUtils.isBlank(group)) {
            return;
        }

        // 等待该 consumer 正在处理的消息完成
        waitForConsumingComplete(consumeKey);

        synchronized (this.consumerLock) {
            if (this.closing || this.closed) {
                log.warn("Kafka 消费者正在关闭，跳过取消订阅，key: {}", key);
                return;
            }
            this.consumingCountMap.remove(consumeKey);
            Map<String, ConcurrentMessageListenerContainer> groupConsumerMap = this.consumerMap.get(topic);
            if (groupConsumerMap != null && !groupConsumerMap.isEmpty()) {
                ConcurrentMessageListenerContainer container = groupConsumerMap.remove(group);
                if (container != null) {
                    closeContainer(container, topic + ":" + group);
                }
            }
            if (groupConsumerMap == null || groupConsumerMap.isEmpty()) {
                this.consumerMap.remove(topic);
            }
        }
    }

    // 提取监听器为独立类，便于测试和复用
    private static class MessageListenerAdapter implements AcknowledgingMessageListener<Object, Object> {

        /** 数据源键值，用于标识特定的数据源配置 */
        private final String dataSourceKey;

        /** Kafka消费者服务实例，用于处理Kafka消息消费逻辑 */
        private final KafkaConsumerService kafkaConsumerService;

        /** 消费键 topic:group，用于 consumingCountMap 跟踪 */
        private final String consumeKey;

        /** 正在消费中的消息计数器，引用 KafkaConsumer 的 consumingCountMap */
        private final Map<String, AtomicInteger> consumingCountMap;

        /** 动态数据源引用，用于 DLT 发送 */
        private final MagicDynamicKafkaDataSource magicDynamicKafkaDataSource;

        /** 消费者是否正在关闭检查 */
        private final BooleanSupplier isShuttingDown;

        /**
         * 确认标志位
         */
        private final boolean acknowledgeFlag;

        /** 最大重试次数，当操作失败时允许的最大重试次数 */
        private final int maxRetryTimes;

        /** 重试退避时间（毫秒），每次重试之间的等待时间间隔 */
        private final long retryBackoffMs;

        /** 死信主题，为空时不启用 DLT */
        private final String deadLetterTopic;

        /**
         * 构造函数，初始化MessageListenerAdapter实例
         *
         * @param dataSourceKey 数据源键值，用于标识数据源
         * @param kafkaConsumerService Kafka消费者服务，用于处理Kafka消息消费逻辑
         * @param subscribeScriptInfo 订阅脚本信息，包含订阅相关的配置信息
         * @param consumeKey 消费键（topic:group），用于消费跟踪
         * @param consumingCountMap 消费计数映射引用
         */
        public MessageListenerAdapter(String dataSourceKey, KafkaConsumerService kafkaConsumerService,
                                       KafkaSubscribeScriptInfo subscribeScriptInfo,
                                       String consumeKey, Map<String, AtomicInteger> consumingCountMap,
                                       MagicDynamicKafkaDataSource magicDynamicKafkaDataSource,
                                       BooleanSupplier isShuttingDown) {
            this.dataSourceKey = dataSourceKey;
            this.kafkaConsumerService = kafkaConsumerService;
            this.consumeKey = consumeKey;
            this.consumingCountMap = consumingCountMap;
            this.magicDynamicKafkaDataSource = magicDynamicKafkaDataSource;
            this.isShuttingDown = isShuttingDown;
            String ackMode = subscribeScriptInfo.getAckMode();
            ackMode = MagicAPIConfigUtils.getKafkaSubscribeConfig(subscribeScriptInfo.getKey(), subscribeScriptInfo.getTopic(), "ackMode", ackMode);
            boolean tmpAcknowledgeFlag = "MANUAL".equalsIgnoreCase(ackMode) || "MANUAL_IMMEDIATE".equalsIgnoreCase(ackMode);
            String other = MagicAPIConfigUtils.getKafkaSubscribeConfig(subscribeScriptInfo.getKey(), subscribeScriptInfo.getTopic(), "other", subscribeScriptInfo.getOther());
            if (tmpAcknowledgeFlag && StringUtils.isNotBlank(other)) {
                Map<String, Object> otherProps = JsonUtils.readValue(other,
                        new TypeReference<Map<String, Object>>() {});
                Object enableAutoCommitObj = otherProps.get(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG);
                if (enableAutoCommitObj instanceof Boolean) {
                    tmpAcknowledgeFlag = !((Boolean) enableAutoCommitObj);
                }
            }
            acknowledgeFlag = tmpAcknowledgeFlag;
            int tmpMaxRetryTimes = DEFAULT_MAX_RETRY_TIMES;
            long tmpRetryBackoffMs = DEFAULT_RETRY_BACKOFF_MS;
            String tmpDeadLetterTopic = null;

            // 解析 other 配置中的重试参数
            if (StringUtils.isNotBlank(other)) {
                try {
                    Map<String, Object> otherConfig = JsonUtils.readValue(other,
                            new TypeReference<Map<String, Object>>() {});
                    // 获取最大重试次数
                    Object maxRetriesObj = otherConfig.get("maxRetryTimes");
                    if (maxRetriesObj != null) {
                        if (maxRetriesObj instanceof Number) {
                            tmpMaxRetryTimes = ((Number) maxRetriesObj).intValue();
                        } else {
                            tmpMaxRetryTimes = Integer.parseInt(maxRetriesObj.toString());
                        }
                    }

                    // 获取重试间隔
                    Object backoffObj = otherConfig.get("retryBackoffMs");
                    if (backoffObj != null) {
                        if (backoffObj instanceof Number) {
                            tmpRetryBackoffMs = ((Number) backoffObj).longValue();
                        } else {
                            tmpRetryBackoffMs = Long.parseLong(backoffObj.toString());
                        }
                    }

                    // 获取死信主题
                    Object dltObj = otherConfig.get("deadLetterTopic");
                    if (dltObj != null) {
                        tmpDeadLetterTopic = dltObj.toString();
                    }
                } catch (Exception e) {
                    log.warn("解析重试配置失败，使用默认值", e);
                }
            }

            this.maxRetryTimes = tmpMaxRetryTimes;
            this.retryBackoffMs = tmpRetryBackoffMs;
            this.deadLetterTopic = tmpDeadLetterTopic;
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
                    log.warn("解析消息头重试次数失败", e);
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
                throw new IllegalArgumentException("重试次数不能为负数");
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
            if (isShuttingDown.getAsBoolean()) {
                log.warn("消费者正在关闭，跳过消息处理，topic: {}", msg.topic());
                if (acknowledgeFlag) {
                    acknowledgment.nack(Duration.ofMillis(1000));
                }
                return;
            }
            AtomicInteger counter = consumingCountMap.computeIfAbsent(consumeKey, k -> new AtomicInteger());
            counter.incrementAndGet();
            try {
                log.debug("消费消息，topic: {}, offset: {}", msg.topic(), msg.offset());
                boolean success = kafkaConsumerService.consume(dataSourceKey, msg);
                if (success) {
                    if (acknowledgeFlag) {
                        acknowledgment.acknowledge();
                    }
                } else {
                    throw new RuntimeException("消息消费失败");
                }
            } catch (Exception e) {
                log.error("消费消息失败，topic: {}, partition: {}, offset: {}",
                        msg.topic(), msg.partition(), msg.offset(), e);
                if (acknowledgeFlag) {
                    int retryCount = getRetryCount(msg);
                    if (retryCount < maxRetryTimes) {
                        resetRetryCount(msg, retryCount + 1);
                        acknowledgment.nack(Duration.ofMillis(retryBackoffMs));
                    } else {
                        sendToDeadLetter(msg, e);
                        acknowledgment.acknowledge();
                    }
                } else {
                    // 非手动确认模式下，必须将异常重新抛出，让 Spring Kafka 的 ErrorHandler 处理
                    // 否则容器会认为消息消费成功并提交 offset，导致消息丢失
                    throw e instanceof RuntimeException ? (RuntimeException) e : new RuntimeException(e);
                }
            } finally {
                counter.decrementAndGet();
            }
        }

        /**
         * 将失败消息发送到死信主题。只有配置了 deadLetterTopic 时才生效。
         */
        private void sendToDeadLetter(ConsumerRecord<Object, Object> msg, Exception cause) {
            String dltTopic = this.deadLetterTopic;
            if (StringUtils.isBlank(dltTopic)) {
                return;
            }
            try {
                com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.kafka.producer.KafkaProducer jgProducer =
                        this.magicDynamicKafkaDataSource.getProducer(this.dataSourceKey);
                if (jgProducer != null) {
                    jgProducer.sendAsync(dltTopic, (String) msg.key(), msg.value());
                    log.info("已发送失败消息到 DLT，dltTopic: {}, originalTopic: {}, offset: {}",
                            dltTopic, msg.topic(), msg.offset());
                } else {
                    log.warn("未找到数据源 {} 的生产者，无法发送 DLT", this.dataSourceKey);
                }
            } catch (Exception dltEx) {
                log.error("发送消息到 DLT 失败，dltTopic: {}", dltTopic, dltEx);
            }
        }
    }

}
