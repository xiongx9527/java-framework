
package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqttclient.consumer;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.common.MagicAPIConfigUtils;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqttclient.model.MqttClientDataSourceInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqttclient.model.MqttClientSubscribeInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqttclient.model.MqttClientSubscribeScriptInfo;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import lombok.extern.slf4j.Slf4j;
import net.dreamlu.iot.mqtt.codec.MqttPublishMessage;
import net.dreamlu.iot.mqtt.codec.MqttQoS;
import net.dreamlu.iot.mqtt.core.client.IMqttClientMessageListener;
import net.dreamlu.iot.mqtt.core.client.MqttClient;
import net.dreamlu.iot.mqtt.core.client.MqttClientCreator;
import net.dreamlu.iot.mqtt.spring.client.MqttClientTemplate;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.tio.core.ChannelContext;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class MqttClientConsumer {

    private final String id;
    private final String key;
    private final String name;
    private final MqttClientConsumerService mqttClientConsumerService;
    private final MqttClientDataSourceInfo dataSourceInfo;
    private final Map<String, MqttClientTemplate> consumerMap = new ConcurrentHashMap<>();

    public MqttClientConsumer(MqttClientConsumerService mqttClientConsumerService,
                               MqttClientDataSourceInfo dataSourceInfo,
                               List<MqttClientSubscribeScriptInfo> subscribeScriptInfos) {
        this.mqttClientConsumerService = mqttClientConsumerService;
        this.dataSourceInfo = dataSourceInfo;
        this.id = dataSourceInfo.getId();
        this.name = dataSourceInfo.getName();
        this.key = dataSourceInfo.getKey();
        createConsumer(subscribeScriptInfos);
    }

    public void createConsumer(List<MqttClientSubscribeScriptInfo> subscribeScriptInfos) {
        if (CollectionUtils.isEmpty(subscribeScriptInfos)) {
            return;
        }

        for (MqttClientSubscribeScriptInfo subscribeScriptInfo : subscribeScriptInfos) {
            Boolean enable = subscribeScriptInfo.isEnabled();
            if (enable != null) {
                String enableStr = MagicAPIConfigUtils.getMqttClientSubscribeConfig(
                        subscribeScriptInfo.getKey(), subscribeScriptInfo.getTopic(), "enabled", enable.toString());
                try {
                    enable = Boolean.parseBoolean(enableStr);
                } catch (Exception e) {
                    enable = Boolean.FALSE;
                }
            }
            if (!Boolean.TRUE.equals(enable)) {
                continue;
            }

            String topicName = subscribeScriptInfo.getTopic();
            if (StringUtils.isBlank(topicName)) {
                continue;
            }

            MqttClientTemplate oldTemplate = this.consumerMap.get(topicName);
            if (oldTemplate != null) {
                try {
                    oldTemplate.unSubscribe(topicName);
                    oldTemplate.destroy();
                } catch (Exception e) {
                    log.error("停止旧的MQTT客户端消费者失败, topic: {}", topicName, e);
                }
            }

            try {
                String ip = dataSourceInfo.getIp();
                ip = MagicAPIConfigUtils.getMqttClientConfig(dataSourceInfo.getKey(), "ip", ip);
                String port = dataSourceInfo.getPort();
                port = MagicAPIConfigUtils.getMqttClientConfig(dataSourceInfo.getKey(), "port", port);
                String clientId = dataSourceInfo.getClientId();
                clientId = MagicAPIConfigUtils.getMqttClientConfig(dataSourceInfo.getKey(), "clientId", clientId);
                String username = dataSourceInfo.getUsername();
                username = MagicAPIConfigUtils.getMqttClientConfig(dataSourceInfo.getKey(), "username", username);
                String password = dataSourceInfo.getPassword();
                password = MagicAPIConfigUtils.getMqttClientConfig(dataSourceInfo.getKey(), "password", password);

                int portInt;
                try {
                    portInt = Integer.parseInt(port);
                } catch (Exception e) {
                    portInt = 1883;
                }

                MqttClientCreator mqttClientCreator = MqttClient.create()
                        .ip(ip)
                        .port(portInt)
                        .clientId(dataSourceInfo.getClientId())
                        .username(username)
                        .password(password);

                String other = subscribeScriptInfo.getOther();
                other = MagicAPIConfigUtils.getMqttClientSubscribeConfig(dataSourceInfo.getKey(), topicName, "other", other);
                if (StringUtils.isNotBlank(other)) {
                    try {
                        JSONObject jsonObject = JSON.parseObject(other);
                        if (jsonObject.containsKey("keepAliveSec")) {
                            mqttClientCreator.keepAliveSecs(jsonObject.getIntValue("keepAliveSec"));
                        }
                        if (jsonObject.containsKey("reconnect")) {
                            mqttClientCreator.reconnect(jsonObject.getBooleanValue("reconnect"));
                        }
                        if (jsonObject.containsKey("cleanSession")) {
                            mqttClientCreator.cleanSession(jsonObject.getBooleanValue("cleanSession"));
                        }
                        if (jsonObject.containsKey("ssl")) {
                            //mqttClient.ssl(jsonObject.getBooleanValue("ssl"));
                        }
                    } catch (Exception e) {
                        log.warn("解析 other 配置失败: {}", e.getMessage(), e);
                    }
                }

                MqttClientTemplate template = new MqttClientTemplate(mqttClientCreator);
                template.setApplicationContext(SpringUtils.getContext());
                template.afterPropertiesSet();
                String qosStr = MagicAPIConfigUtils.getMqttClientSubscribeConfig(dataSourceInfo.getKey(), topicName, "qos", subscribeScriptInfo.getQos());
                MqttQoS qos = MqttQoS.QOS0;
                try {
                    int qosInt = Integer.parseInt(qosStr);
                    if (qosInt == 1) qos = MqttQoS.QOS1;
                    else if (qosInt == 2) qos = MqttQoS.QOS2;
                } catch (Exception ignored) {}

                final MqttClientConsumerService consumerService = this.mqttClientConsumerService;
                final String dsKey = this.dataSourceInfo.getKey();
                MqttQoS finalQos = qos;
                template.subscribe(topicName, qos, new IMqttClientMessageListener() {
                    @Override
                    public void onMessage(ChannelContext context, String topic, MqttPublishMessage message, byte[] payload) {
                        try {
                            consumerService.consume(dsKey, topic, finalQos, message, payload);
                        } catch (Exception e) {
                            log.error("处理MQTT客户端订阅消息失败, topic: {}", topic, e);
                        }
                    }
                });

                if (SpringUtils.isApplicationFullyStarted()) {
                    template.afterSingletonsInstantiated();
                } else {
                    try {
                        while (!SpringUtils.isApplicationFullyStarted()) {
                            Thread.sleep(100);
                        }
                        template.afterSingletonsInstantiated();
                        log.info("MQTT Client consumer started for topic: {}", topicName);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        log.error("Interrupted while waiting for application startup", e);
                    }
                }

                this.consumerMap.put(topicName, template);
            } catch (Exception e) {
                log.error("创建MQTT客户端消费者失败, topic: {}", topicName, e);
            }
        }
    }

    public void unsubscribe(MqttClientSubscribeInfo subscribeInfo) {
        String topic = subscribeInfo.getTopic();
        if (StringUtils.isBlank(topic)) {
            return;
        }
        MqttClientTemplate template = this.consumerMap.remove(topic);
        if (template != null) {
            try {
                template.unSubscribe(topic);
                template.destroy();
            } catch (Exception e) {
                log.warn("取消MQTT客户端订阅失败, topic: {}", topic, e);
            }
        }
    }

    public void close() {
        for (Map.Entry<String, MqttClientTemplate> entry : consumerMap.entrySet()) {
            try {
                MqttClientTemplate template = entry.getValue();
                template.unSubscribe(entry.getKey());
                template.destroy();
            } catch (Exception e) {
                log.error("关闭MQTT客户端消费者失败, topic: {}", entry.getKey(), e);
            }
        }
        consumerMap.clear();
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getKey() { return key; }
    public MqttClientDataSourceInfo getDataSourceInfo() { return dataSourceInfo; }
}