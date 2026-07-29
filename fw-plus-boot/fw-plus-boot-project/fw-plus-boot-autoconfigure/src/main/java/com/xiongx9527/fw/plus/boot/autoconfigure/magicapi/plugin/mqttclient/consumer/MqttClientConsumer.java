
package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqttclient.consumer;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.common.MagicAPIConfigUtils;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqttclient.common.MqttClientConfigSupport;
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
        addSubscriptions(subscribeScriptInfos);
    }

    public boolean addSubscriptions(List<MqttClientSubscribeScriptInfo> subscribeScriptInfos) {
        if (CollectionUtils.isEmpty(subscribeScriptInfos)) {
            return true;
        }

        boolean successful = true;
        for (MqttClientSubscribeScriptInfo subscribeScriptInfo : subscribeScriptInfos) {
            String topicName = subscribeScriptInfo == null ? null : subscribeScriptInfo.getTopic();
            if (StringUtils.isBlank(topicName)) {
                continue;
            }

            MqttClientTemplate template = null;
            try {
                Boolean enable = subscribeScriptInfo.isEnabled();
                if (enable != null) {
                    String enableStr = MagicAPIConfigUtils.getMqttClientSubscribeConfig(
                            subscribeScriptInfo.getKey(), topicName, "enabled", enable.toString());
                    enable = Boolean.parseBoolean(enableStr);
                }
                if (!Boolean.TRUE.equals(enable)) {
                    unsubscribe(topicName);
                    continue;
                }

                unsubscribe(topicName);

                String ip = MagicAPIConfigUtils.getMqttClientConfig(dataSourceInfo.getKey(), "ip", dataSourceInfo.getIp());
                String port = MagicAPIConfigUtils.getMqttClientConfig(dataSourceInfo.getKey(), "port", dataSourceInfo.getPort());
                String clientId = MagicAPIConfigUtils.getMqttClientConfig(dataSourceInfo.getKey(), "clientId", dataSourceInfo.getClientId());
                String username = MagicAPIConfigUtils.getMqttClientConfig(dataSourceInfo.getKey(), "username", dataSourceInfo.getUsername());
                String password = MagicAPIConfigUtils.getMqttClientConfig(dataSourceInfo.getKey(), "password", dataSourceInfo.getPassword());

                String subscribeClientId = MqttClientConfigSupport.buildSubscribeClientId(
                        clientId, dataSourceInfo.getKey(), topicName);
                MqttClientCreator mqttClientCreator = MqttClient.create()
                        .ip(ip)
                        .port(MqttClientConfigSupport.parsePort(port))
                        .clientId(subscribeClientId)
                        .username(username)
                        .password(password);

                String other = MagicAPIConfigUtils.getMqttClientSubscribeConfig(
                        dataSourceInfo.getKey(), topicName, "other", subscribeScriptInfo.getOther());
                MqttClientConfigSupport.applyCommonOptions(mqttClientCreator, other);

                template = new MqttClientTemplate(mqttClientCreator);
                template.setApplicationContext(SpringUtils.getContext());
                template.afterPropertiesSet();
                String qosStr = MagicAPIConfigUtils.getMqttClientSubscribeConfig(dataSourceInfo.getKey(), topicName, "qos", subscribeScriptInfo.getQos());
                MqttQoS qos = MqttClientConfigSupport.parseQos(qosStr);

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
                        throw e;
                    }
                }

                this.consumerMap.put(topicName, template);
                template = null;
            } catch (Exception e) {
                successful = false;
                if (template != null) {
                    try {
                        template.destroy();
                    } catch (Exception destroyException) {
                        log.warn("清理未完成初始化的MQTT客户端消费者失败, topic: {}", topicName, destroyException);
                    }
                }
                log.error("创建MQTT客户端消费者失败, topic: {}", topicName, e);
            }
        }
        return successful;
    }

    public void unsubscribe(MqttClientSubscribeInfo subscribeInfo) {
        unsubscribe(subscribeInfo.getTopic());
    }

    void unsubscribe(String topic) {
        if (StringUtils.isBlank(topic)) {
            return;
        }
        MqttClientTemplate template = this.consumerMap.remove(topic);
        if (template != null) {
            try {
                template.unSubscribe(topic);
            } catch (Exception e) {
                log.warn("取消MQTT客户端订阅失败, topic: {}", topic, e);
            }
            try {
                template.destroy();
            } catch (Exception e) {
                log.warn("销毁MQTT客户端消费者失败, topic: {}", topic, e);
            }
        }
    }

    public void close() {
        for (Map.Entry<String, MqttClientTemplate> entry : consumerMap.entrySet()) {
            MqttClientTemplate template = entry.getValue();
            try {
                template.unSubscribe(entry.getKey());
            } catch (Exception e) {
                log.error("取消MQTT客户端订阅失败, topic: {}", entry.getKey(), e);
            }
            try {
                template.destroy();
            } catch (Exception e) {
                log.error("销毁MQTT客户端消费者失败, topic: {}", entry.getKey(), e);
            }
        }
        consumerMap.clear();
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getKey() { return key; }
    public MqttClientDataSourceInfo getDataSourceInfo() { return dataSourceInfo; }
}
