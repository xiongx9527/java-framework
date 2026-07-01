package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.debezium.model;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;

public class StandaloneMagicDynamicDebeziumDataSource extends MagicDynamicDebeziumDataSource {
    /**
     * 发送事件到Kafka
     * 使用KafkaTemplate将ProducerRecord发送到Kafka集群
     *
     * @param kafkaTemplate Kafka模板，用于发送消息
     * @param messageId 消息ID
     * @param record 要发送的ProducerRecord对象
     */
    @Override
    void onEventSent(KafkaTemplate kafkaTemplate, String messageId ,ProducerRecord<String, Object> record) {
        kafkaTemplate.send(record);
    }
}
