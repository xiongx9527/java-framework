package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.kafka.consumer;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.kafka.model.KafkaSubscribeInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.kafka.service.KafkaSubscribeInfoMagicResourceStorage;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ssssssss.magicapi.core.config.MagicConfiguration;
import org.ssssssss.magicapi.core.service.MagicResourceService;
import org.ssssssss.magicapi.utils.ScriptManager;
import org.ssssssss.script.MagicScriptContext;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * 队列消费服务接口
 *
 * @author zzy
 */
public interface KafkaConsumerService {

    Logger logger = LoggerFactory.getLogger(KafkaConsumerService.class);

    /**
     * 消费默认实现
     * 根据数据源key和ConsumerRecord消息，查找对应的订阅脚本并执行消费逻辑
     *
     * @param key 数据源key
     * @param msg Kafka消费者记录消息
     * @return boolean 消费成功返回true，消费失败返回false
     */
    default boolean consume(String key, ConsumerRecord<Object, Object> msg) {
        KafkaSubscribeInfoMagicResourceStorage subscribeInfoMagicResourceStorage = SpringUtils.getBean(KafkaSubscribeInfoMagicResourceStorage.class);
        Map<String, KafkaSubscribeInfo> subscribeScriptInfoMap = ListUtils.emptyIfNull(subscribeInfoMagicResourceStorage.findAll()).stream()
                .filter(subscribeInfo -> StringUtils.equals(subscribeInfo.getKey(), key))
                .collect(Collectors.toMap(KafkaSubscribeInfo::getTopic, val -> val, (k1, k2) -> k1));
        if (MapUtils.isEmpty(subscribeScriptInfoMap)) {
            logger.warn("通过key:{}未找到对应的订阅脚本信息，请检查数据源是否正确", key);
            return false;
        }
        for (KafkaSubscribeInfo subscribeScriptInfo : subscribeScriptInfoMap.values()) {
            if (StringUtils.equals(subscribeScriptInfo.getTopic(), msg.topic())) {
                MagicScriptContext magicScriptContext = new MagicScriptContext();
                magicScriptContext.setScriptName("kafka/" + subscribeScriptInfo.getKey() + "/" + subscribeScriptInfo.getTopic() + "(" + subscribeScriptInfo.getName() + ")");
                magicScriptContext.set("msg", msg);
                Object result = ScriptManager.executeScript(subscribeScriptInfo.getScript(), magicScriptContext);
                if (result != null && !Boolean.TRUE.equals(result)) {
                    return false;
                }
                return true;
            }
        }
        logger.warn("通过topic:{}未找到对应的订阅脚本，请检查topic是否正确", msg.topic());
        return false;
    }
}
