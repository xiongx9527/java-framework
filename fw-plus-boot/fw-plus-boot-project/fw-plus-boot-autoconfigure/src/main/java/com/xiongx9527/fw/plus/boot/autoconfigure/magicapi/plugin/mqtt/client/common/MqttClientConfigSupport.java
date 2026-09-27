package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.client.common;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import net.dreamlu.iot.mqtt.codec.MqttQoS;
import net.dreamlu.iot.mqtt.core.client.MqttClientCreator;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;

@Slf4j
public final class MqttClientConfigSupport {

    private static final int DEFAULT_MQTT_PORT = 1883;
    private static final int MAX_CLIENT_ID_LENGTH = 64;

    private MqttClientConfigSupport() {
    }

    public static int parsePort(String port) {
        try {
            int parsedPort = Integer.parseInt(StringUtils.trim(port));
            if (parsedPort >= 1 && parsedPort <= 65535) {
                return parsedPort;
            }
        } catch (Exception ignored) {
            // 缺失、格式错误和越界端口统一回退到 MQTT 默认端口。
        }
        return DEFAULT_MQTT_PORT;
    }

    public static MqttQoS parseQos(String qos) {
        try {
            int qosInt = Integer.parseInt(StringUtils.trim(qos));
            if (qosInt == 1) {
                return MqttQoS.QOS1;
            }
            if (qosInt == 2) {
                return MqttQoS.QOS2;
            }
        } catch (Exception ignored) {
            // 非法 QoS 保持旧行为，按 QOS0 处理。
        }
        return MqttQoS.QOS0;
    }

    public static String buildSubscribeClientId(String clientId, String dataSourceKey, String topic) {
        String base = StringUtils.defaultIfBlank(clientId, dataSourceKey);
        base = StringUtils.defaultIfBlank(base, "mqttclient");
        String topicPart = StringUtils.defaultIfBlank(topic, "topic")
                .replaceAll("[^a-zA-Z0-9_-]", "_");
        String value = base + "-sub-" + topicPart;
        if (value.length() <= MAX_CLIENT_ID_LENGTH) {
            return value;
        }
        String hash = Integer.toHexString(value.hashCode());
        int keepLength = Math.max(1, MAX_CLIENT_ID_LENGTH - hash.length() - 1);
        return value.substring(0, keepLength) + "-" + hash;
    }

    public static void applyCommonOptions(MqttClientCreator mqttClientCreator, String other) {
        if (StringUtils.isBlank(other)) {
            return;
        }
        try {
            applyCommonOptions(mqttClientCreator, JSON.parseObject(other));
        } catch (Exception e) {
            log.warn("解析 MQTT Client 订阅扩展配置失败: {}", e.getMessage(), e);
        }
    }

    public static void applyCommonOptions(MqttClientCreator mqttClientCreator, Map<String, Object> properties) {
        if (properties == null || properties.isEmpty()) {
            return;
        }
        try {
            JSONObject options = new JSONObject(properties);
            if (options.containsKey("keepAliveSec")) {
                mqttClientCreator.keepAliveSecs(options.getIntValue("keepAliveSec"));
            }
            if (options.containsKey("reconnect")) {
                mqttClientCreator.reconnect(options.getBooleanValue("reconnect"));
            }
            if (options.containsKey("reIntervalSec")) {
                mqttClientCreator.reInterval(options.getIntValue("reIntervalSec"));
            }
            if (options.containsKey("readBufferSize")) {
                mqttClientCreator.readBufferSize(options.getIntValue("readBufferSize"));
            }
            if (options.containsKey("maxBytesInMessage")) {
                mqttClientCreator.maxBytesInMessage(options.getIntValue("maxBytesInMessage"));
            }
            if (options.containsKey("cleanSession")) {
                mqttClientCreator.cleanSession(options.getBooleanValue("cleanSession"));
            }
            if (options.containsKey("ssl")) {
                log.warn("MQTT Client ssl 已配置，但当前插件尚未根据该字段创建完整 SSL 配置");
            }
        } catch (Exception e) {
            log.warn("应用 MQTT Client properties 失败: {}", e.getMessage(), e);
        }
    }
}
