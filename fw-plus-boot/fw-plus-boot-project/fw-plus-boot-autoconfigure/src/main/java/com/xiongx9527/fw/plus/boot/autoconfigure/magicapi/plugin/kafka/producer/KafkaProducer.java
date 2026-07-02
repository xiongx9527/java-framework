package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.kafka.producer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.kafka.model.KafkaDataSourceInfo;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.support.SendResult;
import org.springframework.util.concurrent.ListenableFuture;
import org.ssssssss.magicapi.utils.JsonUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.ProducerListener;
import org.springframework.kafka.support.serializer.JsonSerializer;
import java.beans.Transient;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

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
        KafkaDataSourceInfo dataSourceInfo = new KafkaDataSourceInfo();
        dataSourceInfo.setId("");
        dataSourceInfo.setName("默认数据源");
        dataSourceInfo.setKey("default");
        this.id = dataSourceInfo.getId();
        this.name = dataSourceInfo.getName();
        this.key = dataSourceInfo.getKey();
        this.dataSourceInfo = dataSourceInfo;
        this.producer = producer;
    }

    /**
     * 创建Kafka生产者工厂
     * 该方法用于构建一个ProducerFactory实例，该实例可用于创建Kafka生产者
     * 主要配置了生产者所需的各项参数，包括服务器地址、序列化方式等
     * 如果指定了额外的配置信息（other），则会将这些信息合并到配置中
     *
     * @return ProducerFactory<Object, Object> 实例化的生产者工厂
     */
    @Transient
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
        String other = dataSourceInfo.getOther();
        other = SpringUtils.getAppConfig(String.format("magic.api.kafka.%s.other",dataSourceInfo.getKey()),other);
        // 处理额外配置信息
        if (StringUtils.isNotBlank(other)) {
            try {
                Map<String, Object> otherProps = JsonUtils.readValue(other,
                        new TypeReference<Map<String, Object>>() {});
                props.putAll(otherProps);
            } catch (Throwable e) {
                // 记录日志并抛出异常或返回空配置
                log.error("Failed to parse other properties: {}", e.getMessage());
            }
        }
        String servers = dataSourceInfo.getServers();
        servers = SpringUtils.getAppConfig(String.format("magic.api.kafka.%s.servers",dataSourceInfo.getKey()),servers);
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
     * 创建Kafka生产者实例
     * 该方法配置并返回一个KafkaTemplate对象，用于向Kafka主题发送消息
     * 如果kafkaProducerService不为空，还将设置一个ProducerListener来监听消息发送的成败
     *
     * @return KafkaTemplate 实例，用于生产消息到Kafka
     */
    @Transient
    private KafkaTemplate createProducer() {
        // 创建KafkaTemplate实例，使用特定的ProducerFactory
        KafkaTemplate kafkaTemplate = new KafkaTemplate(createProducerFactory());
        kafkaTemplate.setProducerListener(new ProducerListener(){
            public void onSuccess(ProducerRecord record, RecordMetadata metadata) {
                log.info("消息发送成功，Topic: {}, Partition: {}, Offset: {}",
                        metadata.topic(), metadata.partition(), metadata.offset());
            }
            public void onError(ProducerRecord record, RecordMetadata metadata, Exception exception) {
                log.error("消息发送失败，Topic: {}, Partition: {}, Offset: {}, 异常信息: {}",
                        metadata.topic(), metadata.partition(), metadata.offset(), exception.getMessage());
            }
        });
        // 返回配置好的KafkaTemplate实例
        return kafkaTemplate;
    }

    /**
     * 发送消息到指定的主题
     *
     * @param topic 消息要发送到的主题名称，不能为空
     * @param partition 指定分区编号，可为空，为空时表示让Kafka自动分配分区
     * @param key 消息键，用于分区路由，可为空
     * @param msg 要发送的消息内容
     * @param async 是否异步发送，true表示异步发送不等待结果，false表示同步发送并等待发送结果
     * @return 发送成功返回true，发送失败返回false（异步模式总是返回true）
     * @throws IllegalArgumentException 当topic为空时抛出异常
     */
    public boolean send(String topic, Integer partition, String key, Object msg, boolean async) {
        if (StringUtils.isBlank(topic)) {
            throw new IllegalArgumentException("Topic cannot be blank");
        }

        // 根据不同的参数组合创建ProducerRecord对象
        ProducerRecord<Object, Object> record;
        if (partition == null && StringUtils.isBlank(key)) {
            record = new ProducerRecord<>(topic, msg);
        } else if (partition != null && StringUtils.isNotBlank(key)) {
            record = new ProducerRecord<>(topic, partition, key, msg);
        } else if (partition != null && StringUtils.isBlank(key)) {
            record = new ProducerRecord<>(topic, partition, msg);
        } else {
            record = new ProducerRecord<>(topic, key, msg);
        }

        // 发送消息并获取发送结果的Future对象
        CompletableFuture<SendResult> future = producer.send(record);

        // 同步发送模式：等待发送结果并处理成功或失败的情况
        if (!async) {
            try {
                SendResult result = future.get();
                RecordMetadata metadata = result.getRecordMetadata();
                log.debug("Message sent successfully, topic: {}, partition: {}, key: {}, offset: {}, timestamp: {}",
                        record.topic(), metadata.partition(), record.key(), metadata.offset(), metadata.timestamp());
                return true;
            } catch (Exception e) {
                log.error("Failed to send message, topic: {}, partition: {}, key: {}, value: {}",
                        record.topic(), record.partition(), record.key(), record.value(), e);
                return false;
            }
        }
        return true;
    }

    /**
     * 异步发送消息到指定主题
     * @param topic 消息主题
     * @param msg 要发送的消息对象
     * @return 发送是否成功
     */
    public boolean sendAsync(String topic,Object msg) {
        return send(topic, null, null, msg, true);
    }

    /**
     * 异步发送消息到指定主题，支持指定消息键
     * @param topic 消息主题
     * @param key 消息键，用于消息分区路由
     * @param msg 要发送的消息对象
     * @return 发送是否成功
     */
    public boolean sendAsync(String topic,String key,Object msg) {
        return send(topic, null, key, msg, true);
    }

    /**
     * 异步发送消息到指定主题，支持指定分区和消息键
     * @param topic 消息主题
     * @param partition 分区编号，null表示使用默认分区策略
     * @param key 消息键，用于消息分区路由
     * @param msg 要发送的消息对象
     * @return 发送是否成功
     */
    public boolean sendAsync(String topic,Integer partition,String key,Object msg) {
        return send(topic, partition, key, msg, true);
    }

    /**
     * 同步发送消息到指定主题
     * @param topic 消息主题
     * @param msg 要发送的消息对象
     * @return 发送是否成功
     */
    public boolean send(String topic,Object msg) {
        return send(topic, null, null, msg, false);
    }

    /**
     * 同步发送消息到指定主题，支持指定消息键
     * @param topic 消息主题
     * @param key 消息键，用于消息分区路由
     * @param msg 要发送的消息对象
     * @return 发送是否成功
     */
    public boolean send(String topic,String key,Object msg) {
        return send(topic, null, key, msg, false);
    }

    /**
     * 同步发送消息到指定主题，支持指定分区和消息键
     * @param topic 消息主题
     * @param partition 分区编号，null表示使用默认分区策略
     * @param key 消息键，用于消息分区路由
     * @param msg 要发送的消息对象
     * @return 发送是否成功
     */
    public boolean send(String topic,Integer partition,String key,Object msg) {
        return send(topic, partition, key, msg, false);
    }


    /**
     * 关闭并销毁生产者资源
     *
     * 此方法用于释放生产者占用的系统资源，通过调用producer.destroy()来执行实际的清理操作。
     * 该方法被标记为@Transient，表示这是一个临时操作，不会持久化到数据库中。
     */
    @Transient
    public void close() {
        if (producer != null) {
            try {
                producer.destroy();
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
    @Transient
    public String getId() {
        return id;
    }

    /**
     * 获取名称信息
     *
     * @return 返回当前对象的名称字符串
     */
    @Transient
    public String getName() {
        return name;
    }

    /**
     * 获取键值信息
     *
     * @return 返回当前对象的键值字符串
     */
    @Transient
    public String getKey() {
        return key;
    }

}
