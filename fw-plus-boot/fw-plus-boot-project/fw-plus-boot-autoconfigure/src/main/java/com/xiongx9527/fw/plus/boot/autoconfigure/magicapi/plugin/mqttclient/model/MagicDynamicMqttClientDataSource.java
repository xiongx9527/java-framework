
package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqttclient.model;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.common.MagicAPIConfigUtils;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqttclient.common.MqttClientConfigSupport;
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
import java.util.function.Supplier;

@Slf4j
public class MagicDynamicMqttClientDataSource implements DisposableBean {

    private final Map<String, MqttClientProducer> producerMap = new ConcurrentHashMap<>();
    private final Map<String, MqttClientConsumer> consumerMap = new ConcurrentHashMap<>();
    private final MqttClientConsumerService mqttClientConsumerService;
    private boolean destroyed;

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
        MqttClientCreator mqttClientCreator = MqttClient.create()
                .ip(ip)
                .port(MqttClientConfigSupport.parsePort(port))
                .clientId(clientId)
                .username(username)
                .password(password);

        String configuredProperties = MagicAPIConfigUtils.getMqttClientConfig(dataSourceInfo.getKey(), "properties", null);
        Map<String, Object> properties = MagicAPIConfigUtils.mergeProperties(dataSourceInfo.getProperties(), configuredProperties);
        MqttClientConfigSupport.applyCommonOptions(mqttClientCreator, properties);

        MqttClientTemplate template = new MqttClientTemplate(mqttClientCreator);
        template.setApplicationContext(SpringUtils.getContext());
        template.afterPropertiesSet();
        return template;
    }

    public synchronized boolean putProducer(MqttClientDataSourceInfo info) {
        if (this.destroyed) {
            log.warn("MQTT Client 数据源已销毁，拒绝注册生产者:[{}]", info.getKey());
            return false;
        }
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

    public synchronized boolean putConsumer(MqttClientDataSourceInfo info, List<MqttClientSubscribeScriptInfo> subscribeScriptInfos) {
        if (this.destroyed) {
            log.warn("MQTT Client 数据源已销毁，拒绝注册消费者:[{}]", info.getKey());
            return false;
        }
        try {
            MqttClientConsumer consumer = this.consumerMap.get(info.getKey());
            if (consumer == null) {
                consumer = new MqttClientConsumer(this.mqttClientConsumerService, info, null);
                if (!consumer.addSubscriptions(subscribeScriptInfos)) {
                    consumer.close();
                    return false;
                }
                this.consumerMap.put(info.getKey(), consumer);
            } else if (!consumer.addSubscriptions(subscribeScriptInfos)) {
                return false;
            }
            log.info("注册 MQTT Client 消费者:[{}]完成执行", info.getKey());
        } catch (Throwable e) {
            log.error("MQTT Client 消费者:[{}]注册失败", info.getKey(), e);
            return false;
        }
        return true;
    }

    public synchronized boolean replaceConsumer(MqttClientDataSourceInfo info,
                                                List<MqttClientSubscribeScriptInfo> subscribeScriptInfos) {
        if (this.destroyed) {
            log.warn("MQTT Client 数据源已销毁，拒绝刷新消费者:[{}]", info.getKey());
            return false;
        }
        MqttClientConsumer candidate = new MqttClientConsumer(this.mqttClientConsumerService, info, null);
        try {
            if (!candidate.addSubscriptions(subscribeScriptInfos)) {
                candidate.close();
                return false;
            }
            MqttClientConsumer oldConsumer = this.consumerMap.put(info.getKey(), candidate);
            if (oldConsumer != null) {
                oldConsumer.close();
            }
            log.info("刷新 MQTT Client 消费者:[{}]完成执行", info.getKey());
            return true;
        } catch (Throwable e) {
            candidate.close();
            log.error("MQTT Client 消费者:[{}]刷新失败", info.getKey(), e);
            return false;
        }
    }

    public synchronized boolean replaceConsumerFromSnapshot(MqttClientDataSourceInfo info,
                                                            Supplier<List<MqttClientSubscribeScriptInfo>> snapshotSupplier) {
        if (this.destroyed) {
            log.warn("MQTT Client 数据源已销毁，拒绝刷新消费者:[{}]", info.getKey());
            return false;
        }
        List<MqttClientSubscribeScriptInfo> snapshot = snapshotSupplier == null ? null : snapshotSupplier.get();
        return replaceConsumer(info, snapshot);
    }

    public synchronized void deleteProducer(String datasourceKey) {
        if (StringUtils.isBlank(datasourceKey)) {
            return;
        }
        MqttClientProducer producer = this.producerMap.remove(datasourceKey);
        if (producer != null) {
            producer.close();
        }
    }

    public synchronized void deleteConsumer(String datasourceKey) {
        if (StringUtils.isBlank(datasourceKey)) {
            return;
        }
        MqttClientConsumer consumer = this.consumerMap.remove(datasourceKey);
        if (consumer != null) {
            consumer.close();
        }
    }

    public synchronized void unsubscribe(MqttClientSubscribeInfo subscribeInfo) {
        if (subscribeInfo == null || StringUtils.isBlank(subscribeInfo.getKey())) {
            return;
        }
        MqttClientConsumer consumer = this.consumerMap.get(subscribeInfo.getKey());
        if (consumer != null) {
            consumer.unsubscribe(subscribeInfo);
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
            log.debug("未找到默认 MQTT ClientTemplate，跳过默认 MQTT Client 数据源暴露", e);
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
    public synchronized void destroy() throws Exception {
        if (this.destroyed) {
            return;
        }
        this.destroyed = true;
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
