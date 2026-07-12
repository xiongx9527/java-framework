package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rabbitmq.consumer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rabbitmq.model.RabbitMqSubscribeScriptInfo;

public class DefaultRabbitMqConsumerService implements RabbitMqConsumerService {
    /**
     * 订阅脚本信息映射表，外层Map的key为交换机名称，内层Map的key为路由键，value为RabbitMQ订阅脚本信息
     */
    private final Map<String, Map<String, RabbitMqSubscribeScriptInfo>> subscribeScriptInfoMap = new ConcurrentHashMap<>();

    /**
     * 构造函数，初始化默认的RabbitMQ消费者服务
     */
    public DefaultRabbitMqConsumerService() {
    }

    /**
     * 获取订阅脚本信息映射表
     *
     * @return 返回一个线程安全的嵌套Map结构，外层key为交换机名称，内层key为路由键，value为RabbitMQ订阅脚本信息
     */
    public Map<String, Map<String, RabbitMqSubscribeScriptInfo>> getSubscribeScriptInfoMap() {
        return this.subscribeScriptInfoMap;
    }
}
