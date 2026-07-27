
package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqttclient.producer;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.common.MagicAPIConfigUtils;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqttclient.model.MqttClientDataSourceInfo;
import lombok.extern.slf4j.Slf4j;
import net.dreamlu.iot.mqtt.codec.MqttQoS;
import net.dreamlu.iot.mqtt.core.client.MqttClient;
import net.dreamlu.iot.mqtt.core.client.MqttClientCreator;
import net.dreamlu.iot.mqtt.spring.client.MqttClientTemplate;
import org.apache.commons.lang3.StringUtils;
import org.tio.utils.buffer.ByteBufferAllocator;

import java.beans.Transient;
import java.util.Map;

@Slf4j
public class MqttClientProducer {

    private final String id;
    private final String key;
    private final String name;
    private final MqttClientDataSourceInfo dataSourceInfo;
    private final MqttClientTemplate mqttClientTemplate;

    public MqttClientProducer(MqttClientDataSourceInfo dataSourceInfo) {
        this.id = dataSourceInfo.getId();
        this.name = dataSourceInfo.getName();
        this.key = dataSourceInfo.getKey();
        this.dataSourceInfo = dataSourceInfo;
        this.mqttClientTemplate = createMqttClientTemplate();
    }

    public MqttClientProducer(MqttClientTemplate mqttClientTemplate) {
        MqttClientDataSourceInfo dataSourceInfo = new MqttClientDataSourceInfo();
        dataSourceInfo.setId("");
        dataSourceInfo.setName("默认数据源");
        dataSourceInfo.setKey("default");
        this.id = dataSourceInfo.getId();
        this.name = dataSourceInfo.getName();
        this.key = dataSourceInfo.getKey();
        this.dataSourceInfo = dataSourceInfo;
        this.mqttClientTemplate = mqttClientTemplate;
    }

    public MqttClientDataSourceInfo getDataSourceInfo() {
        return this.dataSourceInfo;
    }

    public MqttClientTemplate getMqttClientTemplate() {
        return this.mqttClientTemplate;
    }

    @Transient
    private MqttClientTemplate createMqttClientTemplate() {
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
                .clientId(clientId)
                .username(username)
                .password(password)
                .bufferAllocator(ByteBufferAllocator.HEAP);

        String configuredProperties = MagicAPIConfigUtils.getMqttClientConfig(dataSourceInfo.getKey(), "properties", null);
        Map<String, Object> properties = MagicAPIConfigUtils.mergeProperties(dataSourceInfo.getProperties(), configuredProperties);
        if (!properties.isEmpty()) {
            try {
                JSONObject jsonObject = new JSONObject(properties);
                if (jsonObject.containsKey("keepAliveSec")) {
                    //client.keepAliveSec(jsonObject.getIntValue("keepAliveSec"));
                    mqttClientCreator.keepAliveSecs(jsonObject.getIntValue("keepAliveSec"));
                }
                if (jsonObject.containsKey("reconnect")) {
                    mqttClientCreator.reconnect(jsonObject.getBooleanValue("reconnect"));
                }
                if (jsonObject.containsKey("reIntervalSec")) {
                    mqttClientCreator.reInterval(jsonObject.getIntValue("reIntervalSec"));
                }
                if (jsonObject.containsKey("readBufferSize")) {
                    mqttClientCreator.readBufferSize(jsonObject.getIntValue("readBufferSize"));
                }
                if (jsonObject.containsKey("maxBytesInMessage")) {
                    mqttClientCreator.maxBytesInMessage(jsonObject.getIntValue("maxBytesInMessage"));
                }
                if (jsonObject.containsKey("cleanSession")) {
                    mqttClientCreator.cleanSession(jsonObject.getBooleanValue("cleanSession"));
                }
                if (jsonObject.containsKey("ssl")) {
                    //mqttClientCreator.ssl(jsonObject.getBooleanValue("ssl"));
                    //SslConfig
                }
            } catch (Exception e) {
                log.warn("应用 MQTT Client properties 失败: {}", e.getMessage(), e);
            }
        }

        MqttClientTemplate template = new MqttClientTemplate(mqttClientCreator);
        return template;
    }

    public boolean publish(String topic, Object payload) {
        return publish(topic, payload, MqttQoS.QOS0);
    }

    public boolean publish(String topic, Object payload, MqttQoS qos) {
        if (StringUtils.isBlank(topic)) {
            throw new IllegalArgumentException("Topic cannot be blank");
        }
        try {
            byte[] data;
            if (payload instanceof byte[]) {
                data = (byte[]) payload;
            } else if (payload instanceof String) {
                data = ((String) payload).getBytes();
            } else {
                data = JSON.toJSONBytes(payload);
            }
            boolean result = mqttClientTemplate.publish(topic, data, qos);
            if (result) {
                log.debug("消息发布成功，Topic: {}, QoS: {}", topic, qos);
            } else {
                log.warn("消息发布失败，Topic: {}, QoS: {}", topic, qos);
            }
            return result;
        } catch (Exception e) {
            log.error("消息发布异常，Topic: {}, QoS: {}", topic, qos, e);
            return false;
        }
    }

    @Transient
    public void close() {
        if (mqttClientTemplate != null) {
            try {
                mqttClientTemplate.destroy();
            } catch (Exception e) {
                log.warn("关闭MQTT客户端生产者资源过程中发生异常", e);
            }
        }
    }

    @Transient
    public String getId() {
        return id;
    }

    @Transient
    public String getName() {
        return name;
    }

    @Transient
    public String getKey() {
        return key;
    }
}
