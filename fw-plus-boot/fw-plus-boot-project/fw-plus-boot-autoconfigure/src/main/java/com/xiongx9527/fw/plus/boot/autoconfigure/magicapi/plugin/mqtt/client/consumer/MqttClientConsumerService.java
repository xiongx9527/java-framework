
package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.client.consumer;

import net.dreamlu.iot.mqtt.codec.MqttPublishMessage;
import net.dreamlu.iot.mqtt.codec.MqttQoS;

public interface MqttClientConsumerService {

    boolean consume(String dataSourceKey, String topic, MqttQoS qos, MqttPublishMessage message,byte[] payload);
}