
package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.client.consumer;

import org.dromara.mica.mqtt.codec.message.MqttPublishMessage;
import org.dromara.mica.mqtt.codec.MqttQoS;

public interface MqttClientConsumerService {

    boolean consume(String dataSourceKey, String topic, MqttQoS qos, MqttPublishMessage message,byte[] payload);
}