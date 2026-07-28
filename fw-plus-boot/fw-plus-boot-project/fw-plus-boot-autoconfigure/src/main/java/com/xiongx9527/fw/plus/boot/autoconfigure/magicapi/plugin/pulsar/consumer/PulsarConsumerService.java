package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.pulsar.consumer;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.pulsar.model.PulsarSubscribeInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.pulsar.service.PulsarSubscribeInfoMagicResourceStorage;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.pulsar.client.api.Message;
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
public interface PulsarConsumerService {

    /**
     * Pulsar消费者服务类的日志记录器实例
     *
     * 该日志记录器用于记录PulsarConsumerService类中的各种操作日志，
     * 包括连接状态、消息处理、异常信息等，便于系统监控和问题排查。
     */
    Logger logger = LoggerFactory.getLogger(PulsarConsumerService.class);


    /**
     * 消费消息的方法
     *
     * @param key 消息订阅的键值，用于获取对应的脚本信息映射
     * @param message 待处理的消息对象，包含消息数据
     * @return 消息消费结果，true表示消费成功，false表示消费失败
     */
    default boolean consume(String key, Message<byte[]> message) {
        PulsarSubscribeInfoMagicResourceStorage subscribeInfoMagicResourceStorage = SpringUtils.getBean(PulsarSubscribeInfoMagicResourceStorage.class);
        Map<String, PulsarSubscribeInfo> subscribeScriptInfoMap = ListUtils.emptyIfNull(subscribeInfoMagicResourceStorage.findAll()).stream()
                .filter(subscribeInfo -> StringUtils.equals(subscribeInfo.getKey(), key))
                .collect(Collectors.toMap(PulsarSubscribeInfo::getTopic, val -> val, (k1, k2) -> k1));
        if (MapUtils.isEmpty(subscribeScriptInfoMap)) {
            logger.warn("通过key:{}未找到对应的订阅脚本信息，请检查数据源是否正确", key);
            return false;
        }
        // 遍历所有订阅脚本信息并执行对应的脚本
        // Pulsar topic 完整路径格式: persistent://tenant/namespace/topic
        for (PulsarSubscribeInfo subscribeScriptInfo : subscribeScriptInfoMap.values()) {
            if (StringUtils.equals(subscribeScriptInfo.getTopic(), message.getTopicName())
                    || message.getTopicName().endsWith("/" + subscribeScriptInfo.getTopic())) {
                MagicScriptContext magicScriptContext = new MagicScriptContext();
                magicScriptContext.setScriptName("pulsar/" + subscribeScriptInfo.getKey() + "/" + subscribeScriptInfo.getTopic() + "(" + subscribeScriptInfo.getName() + ")");
                magicScriptContext.set("msg", message);
                Object result = ScriptManager.executeScript(subscribeScriptInfo.getScript(), magicScriptContext);
                if (result != null && !Boolean.TRUE.equals(result)) {
                    return false;
                }
                return true;
            }
        }
        // 如果没有执行任何脚本，则返回消费失败
        logger.warn("通过topic:{}未找到对应的订阅脚本，请检查topic是否正确", message.getTopicName());
        return false;
    }

}
