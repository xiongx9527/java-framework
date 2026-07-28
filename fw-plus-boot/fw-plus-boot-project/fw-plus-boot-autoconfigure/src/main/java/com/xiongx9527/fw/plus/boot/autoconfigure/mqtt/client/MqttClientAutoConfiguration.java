package com.xiongx9527.fw.plus.boot.autoconfigure.mqtt.client;

import net.dreamlu.iot.mqtt.spring.client.MqttClientTemplate;
import net.dreamlu.iot.mqtt.spring.client.config.MqttClientConfiguration;
import net.dreamlu.iot.mqtt.spring.client.config.MqttClientProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
        prefix = MqttClientProperties.PREFIX,
        name = "enabled",
        havingValue = "true",
        matchIfMissing = false
)
@ConditionalOnClass(MqttClientTemplate.class)
@Import(MqttClientConfiguration.class)
public class MqttClientAutoConfiguration {
}
