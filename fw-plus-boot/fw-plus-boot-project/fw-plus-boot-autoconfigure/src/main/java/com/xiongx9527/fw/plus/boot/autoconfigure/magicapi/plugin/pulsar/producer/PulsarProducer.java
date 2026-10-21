package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.pulsar.producer;

import com.xiongx9527.fw.plus.common.i18n.I18nMessageResolver;
import com.alibaba.fastjson.JSON;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.common.MagicAPIConfigUtils;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.pulsar.common.BaseService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.pulsar.model.MagicDynamicPulsarDataSource;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.pulsar.model.PulsarDataSourceInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.pulsar.client.api.Producer;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.PulsarClientException;
import org.apache.pulsar.client.api.TypedMessageBuilder;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Pulsar 生产者实现
 *
 * @author zzy
 */
@Slf4j
public class PulsarProducer implements BaseService {

    // 唯一标识符，用于区分不同的 Pulsar 生产者实例
    private final String id;

    // 关键字，通常用于权限验证或识别特定的生产者
    private final String key;

    // 名称，提供一个可读的标识符，便于在日志或界面中显示
    private final String name;

    // Pulsar 数据源信息，包含了连接 Pulsar 所需的配置和参数
    private final PulsarDataSourceInfo dataSourceInfo;

    /**
     * Pulsar Producer 实例映射，用于存储和管理不同主题的 Producer 实例
     * 使用 ConcurrentHashMap 是为了线程安全，允许多个线程同时读写映射
     */
    private final Map<String,Producer<byte[]>> pulsarProducerMap = new ConcurrentHashMap<>();

    // 创建 Pulsar 客户端，用于与 Pulsar 消息队列建立连接和交互
    private final MagicDynamicPulsarDataSource magicDynamicPulsarDataSource ;

    /**
     * 构造方法，用于初始化 PulsarProducer 实例
     *
     * @param magicDynamicPulsarDataSource Pulsar 数据源实例，用于创建和管理 Pulsar 客户端
     * @param dataSourceInfo Pulsar 数据源信息，包含连接和配置详情
     * @throws PulsarClientException 当创建 Pulsar 客户端实例时出现错误，抛出此异常
     */
    public PulsarProducer(MagicDynamicPulsarDataSource magicDynamicPulsarDataSource, PulsarDataSourceInfo dataSourceInfo) throws PulsarClientException {
        this.magicDynamicPulsarDataSource = magicDynamicPulsarDataSource;
        this.id = dataSourceInfo.getId();
        this.name = dataSourceInfo.getName();
        this.key = dataSourceInfo.getKey();
        this.dataSourceInfo = dataSourceInfo;
    }

    /**
     * 构造函数，创建默认的Pulsar生产者实例
     *
     * @param magicDynamicPulsarDataSource Pulsar动态数据源，用于管理Pulsar连接
     * @param producer                     Pulsar生产者实例，用于实际的消息发送操作
     */
    public PulsarProducer(MagicDynamicPulsarDataSource magicDynamicPulsarDataSource,Producer<byte[]>  producer) {
        this.magicDynamicPulsarDataSource = magicDynamicPulsarDataSource;
        PulsarDataSourceInfo dataSourceInfo = new PulsarDataSourceInfo();
        dataSourceInfo.setId("");
        dataSourceInfo.setName(I18nMessageResolver.resolveKey("fw.boot.datasource.defaultName"));
        dataSourceInfo.setKey("default");
        this.id = dataSourceInfo.getId();
        this.key = dataSourceInfo.getKey();
        this.name = dataSourceInfo.getName();
        this.dataSourceInfo = dataSourceInfo;
        this.pulsarProducerMap.put(this.key, producer);
    }


