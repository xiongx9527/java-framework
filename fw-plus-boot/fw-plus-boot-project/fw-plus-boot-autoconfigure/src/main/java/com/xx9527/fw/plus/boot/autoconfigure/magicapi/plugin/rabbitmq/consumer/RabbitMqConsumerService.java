package com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rabbitmq.consumer;

import java.util.Map;
import java.util.stream.Collectors;

import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.kafka.model.KafkaSubscribeInfo;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.kafka.service.KafkaSubscribeInfoMagicResourceStorage;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rabbitmq.model.RabbitMqSubscribeInfo;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rabbitmq.model.RabbitMqSubscribeScriptInfo;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rabbitmq.service.RabbitMqSubscribeInfoMagicResourceStorage;
import com.xx9527.fw.plus.common.util.SpringUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.transaction.annotation.Transactional;
import org.ssssssss.magicapi.core.config.MagicConfiguration;
import org.ssssssss.magicapi.core.service.MagicResourceService;
import org.ssssssss.magicapi.utils.ScriptManager;
import org.ssssssss.script.MagicScriptContext;

public interface RabbitMqConsumerService {

    /**
     * 日志记录器实例，用于记录RabbitMQ消费者服务的相关日志信息
     * 使用SLF4J框架创建，以当前类作为日志记录器名称
     */
    Logger logger = LoggerFactory.getLogger(RabbitMqConsumerService.class);

    /**
     * 消费消息的方法
     *
     * @param key 消息键值，用于获取对应的订阅脚本信息映射
     * @param msg 消息对象，包含消息内容和属性信息
     * @return 消费成功返回true，消费失败或异常情况返回false
     */
    @Transactional(rollbackFor = {Exception.class})
    default boolean consume(String key, Message msg) {
        RabbitMqSubscribeInfoMagicResourceStorage subscribeInfoMagicResourceStorage = SpringUtils.getBean(RabbitMqSubscribeInfoMagicResourceStorage.class);
        Map<String, RabbitMqSubscribeInfo> subscribeScriptInfoMap = ListUtils.emptyIfNull(subscribeInfoMagicResourceStorage.findAll()).stream().filter(subscribeInfo -> StringUtils.equals(subscribeInfo.getKey(), key))
                .collect(Collectors.toList()).stream().collect(Collectors.toMap(RabbitMqSubscribeInfo::getQueueName, val -> val, (k1, k2) -> k1));
        if (subscribeScriptInfoMap == null) {
            logger.warn("通过key:{}未找到对应的订阅脚本信息，请检查数据源是否正确", key);
            return false;
        }

        String consumerQueue = "";
        // 检查消息和队列信息
        if (msg != null && msg.getMessageProperties() != null) {
            consumerQueue = msg.getMessageProperties().getConsumerQueue();
        }

        // 遍历订阅脚本信息，查找匹配的队列并执行对应脚本
        for (RabbitMqSubscribeInfo subscribeScriptInfo : subscribeScriptInfoMap.values()) {
            if (StringUtils.equals(subscribeScriptInfo.getQueueName(), consumerQueue)) {
                MagicScriptContext magicScriptContext = new MagicScriptContext();
                magicScriptContext.setScriptName("rabbitmq/" + subscribeScriptInfo.getKey() + "/" + subscribeScriptInfo.getQueueName() + "(" + subscribeScriptInfo.getName() + ")");
                magicScriptContext.set("msg", msg);
                try {
                    // 执行脚本并返回结果
                    Object result = ScriptManager.executeScript(subscribeScriptInfo.getScript(), magicScriptContext);
                    if (result != null && !Boolean.TRUE.equals(result)) {
                        return false;
                    }
                    return true;
                } catch (Exception e) {
                    throw e;
                }
            }
        }
        return false;
    }

}
