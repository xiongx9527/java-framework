package com.xx9527.fw.plus.boot.autoconfigure.mqtt.server.broker.listener;

import lombok.extern.slf4j.Slf4j;
import net.dreamlu.iot.mqtt.codec.MqttQoS;
import net.dreamlu.iot.mqtt.core.server.MqttServer;
import net.dreamlu.iot.mqtt.core.server.cluster.MqttClusterMessageListener;
import net.dreamlu.iot.mqtt.core.server.serializer.IMessageSerializer;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;

@Slf4j
public class RedisMessageListener implements MessageListener {

    /** 消息序列化器，用于将消息对象序列化为字节流或反序列化 */
    private final IMessageSerializer messageSerializer;

    /** MQTT服务器实例，用于处理MQTT协议相关的服务端功能 */
    private MqttServer mqttServer;

    /**通道名称，标识消息发送的目标通道*/
    private MqttClusterMessageListener messageListener;

    /**
     * Redis消息监听器构造函数
     * @param messageSerializer 消息序列化器，用于处理消息的序列化和反序列化
     * @param mqttServer MQTT服务器实例，用于处理MQTT协议相关的服务端功能
     */
    public RedisMessageListener(IMessageSerializer messageSerializer, MqttServer mqttServer, MqttClusterMessageListener messageListener) {
        this.messageSerializer = messageSerializer;
        this.mqttServer = mqttServer;
        this.messageListener = messageListener;
    }

    /**
     * 处理MQTT消息的回调方法
     *
     * @param message 接收到的MQTT消息对象，包含消息体等信息
     * @param pattern 消息匹配的模式字节数组
     */
    @Override
    public void onMessage(Message message, byte[] pattern) {
        // 反序列化MQTT消息
        net.dreamlu.iot.mqtt.core.server.model.Message mqttMessage = this.messageSerializer.deserialize(message.getBody());
        if (mqttMessage == null) {
            return;
        }
        // 根据配置处理消息：优先使用消息监听器，否则使用MQTT服务器直接发送
        if (null != this.messageListener) {
            this.messageListener.onMessage(mqttMessage);
        } else if (null != this.mqttServer) {
            // 下行消息，发送到设备
            String topic = mqttMessage.getTopic();
            if (StringUtils.isBlank(topic)) {
                log.error("Mqtt down stream topic is blank.");
                return;
            }
            // 获取MQTT客户端ID
            String clientId = mqttMessage.getClientId();
            // 获取消息负载数据
            byte[] payload = mqttMessage.getPayload();
            // 获取消息服务质量等级
            MqttQoS mqttQoS = MqttQoS.valueOf(mqttMessage.getQos());
            // 获取消息保留标志
            boolean retain = mqttMessage.isRetain();
            // 根据是否有指定客户端ID来决定是向所有客户端发布还是指定客户端发布
            if (StringUtils.isBlank(clientId)) {
                this.mqttServer.publishAll(topic, payload, mqttQoS, retain);
            } else {
                this.mqttServer.publish(clientId, topic, payload, mqttQoS, retain);
            }
        }
    }

}
