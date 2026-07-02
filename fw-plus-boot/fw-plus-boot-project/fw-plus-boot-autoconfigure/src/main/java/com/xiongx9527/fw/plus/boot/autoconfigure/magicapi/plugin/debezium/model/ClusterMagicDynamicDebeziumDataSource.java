package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.debezium.model;

import org.apache.commons.collections4.MapUtils;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ClusterMagicDynamicDebeziumDataSource extends MagicDynamicDebeziumDataSource implements ApplicationContextAware {

    /**
     * Redis模板，用于操作Redis存储，包括记录已发送的消息ID以防止重复发送
     */
    private StringRedisTemplate stringRedisTemplate;
    
    // Redis中用于存储已发送消息ID的集合键前缀
    private static final String SENT_MESSAGES_KEY_PREFIX = "debezium-sent-messages-";
    
    // 消息ID在Redis中的过期时间（小时）
    private static final int MESSAGE_ID_EXPIRE_HOURS = 1;
    
    // 节点ID，用于标识当前实例
    private final String nodeId = UUID.randomUUID().toString();

    /**
     * 处理事件发送逻辑，通过Redis实现集群环境下的消息去重机制
     *
     * @param kafkaTemplate Kafka模板，用于发送消息到Kafka
     * @param messageId 消息唯一标识符，用于去重判断
     * @param record 要发送的Kafka记录
     */
    @Override
    void onEventSent(KafkaTemplate kafkaTemplate, String messageId,ProducerRecord<String, Object> record) {
        // 如果没有消息ID或没有Redis模板，则直接发送消息
        if (messageId == null || stringRedisTemplate == null) {
            log.debug("Message ID is null or Redis template is null, sending message directly. Message ID: {}", messageId);
            kafkaTemplate.send(record);
            return;
        }
        
        log.debug("Processing message with ID: {}", messageId);
        
        // 使用Redis防止消息重复发送，采用原子操作避免两次网络请求
        String sentMessagesKey = SENT_MESSAGES_KEY_PREFIX + ":" + messageId;
        try {
            // 随机等待0到100毫秒，避免惊群效应，提高系统可靠性
            try {
                // 在尝试获取锁之前添加随机等待
                long randomDelay = ThreadLocalRandom.current().nextLong(100); // 0-100毫秒随机等待
                log.debug("Random delay {} ms before Redis operation for message ID: {}", randomDelay, messageId);
                Thread.sleep(randomDelay);
            } catch (InterruptedException e) {
                log.warn("Thread interrupted during random delay for message ID: {}", messageId);
                // 恢复中断状态
                Thread.currentThread().interrupt();
            }
            
            // 使用SET命令的NX选项实现原子性的"不存在则设置"操作
            // NX: 只有键不存在时才设置
            // EX: 设置过期时间（单位：小时）
            Boolean notExists = stringRedisTemplate.opsForValue().setIfAbsent(
                sentMessagesKey, 
                nodeId, 
                MESSAGE_ID_EXPIRE_HOURS, 
                TimeUnit.HOURS
            );
            
            log.debug("Redis setIfAbsent result for message ID {}: {}", messageId, notExists);
            
            // 如果键不存在（notExists为true），则发送消息
            if (Boolean.TRUE.equals(notExists)) {
                log.debug("Sending message with ID: {} to Kafka topic: {}", messageId, record.topic());
                kafkaTemplate.send(record);
                log.debug("Message with ID: {} sent successfully", messageId);
            } else {
                log.debug("Message with ID: {} already processed by node, skipping", messageId);
            }
        } catch (Exception redisException) {
            log.error("Redis operation failed for message ID: {}, sending message directly to ensure no message loss", messageId, redisException);
            // Redis操作异常时，为保证消息不丢失，仍然发送消息
            kafkaTemplate.send(record);
        }
    }

    /**
     * 设置应用程序上下文，并从中获取RedisTemplate实例
     *
     * @param applicationContext 应用程序上下文对象
     * @throws BeansException 当Bean处理出现异常时抛出
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        if (this.stringRedisTemplate != null) {
            return ;
        }
        // 从应用上下文中获取所有RedisTemplate类型的Bean实例
        Map<String, StringRedisTemplate> map = MapUtils.emptyIfNull(applicationContext.getBeansOfType(StringRedisTemplate.class));
        for (Map.Entry<String, StringRedisTemplate> entry : map.entrySet()) {
            // 跳过StringRedisTemplate类型的实例，只获取RedisTemplate实例
            if (entry.getValue() instanceof StringRedisTemplate) {
                this.stringRedisTemplate = entry.getValue();
                break;
            }
        }
        // 如果未找到合适的RedisTemplate实例，则抛出运行时异常
        if (this.stringRedisTemplate == null) {
            throw new RuntimeException("未找到RedisTemplate实例");
        }
    }



}
