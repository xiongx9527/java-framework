package com.xx9527.fw.plus.boot.autoconfigure.mqtt.client;

import net.dreamlu.iot.mqtt.spring.client.config.MqttClientProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
        prefix = MqttClientProperties.PREFIX,
        name = "enabled",
        havingValue = "true",
        matchIfMissing = false
)
@ConditionalOnClass(name = {"net.dreamlu.iot.mqtt.spring.client.MqttClientTemplate"})
public class MqttClientAutoConfiguration {
}
