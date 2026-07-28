package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.kafka.producer;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.common.MagicAPIConfigUtils;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.kafka.model.KafkaDataSourceInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.ProducerListener;
import org.springframework.kafka.support.SendResult;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.ssssssss.magicapi.utils.JsonUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
public class KafkaProducer {

    /**
     * 唯一标识符
     */
    private final String id;

    /**
     * 关键字，用于特定的处理或识别
     */
    private final String key;

    /**
     * 名称，通常用于界面显示或日志记录
     */
    private final String name;

    /**
     * 数据源信息，存储连接Kafka所需的具体配置
     */
    private final KafkaDataSourceInfo dataSourceInfo;

    /**
     * 生产者实例
     */
    private final KafkaTemplate producer;

    /**
     * 构造函数用于创建KafkaProducer实例
     * @param dataSourceInfo Kafka数据源信息，包含连接Kafka所需的配置和元数据
     */
    public KafkaProducer(KafkaDataSourceInfo dataSourceInfo) {
        this.id = dataSourceInfo.getId();
        this.name = dataSourceInfo.getName();
        this.key = dataSourceInfo.getKey();
        this.dataSourceInfo = dataSourceInfo;
        // 根据数据源信息创建Kafka生产者实例
        this.producer = createProducer();
    }

    /**
     * 构造函数，用于创建默认的Kafka生产者实例
     *
     * @param producer             Kafka模板实例，用于实际的消息发送操作
     */
    public KafkaProducer(KafkaTemplate producer) {
        this.dataSourceInfo = new KafkaDataSourceInfo();
        this.dataSourceInfo.setId("");
        this.dataSourceInfo.setName("默认数据源");
        this.dataSourceInfo.setKey("default");
        this.id = "";
        this.name = "默认数据源";
        this.key = "default";
        this.producer = producer;
    }

    /**
     * 获取Kafka数据源配置信息
     *
     * @return Kafka数据源配置信息
     */
    public KafkaDataSourceInfo getDataSourceInfo() {
        return this.dataSourceInfo;
    }

    /**
     * 获取KafkaTemplate实例
     *
     * @return KafkaTemplate实例
     */
    public KafkaTemplate getProducer() {
        return this.producer;
    }

    /**
     * 创建Kafka生产者工厂
     * 该方法用于构建一个ProducerFactory实例，该实例可用于创建Kafka生产者
     * 主要配置了生产者所需的各项参数，包括服务器地址、序列化方式等
     * 如果指定了额外的配置信息（other），则会将这些信息合并到配置中
     *
     * @return ProducerFactory<Object, Object> 实例化的生产者工厂
     */
    private ProducerFactory<Object, Object> createProducerFactory() {
        // 初始容量设为预计大小，避免频繁扩容
        Map<String, Object> props = new HashMap<>(32);
        // 基础配置
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, "3");
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true");

