package com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.debezium.model;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ClusterMagicDynamicDebeziumDataSource extends MagicDynamicDebeziumDataSource implements ApplicationContextAware {

    /**
     * Spring Redis模板实例，用于操作Redis实现消息去重功能
     */
    private StringRedisTemplate stringRedisTemplate;

    /**
     * Redis中存储已发送消息的键前缀
     */
    private static final String SENT_MESSAGES_KEY_PREFIX = "debezium-sent-messages";

    /**
     * 消息ID在Redis中的过期时间（分钟）
     */
    private static final int MESSAGE_ID_EXPIRE_MINUTES = 5;

    /**
     * 当前节点的唯一标识符，用于标记消息由哪个节点发送
     */
    private final String nodeId = UUID.randomUUID().toString();

    /**
     * 构建Redis中存储已发送消息的完整键名。
     * <p>
     * 键名格式为：debezium-sent-messages:{debeziumPrefix}:{messageId}
     * </p>
     *
     * @param messageId 消息唯一标识符
     * @return 完整的Redis键名
     */
    private String buildSentMessagesKey(String messageId) {
        return SENT_MESSAGES_KEY_PREFIX + ":" + getDebeziumPrefix() + ":" + messageId;
    }

    /**
     * 处理Debezium事件发送，使用Redis实现分布式环境下的消息去重功能。
     * <p>
     * 该方法在事件发送时被调用，通过Redis的setIfAbsent操作确保同一消息在集群中只被发送一次。
     * 如果messageId为空或StringRedisTemplate未初始化，则直接发送消息不进行去重。
     * 使用随机延迟减少Redis并发冲突，若Redis操作失败则降级为直接发送以确保消息不丢失。
     * </p>
     *
     * @param kafkaTemplate Kafka模板对象，用于发送消息到Kafka主题
     * @param messageId 消息唯一标识符，用于去重判断
     * @param record Kafka生产者记录，包含要发送的消息内容
     */
    @Override
    void onEventSent(KafkaTemplate kafkaTemplate, String messageId, ProducerRecord<String, Object> record) {
        // 前置条件检查：消息ID为空或Redis模板未初始化时直接发送
        if (messageId == null || this.stringRedisTemplate == null) {
            log.debug("Message ID is null or Redis template is null, sending message directly. Message ID: {}", messageId);
            kafkaTemplate.send(record);
            return;
        }

        log.debug("Processing message with ID: {}", messageId);
        String sentMessagesKey = buildSentMessagesKey(messageId);

        try {
            // 添加随机延迟以减少Redis并发冲突
            try {
                long randomDelay = ThreadLocalRandom.current().nextLong(100);
                log.debug("Random delay {} ms before Redis operation for message ID: {}", randomDelay, messageId);
                Thread.sleep(randomDelay);
            } catch (InterruptedException e) {
                log.warn("Thread interrupted during random delay for message ID: {}", messageId);
                Thread.currentThread().interrupt();
            }

            // 使用Redis setIfAbsent实现分布式锁，确保消息只被发送一次
            Boolean isNewMessage = this.stringRedisTemplate.opsForValue().setIfAbsent(
                    sentMessagesKey,
                    nodeId,
                    MESSAGE_ID_EXPIRE_MINUTES,
                    TimeUnit.MINUTES
            );
            log.debug("Redis setIfAbsent result for message ID {}: {}", messageId, isNewMessage);

            // 根据Redis操作结果决定是否发送消息
            if (Boolean.TRUE.equals(isNewMessage)) {
                log.debug("Sending message with ID: {} to Kafka topic: {}", messageId, record.topic());
                kafkaTemplate.send(record);
                log.debug("Message with ID: {} sent successfully", messageId);
            } else {
                log.debug("Message with ID: {} already processed by node, skipping", messageId);
            }
        } catch (Exception redisException) {
            // Redis操作失败时的降级处理：直接发送消息确保不丢失
            log.error("Redis operation failed for message ID: {}, sending message directly to ensure no message loss", messageId, redisException);
            kafkaTemplate.send(record);
        }
    }

    /**
     * 设置Spring应用上下文，并从容器中获取StringRedisTemplate实例用于消息去重功能。
     * <p>
     * 该方法实现了ApplicationContextAware接口，在Spring容器初始化时自动调用。
     * 如果已存在StringRedisTemplate实例则直接返回；否则从应用上下文中查找并设置。
     * 若未找到StringRedisTemplate实例，将记录警告日志，消息去重功能不可用时会降级为直接发送模式。
     * </p>
     *
     * @param applicationContext Spring应用上下文，由框架自动注入
     * @throws BeansException 当获取Bean实例失败时抛出异常
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        // 如果已经设置了StringRedisTemplate，则直接返回，避免重复设置
        if (this.stringRedisTemplate != null) {
            return;
        }

        // 从应用上下文中获取所有StringRedisTemplate类型的Bean
        Map<String, StringRedisTemplate> map = MapUtils.emptyIfNull(applicationContext.getBeansOfType(StringRedisTemplate.class));

        // 遍历获取第一个StringRedisTemplate实例并设置
        for (Map.Entry<String, StringRedisTemplate> entry : map.entrySet()) {
            this.stringRedisTemplate = entry.getValue();
            break;
        }

        // 如果未找到StringRedisTemplate实例，记录警告日志
        if (this.stringRedisTemplate == null) {
            log.warn("未找到 StringRedisTemplate 实例，消息去重功能不可用，将降级为直接发送模式");
        }
    }

}