    /**
     * 创建或获取与指定主题名称关联的Pulsar Producer实例
     * 该方法通过使用并发哈希映射（pulsarProducerMap）来缓存Producer实例，确保线程安全且高效的单次创建逻辑
     * 当指定的主题名称在映射中不存在时，会创建一个新的Producer实例并与其关联
     *
     * @param topicName Pulsar主题的名称，用于标识特定的Producer
     * @return 与指定主题名称关联的Producer实例如果创建失败，则抛出运行时异常
     */
    private Producer<byte[]> createPulsarProducer(String topicName) {
        // 校验主题名称有效性，如果主题名称为空或null则抛出非法参数异常
        if (topicName == null || topicName.isEmpty()) {
            throw new IllegalArgumentException("Topic name must not be null or empty");
        }

        // 校验数据源信息是否已初始化，如果未初始化则抛出非法状态异常
        if (dataSourceInfo == null) {
            throw new IllegalStateException("DataSourceInfo has not been initialized.");
        }

        // 使用 computeIfAbsent 实现线程安全且高效的单次创建逻辑
        return pulsarProducerMap.computeIfAbsent(topicName, tn -> {
            try {
                String tenant = dataSourceInfo.getTenant();
                tenant = MagicAPIConfigUtils.getPulsarConfig(dataSourceInfo.getKey(), "tenant", tenant);
                String namespace = dataSourceInfo.getNamespace();
                namespace = MagicAPIConfigUtils.getPulsarConfig(dataSourceInfo.getKey(), "namespace", namespace);
                if (tenant == null || namespace == null) {
                    throw new IllegalStateException("Tenant or Namespace in DataSourceInfo is null.");
                }
                String newTopicName = createTopicName(tenant, namespace, tn);
                // 获取客户端并创建Producer
                PulsarClient client = magicDynamicPulsarDataSource.getPulsarClient(dataSourceInfo.getKey());
                if (client == null) {
                    throw new IllegalStateException("Failed to get PulsarClient from data source.");
                }
                Producer<byte[]> pulsarProducer = client.newProducer()
                        .topic(newTopicName)
                        .create();
                // 记录Producer实例化成功的日志信息
                String sourceName = this.name; // 假设 'name' 是当前类的一个字段
                log.info("[Pulsar] Producer实例化成功，sourceName is {}, topic is {}", sourceName, tn);
                return pulsarProducer;
            } catch (PulsarClientException e) {
                // 抛出运行时异常以便 computeIfAbsent 能感知到失败
                throw new RuntimeException("Failed to create Pulsar producer for topic: " + tn, e);
            }
        });
    }

    /**
     * 创建消息构建器
     *
     * @param topic 消息主题
     * @param partition 分区标识，用于消息顺序性控制
     * @param key 消息键，用于消息路由和去重
     * @param delay 延迟时间
     * @param unit 延迟时间单位
     * @param msg 消息内容对象
     * @return 配置好的消息构建器实例
     */
    private TypedMessageBuilder createMessageBuilder(String topic, Integer partition, String key, Long delay, TimeUnit unit,Object msg) {
        // 将消息对象序列化为字节数组
        byte[] messageBytes = JSON.toJSONBytes(msg);
        TypedMessageBuilder messageBuilder = createPulsarProducer(topic).newMessage();

        // 设置消息键
        if (StringUtils.isNotBlank(key)) {
            messageBuilder = messageBuilder.key(key);
        }

        // 设置分区顺序键
        if (partition != null) {
            messageBuilder = messageBuilder.orderingKey(String.valueOf(partition).getBytes(StandardCharsets.UTF_8));
        }

        // 设置消息延迟发送时间
        if (delay != null && unit != null) {
            messageBuilder = messageBuilder.deliverAfter(delay, unit);
        }
        // 设置消息内容并返回构建器
        return messageBuilder.value(messageBytes);
    }