        // 关键：大幅增加消息大小限制
        props.put(ProducerConfig.MAX_REQUEST_SIZE_CONFIG, 4194304);     // 增加到 4MB
        props.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 67108864);       // 增加到 64MB
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, 524288);            // 增加到 512KB

        // 性能和压缩配置
        props.put(ProducerConfig.LINGER_MS_CONFIG, "50");               // 减少到 50ms
        props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "gzip");      // 使用 gzip 获得更高压缩比
        props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, "60000");   // 增加到 60秒
        props.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, "120000"); // 增加到 120秒
        String configuredProperties = MagicAPIConfigUtils.getKafkaConfig(dataSourceInfo.getKey(), "properties", null);
        props.putAll(MagicAPIConfigUtils.mergeProperties(dataSourceInfo.getProperties(), configuredProperties));
        String servers = dataSourceInfo.getServers();
        servers = MagicAPIConfigUtils.getKafkaConfig(dataSourceInfo.getKey(), "servers", servers);
        // 添加服务器地址配置
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, servers);
        // 序列化方式配置
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        // 创建并返回生产者工厂实例
        DefaultKafkaProducerFactory<Object, Object> producerFactory = new DefaultKafkaProducerFactory<>(props);
        return producerFactory;
    }

    /**
     * 创建KafkaTemplate实例
     * 该方法通过生产者工厂创建一个KafkaTemplate实例，并配置生产者监听器
     * 监听器用于记录消息发送成功或失败的状态信息
     *
     * @return 配置好的KafkaTemplate实例
     */
    private KafkaTemplate createProducer() {
        KafkaTemplate kafkaTemplate = new KafkaTemplate(createProducerFactory());
        kafkaTemplate.setProducerListener(new ProducerListener<Object, Object>() {
            @Override
            public void onSuccess(ProducerRecord<Object, Object> record, RecordMetadata metadata) {
                log.info("消息发送成功，Topic: {}, Partition: {}, Offset: {}",
                        metadata.topic(), metadata.partition(), metadata.offset());
            }
            @Override
            public void onError(ProducerRecord<Object, Object> record, RecordMetadata metadata, Exception exception) {
                if (metadata != null) {
                    log.error("消息发送失败，Topic: {}, Partition: {}, Offset: {}, 异常信息: {}",
                            metadata.topic(), metadata.partition(), metadata.offset(), exception.getMessage());
                } else {
                    log.error("消息发送失败，Topic: {}, 异常信息: {}",
                            record != null ? record.topic() : "unknown", exception.getMessage());
                }
            }
        });
        // 返回配置好的KafkaTemplate实例
        return kafkaTemplate;
    }

    /**
     * 发送消息到Kafka主题
     * 支持同步和异步两种发送方式，可根据需要选择是否等待发送结果
     *
     * @param topic     目标主题名称，不能为空
     * @param partition 分区编号，可以为null（由Kafka决定）
     * @param key       消息键，可以为null
     * @param msg       消息内容
     * @param async     是否异步发送，true为异步，false为同步
     * @return 异步发送始终返回true；同步发送根据结果返回true或false
     * @throws IllegalArgumentException 如果topic为空则抛出此异常
     */
    public boolean send(String topic, Integer partition, String key, Object msg, boolean async) {
        if (StringUtils.isBlank(topic)) {
            throw new IllegalArgumentException("Topic 不能为空");
        }
        ProducerRecord<Object, Object> record;
        if (partition == null && StringUtils.isBlank(key)) {
            record = new ProducerRecord<>(topic, msg);
        } else if (partition != null && StringUtils.isNotBlank(key)) {
            record = new ProducerRecord<>(topic, partition, key, msg);
        } else if (partition != null) {
            record = new ProducerRecord<>(topic, partition, msg);
        } else {
            record = new ProducerRecord<>(topic, key, msg);
        }
        if (async) {
            producer.send(record).whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("异步消息发送失败，topic: {}, partition: {}, key: {}",
                            record.topic(), record.partition(), record.key(), ex);
                    return;
                }
                SendResult sendResult = (SendResult) result;
                log.debug("异步消息发送成功，topic: {}, partition: {}, offset: {}",
                        sendResult.getRecordMetadata().topic(),
                        sendResult.getRecordMetadata().partition(),
                        sendResult.getRecordMetadata().offset());
            });
            return true;
        }
        try {
            SendResult result = (SendResult) producer.send(record).get();
            log.debug("消息发送成功，topic: {}, partition: {}, offset: {}",
                    result.getRecordMetadata().topic(), result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
            return true;
        } catch (Exception e) {
            log.error("消息发送失败，topic: {}, partition: {}, key: {}, value: {}",
                    record.topic(), record.partition(), record.key(), record.value(), e);
            return false;
        }
    }

    /**
     * 异步发送消息到指定主题
     *
     * @param topic 目标主题
     * @param msg   消息内容
     * @return 始终返回true
     */
    public boolean sendAsync(String topic, Object msg) {
        return send(topic, null, null, msg, true);
    }

    /**
     * 异步发送带键的消息到指定主题
     *
     * @param topic 目标主题
     * @param key   消息键
     * @param msg   消息内容
     * @return 始终返回true
     */
    public boolean sendAsync(String topic, String key, Object msg) {
        return send(topic, null, key, msg, true);
    }

    /**
     * 异步发送消息到指定主题和分区
     *
     * @param topic     目标主题
     * @param partition 分区编号
     * @param key       消息键
     * @param msg       消息内容
     * @return 始终返回true
     */
    public boolean sendAsync(String topic, Integer partition, String key, Object msg) {
        return send(topic, partition, key, msg, true);
    }

    /**
     * 同步发送消息到指定主题
     *
     * @param topic 目标主题
     * @param msg   消息内容
     * @return 发送成功返回true，失败返回false
     */
    public boolean send(String topic, Object msg) {
        return send(topic, null, null, msg, false);
    }

    /**
     * 同步发送带键的消息到指定主题
     *
     * @param topic 目标主题
     * @param key   消息键
     * @param msg   消息内容
     * @return 发送成功返回true，失败返回false
     */
    public boolean send(String topic, String key, Object msg) {
        return send(topic, null, key, msg, false);
    }

    /**
     * 同步发送消息到指定主题和分区
     *
     * @param topic     目标主题
     * @param partition 分区编号
     * @param key       消息键
     * @param msg       消息内容
     * @return 发送成功返回true，失败返回false
     */
    public boolean send(String topic, Integer partition, String key, Object msg) {
        return send(topic, partition, key, msg, false);
    }


    /**
     * 关闭Kafka生产者资源
     * 释放KafkaTemplate及其关联的生产者连接
     */
    public void close() {
        if (producer != null) {
            try {
                CompletableFuture.runAsync(() -> producer.destroy())
                        .get(5, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                log.warn("关闭Kafka生产者超时，已跳过");
            } catch (Exception e) {
                log.warn("关闭Kafka生产者资源过程中发生异常", e);
            }
        }
    }

    /**
     * 获取ID标识符
     *
     * @return 返回当前对象的ID字符串标识
     */
    /**
     * 获取唯一标识符
     *
     * @return 返回当前对象的ID字符串标识
     */
    public String getId() {
        return id;
    }

    /**
     * 获取名称
     *
     * @return 返回当前对象的名称字符串
     */
    public String getName() {
        return name;
    }

    /**
     * 获取关键字
     *
     * @return 返回当前对象的键值字符串
     */
    public String getKey() {
        return key;
    }

}
