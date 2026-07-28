
package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqttclient.model;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.common.MagicAPIConfigUtils;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqttclient.consumer.MqttClientConsumer;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqttclient.consumer.MqttClientConsumerService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqttclient.producer.MqttClientProducer;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import lombok.extern.slf4j.Slf4j;
import net.dreamlu.iot.mqtt.core.client.MqttClient;
import net.dreamlu.iot.mqtt.core.client.MqttClientCreator;
import net.dreamlu.iot.mqtt.spring.client.MqttClientTemplate;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.DisposableBean;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class MagicDynamicMqttClientDataSource implements DisposableBean {

    private final Map<String, MqttClientProducer> producerMap = new ConcurrentHashMap<>();
    private final Map<String, MqttClientConsumer> consumerMap = new ConcurrentHashMap<>();
    private final MqttClientConsumerService mqttClientConsumerService;

    public MagicDynamicMqttClientDataSource(MqttClientConsumerService mqttClientConsumerService) {
        this.mqttClientConsumerService = mqttClientConsumerService;
    }


    public MqttClientTemplate createMqttClientTemplate(MqttClientDataSourceInfo dataSourceInfo) throws Exception {
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
                .password(password);

        String configuredProperties = MagicAPIConfigUtils.getMqttClientConfig(dataSourceInfo.getKey(), "properties", null);
        Map<String, Object> properties = MagicAPIConfigUtils.mergeProperties(dataSourceInfo.getProperties(), configuredProperties);
        if (!properties.isEmpty()) {
            try {
                JSONObject jsonObject = new JSONObject(properties);
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
                log.warn("应用 MQTT Client properties 失败: {}", e.getMessage(), e);
            }
        }

        MqttClientTemplate template = new MqttClientTemplate(mqttClientCreator);
        template.setApplicationContext(SpringUtils.getContext());
        template.afterPropertiesSet();
        return template;
    }

    public boolean putProducer(MqttClientDataSourceInfo info) {
        try {
            MqttClientProducer oldProducer = this.producerMap.put(info.getKey(), new MqttClientProducer(info));
            if (oldProducer != null) {
                oldProducer.close();
            }
            log.info("注册 MQTT Client 生产者:[{}]完成执行", info.getKey());
        } catch (Throwable e) {
            log.error("MQTT Client 生产者:[{}]注册失败", info.getKey(), e);
            return false;
        }
        return true;
    }

    public boolean putConsumer(MqttClientDataSourceInfo info, List<MqttClientSubscribeScriptInfo> subscribeScriptInfos) {
        try {
            MqttClientConsumer oldConsumer = this.consumerMap.get(info.getKey());
            if (oldConsumer != null) {
                oldConsumer.createConsumer(subscribeScriptInfos);
            } else {
                MqttClientConsumer consumer = new MqttClientConsumer(this.mqttClientConsumerService, info, subscribeScriptInfos);
                this.consumerMap.put(info.getKey(), consumer);
            }
            log.info("注册 MQTT Client 消费者:[{}]完成执行", info.getKey());
        } catch (Throwable e) {
            log.error("MQTT Client 消费者:[{}]注册失败", info.getKey(), e);
            return false;
        }
        return true;
    }

    public void deleteProducer(String datasourceKey) {
        if (datasourceKey != null && !datasourceKey.isEmpty()) {
            MqttClientProducer producer = this.producerMap.remove(datasourceKey);
            if (producer != null) {
                producer.close();
            }
        }
    }

    public void deleteConsumer(String datasourceKey) {
        if (datasourceKey != null && !datasourceKey.isEmpty()) {
            MqttClientConsumer consumer = this.consumerMap.remove(datasourceKey);
            if (consumer != null) {
                consumer.close();
            }
        }
    }

    public Collection<MqttClientProducer> producerNodes() {
        List<MqttClientProducer> producers = new ArrayList<>(this.producerMap.values());
        try {
            MqttClientTemplate mqttClientTemplate = SpringUtils.getBean(MqttClientTemplate.class);
            if (mqttClientTemplate != null) {
                producers.add(new MqttClientProducer(mqttClientTemplate));
            }
        } catch (Exception e) {
        }
        return producers;
    }

    public MqttClientProducer getProducer(String datasourceKey) {
        return this.producerMap.get(datasourceKey);
    }

    public MqttClientConsumer getConsumer(String datasourceKey) {
        return this.consumerMap.get(datasourceKey);
    }

    @Override
    public void destroy() throws Exception {
        log.info("开始关闭所有 MQTT Client 生产者和消费者连接...");
        for (Map.Entry<String, MqttClientProducer> entry : this.producerMap.entrySet()) {
            try {
                entry.getValue().close();
                log.info("已关闭 MQTT Client 生产者: {}", entry.getKey());
            } catch (Exception e) {
                log.error("关闭 MQTT Client 生产者 {} 时发生异常", entry.getKey(), e);
            }
        }
        this.producerMap.clear();
        for (Map.Entry<String, MqttClientConsumer> entry : this.consumerMap.entrySet()) {
            try {
                entry.getValue().close();
                log.info("已关闭 MQTT Client 消费者: {}", entry.getKey());
            } catch (Exception e) {
                log.error("关闭 MQTT Client 消费者 {} 时发生异常", entry.getKey(), e);
            }
        }
        this.consumerMap.clear();
        log.info("所有 MQTT Client 连接已关闭");
    }
}
