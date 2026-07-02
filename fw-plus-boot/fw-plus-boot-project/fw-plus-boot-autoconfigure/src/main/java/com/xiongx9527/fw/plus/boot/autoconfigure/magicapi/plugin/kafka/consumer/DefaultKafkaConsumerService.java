package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.kafka.consumer;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.kafka.model.KafkaSubscribeScriptInfo;

import java.util.HashMap;
import java.util.Map;

/**
 * 默认队列消费服务接口实现
 *
 * @author zzy
 */
public class DefaultKafkaConsumerService implements KafkaConsumerService {

    // 订阅脚本信息映射表，外层Map的key为数据源key，内层Map的key为topic，value为Kafka订阅脚本信息
    private final Map<String, Map<String, KafkaSubscribeScriptInfo>> subscribeScriptInfoMap = new HashMap<>();

    /**
     * 获取订阅脚本信息映射表
     *
     * @return Map<String, Map<String, KafkaSubscribeScriptInfo>> 订阅脚本信息映射表
     */
    public Map<String, Map<String, KafkaSubscribeScriptInfo>> getSubscribeScriptInfoMap() {
        return this.subscribeScriptInfoMap;
    }

}
