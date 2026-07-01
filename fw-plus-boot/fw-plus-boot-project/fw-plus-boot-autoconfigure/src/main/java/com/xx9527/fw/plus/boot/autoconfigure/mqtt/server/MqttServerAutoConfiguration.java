package com.xx9527.fw.plus.boot.autoconfigure.mqtt.server;

import com.xx9527.fw.plus.boot.autoconfigure.mqtt.server.broker.MqttServerBrokerConfiguration;
import net.dreamlu.iot.mqtt.spring.server.config.MqttServerProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
        prefix = MqttServerProperties.PREFIX,
        name = "enabled",
        havingValue = "true",
        matchIfMissing = false
)
@ConditionalOnClass(name = {"net.dreamlu.iot.mqtt.spring.server.MqttServerTemplate"})
@Import({MqttServerConfiguration.class, MqttServerBrokerConfiguration.class})
public class MqttServerAutoConfiguration {
}
