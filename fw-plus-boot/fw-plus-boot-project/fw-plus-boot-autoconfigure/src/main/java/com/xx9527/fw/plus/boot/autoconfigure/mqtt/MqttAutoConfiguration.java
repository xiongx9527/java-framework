package com.xx9527.fw.plus.boot.autoconfigure.mqtt;

import com.xx9527.fw.plus.boot.autoconfigure.mqtt.client.MqttClientAutoConfiguration;
import com.xx9527.fw.plus.boot.autoconfigure.mqtt.server.MqttServerAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({MqttServerAutoConfiguration.class, MqttClientAutoConfiguration.class})
public class MqttAutoConfiguration {
}
