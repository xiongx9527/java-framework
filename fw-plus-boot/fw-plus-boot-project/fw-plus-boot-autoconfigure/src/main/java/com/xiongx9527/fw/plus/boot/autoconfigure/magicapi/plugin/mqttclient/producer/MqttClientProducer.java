
package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqttclient.producer;

import com.alibaba.fastjson2.JSON;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.common.MagicAPIConfigUtils;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqttclient.common.MqttClientConfigSupport;
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
        String ip = MagicAPIConfigUtils.getMqttClientConfig(dataSourceInfo.getKey(), "ip", dataSourceInfo.getIp());
        String port = MagicAPIConfigUtils.getMqttClientConfig(dataSourceInfo.getKey(), "port", dataSourceInfo.getPort());
        String clientId = MagicAPIConfigUtils.getMqttClientConfig(dataSourceInfo.getKey(), "clientId", dataSourceInfo.getClientId());
        String username = MagicAPIConfigUtils.getMqttClientConfig(dataSourceInfo.getKey(), "username", dataSourceInfo.getUsername());
        String password = MagicAPIConfigUtils.getMqttClientConfig(dataSourceInfo.getKey(), "password", dataSourceInfo.getPassword());

        MqttClientCreator mqttClientCreator = MqttClient.create()
                .ip(ip)
                .port(MqttClientConfigSupport.parsePort(port))
                .clientId(clientId)
                .username(username)
                .password(password)
                .bufferAllocator(ByteBufferAllocator.HEAP);

        String configuredProperties = MagicAPIConfigUtils.getMqttClientConfig(dataSourceInfo.getKey(), "properties", null);
        Map<String, Object> properties = MagicAPIConfigUtils.mergeProperties(dataSourceInfo.getProperties(), configuredProperties);
        MqttClientConfigSupport.applyCommonOptions(mqttClientCreator, properties);
        return new MqttClientTemplate(mqttClientCreator);
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
