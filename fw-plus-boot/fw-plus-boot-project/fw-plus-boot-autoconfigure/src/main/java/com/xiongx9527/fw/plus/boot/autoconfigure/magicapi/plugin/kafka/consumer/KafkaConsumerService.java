package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.kafka.consumer;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.kafka.model.KafkaSubscribeInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.kafka.service.KafkaSubscribeInfoMagicResourceStorage;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import org.apache.commons.collections4.ListUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ssssssss.magicapi.core.config.MagicConfiguration;
import org.ssssssss.magicapi.core.service.MagicResourceService;
import org.ssssssss.magicapi.utils.ScriptManager;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.transaction.annotation.Transactional;
import org.ssssssss.script.MagicScriptContext;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * 队列消费服务接口
 *
 * @author zzy
 */
public interface KafkaConsumerService {

    /**
     * Kafka消费者服务日志记录器实例
     *
     * 该日志记录器用于记录KafkaConsumerService类中的各种操作日志，
     * 包括消费消息、处理异常、连接状态等信息。
     * 使用SLF4J框架提供统一的日志记录接口。
     */
    Logger logger = LoggerFactory.getLogger(KafkaConsumerService.class);

    /**
     * 消费默认实现
     * 根据数据源key和ConsumerRecord消息，查找对应的订阅脚本并执行消费逻辑
     *
     * @param key 数据源key
     * @param msg Kafka消费者记录消息
     * @return boolean 消费成功返回true，消费失败返回false
     */
    @Transactional(rollbackFor = Throwable.class)
    default boolean consume(String key, ConsumerRecord<Object, Object> msg) {
        MagicResourceService resourceService = MagicConfiguration.getMagicResourceService();
        KafkaSubscribeInfoMagicResourceStorage subscribeInfoMagicResourceStorage = SpringUtils.getBean(KafkaSubscribeInfoMagicResourceStorage.class);
        Map<String, KafkaSubscribeInfo> subscribeScriptInfoMap = ListUtils.emptyIfNull(subscribeInfoMagicResourceStorage.findAll()).stream().filter(subscribeInfo -> StringUtils.equals(subscribeInfo.getKey(), key))
                .collect(Collectors.toList()).stream().collect(Collectors.toMap(KafkaSubscribeInfo::getTopic, val -> val, (k1, k2) -> k1));
        if (subscribeScriptInfoMap == null) {
            logger.warn("通过key:{}未找到对应的订阅脚本信息，请检查数据源是否正确", key);
            return false;
        }
        for (KafkaSubscribeInfo subscribeScriptInfo : subscribeScriptInfoMap.values()) {
            if (StringUtils.equals(subscribeScriptInfo.getTopic(), msg.topic())) {
                MagicScriptContext magicScriptContext = new MagicScriptContext();
                magicScriptContext.setScriptName(resourceService.getScriptName(subscribeScriptInfo));
                magicScriptContext.set("msg", msg);
                try {
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
        // 如果没有执行任何脚本，则返回消费失败
        logger.warn("通过topic:{}未找到对应的订阅脚本，请检查topic是否正确", msg.topic());
        return false;
    }
}
