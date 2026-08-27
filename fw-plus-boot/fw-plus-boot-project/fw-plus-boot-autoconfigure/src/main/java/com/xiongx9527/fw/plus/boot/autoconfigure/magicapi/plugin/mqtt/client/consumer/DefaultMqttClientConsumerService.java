
package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.client.consumer;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.client.model.MqttClientSubscribeInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.client.service.MqttClientSubscribeInfoMagicResourceStorage;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import lombok.extern.slf4j.Slf4j;
import net.dreamlu.iot.mqtt.codec.MqttPublishMessage;
import net.dreamlu.iot.mqtt.codec.MqttQoS;
import org.apache.commons.collections4.ListUtils;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class DefaultMqttClientConsumerService implements MqttClientConsumerService {

    @Override
    public boolean consume(String dataSourceKey, String topic, MqttQoS qos, MqttPublishMessage message,byte[] payload) {
        MqttClientSubscribeInfoMagicResourceStorage storage = SpringUtils.getBean(MqttClientSubscribeInfoMagicResourceStorage.class);
        List<MqttClientSubscribeInfo> subscribeInfoList = ListUtils.emptyIfNull(storage.findAll());

        List<MqttClientSubscribeInfo> matchedList = subscribeInfoList.stream()
                .filter(info -> info.getKey().equals(dataSourceKey) && info.getTopic().equals(topic)).collect(Collectors.toList());

        boolean allSuccess = true;
        /**
        for (MqttClientSubscribeInfo subscribeInfo : matchedList) {
            try {
                MagicResourceService resourceService = org.ssssssss.magicapi.core.config.MagicConfiguration.getMagicResourceService();
                String scriptName = resourceService.getScriptName(subscribeInfo);

                MagicScriptContext magicScriptContext = new MagicScriptContext();
                magicScriptContext.setScriptName(scriptName);
                magicScriptContext.set("key", dataSourceKey);
                magicScriptContext.set("topic", topic);
                magicScriptContext.set("qoS", qos);
                magicScriptContext.set("message", message);

                byte[] payloadBytes = new byte[message.getPayload().readableBytes()];
                message.getPayload().getBytes(message.getPayload().readerIndex(), payloadBytes);
                String payloadStr = new String(payloadBytes, StandardCharsets.UTF_8);
                magicScriptContext.set("payload", payloadStr);

                Object result = ScriptManager.executeScript(subscribeInfo.getScript(), magicScriptContext);
                log.debug("MQTT Client 订阅消息脚本执行结果: {}", result);
            } catch (Exception e) {
                log.error("MQTT Client 订阅消息脚本执行失败, topic: {}", topic, e);
                allSuccess = false;
            }
        }
         **/
        return allSuccess;
    }
}