    /**
     * 发送消息到指定的主题
     *
     * @param topic 消息要发送到的主题名称
     * @param partition 指定的分区编号，可为null表示使用默认分区策略
     * @param key 消息的键，用于分区路由，可为null
     * @param delay 延迟时间，可为null表示不延迟
     * @param unit 延迟时间的单位，当delay不为null时有效
     * @param msg 要发送的消息对象
     * @param async 是否异步发送，true表示异步发送，false表示同步发送
     * @return 发送成功返回true，发送失败返回false
     */
    public boolean send(String topic,Integer partition,String key,Long delay, TimeUnit unit,Object msg , boolean async) {
        try {
            TypedMessageBuilder builder = createMessageBuilder(topic, partition, key, delay, unit, msg);
            if (async) {
                builder.sendAsync().whenComplete((messageId, throwable) -> {
                    if (throwable != null) {
                        log.error("异步发送消息失败：topic={}, partition={}, key={}",
                                topic, partition, key, throwable);
                    }
                });
            } else {
                builder.send();
            }
            log.debug("发送消息成功：topic={}, partition={}, key={}, delay={}, async={}",
                    topic, partition, key, delay, async);
            return true;
        } catch (Exception e) {
            log.error("发送消息失败：topic={}, partition={}, key={}, delay={}, async={}",
                    topic, partition, key, delay, async, e);
            return false;
        }
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
        return send(topic, null, null, delay, unit, msg, true);
    }

    /**
     * 异步发送消息到指定主题。
     *
     * @param topic 消息要发送的主题名称
     * @param msg   要发送的消息对象
     * @return 发送是否成功
     */
    public boolean sendAsync(String topic,Object msg) {
        return send(topic, null, null, null, null, msg, true);
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
        return send(topic, null, null, delay, unit, msg, false);
    }

    /**
     * 发送消息到指定主题。
     *
     * @param topic 消息要发送的主题名称
     * @param msg   要发送的消息对象
     * @return 发送是否成功
     */
    public boolean send(String topic,Object msg) {
        return send(topic, null, null, null, null, msg, false);
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
        return send(topic, null, key, delay, unit, msg, true);
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
        return send(topic, null, key, null, null, msg, true);
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
        return send(topic, null, key, delay, unit, msg, false);
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
        return send(topic, null, key, null, null, msg, false);
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
        return send(topic, partition, key, delay, unit, msg, true);
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
        return send(topic, partition, key, null, null, msg, true);
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
        return send(topic, partition, key, delay, unit, msg, false);
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
        return send(topic, partition, key, null, null, msg, false);
    }

    /**
     * 关闭所有Pulsar生产者连接
     *
     * 本方法遍历pulsarProducerMap中的所有生产者对象，并尝试关闭它们的连接
     * 如果在关闭过程中遇到任何错误，将会记录错误信息，但不会抛出异常
     */
    public void close() {
        if (pulsarProducerMap == null || pulsarProducerMap.isEmpty()) {
            return;
        }
        synchronized (this) {
            try {
                for (Map.Entry<String, Producer<byte[]>> entry : pulsarProducerMap.entrySet()) {
                    String producerKey = entry.getKey();
                    Producer<byte[]> producer = entry.getValue();
                    try {
                        CompletableFuture.runAsync(() -> closeProducerSafely(producerKey, producer))
                                .get(5, TimeUnit.SECONDS);
                    } catch (TimeoutException e) {
                        log.warn("关闭Pulsar生产者 [{}] 超时，已跳过", producerKey);
                    } catch (Exception e) {
                        log.warn("关闭Pulsar生产者 [{}] 异常", producerKey, e);
                    }
                }
            } finally {
                pulsarProducerMap.clear();
            }
        }
    }

    /**
     * 安全地关闭指定的生产者，并处理可能出现的异常
     *
     * @param producerKey 生产者的标识符（用于日志记录）
     * @param producer    要关闭的生产者对象
     */
    private void closeProducerSafely(String producerKey, Producer<byte[]> producer) {
        try {
            if (producer != null) {
                producer.close();
            }
        }catch (Throwable ee) {
            log.error("Unexpected error while closing Pulsar producer [{}]", producerKey, ee);
        }
    }


    /**
     * 获取ID标识符
     *
     * @return 返回当前对象的ID字符串
     */
    public String getId() {
        return id;
    }

    /**
     * 获取名称信息
     *
     * @return 返回当前对象的名称字符串
     */
    public String getName() {
        return name;
    }

    /**
     * 获取键值信息
     *
     * @return 返回当前对象的键字符串
     */
    public String getKey() {
        return key;
    }

    /**
     * 获取数据源信息
     *
     * @return 返回Pulsar数据源信息对象
     */
    public PulsarDataSourceInfo getDataSourceInfo() {
        return dataSourceInfo;
    }

}
