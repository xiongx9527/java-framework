package com.xiongx9527.fw.plus.boot.autoconfigure.mqtt.server.broker.listener;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.dromara.mica.mqtt.codec.MqttQoS;
import org.dromara.mica.mqtt.core.server.MqttServer;
import org.dromara.mica.mqtt.core.server.serializer.IMessageSerializer;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;

/**
 * Redis 集群消息监听器：反序列化集群消息后投递到本站点 MQTT 服务。
 *
 * <p>exchange（上行）通道消息会跳过来源节点自身（本站点已由发布管线完成本地投递，
 * 避免重复下发）；nodeName 为空时不做过滤（用于 down 下行通道）。</p>
 */
@Slf4j
public class RedisMessageListener implements MessageListener {

    /** 消息序列化器，用于将消息对象序列化为字节流或反序列化 */
    private final IMessageSerializer messageSerializer;

    /** MQTT服务器实例，用于处理MQTT协议相关的服务端功能 */
    private final MqttServer mqttServer;

    /** 本地节点名；非空时跳过本节点发出的集群消息 */
    private final String nodeName;

    /**
     * Redis消息监听器构造函数（下行通道，不做来源节点过滤）
     *
     * @param messageSerializer 消息序列化器，用于处理消息的序列化和反序列化
     * @param mqttServer MQTT服务器实例，用于处理MQTT协议相关的服务端功能
     */
    public RedisMessageListener(IMessageSerializer messageSerializer, MqttServer mqttServer) {
        this(messageSerializer, mqttServer, null);
    }

    /**
     * Redis消息监听器构造函数
     *
     * @param messageSerializer 消息序列化器，用于处理消息的序列化和反序列化
     * @param mqttServer MQTT服务器实例，用于处理MQTT协议相关的服务端功能
     * @param nodeName 本地节点名，非空时跳过本节点发出的集群消息
     */
    public RedisMessageListener(IMessageSerializer messageSerializer, MqttServer mqttServer, String nodeName) {
        this.messageSerializer = messageSerializer;
        this.mqttServer = mqttServer;
        this.nodeName = nodeName;
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
        org.dromara.mica.mqtt.core.server.model.Message mqttMessage = this.messageSerializer.deserialize(message.getBody());
        if (mqttMessage == null) {
            return;
        }
        // 集群上行消息：来源节点已由发布管线完成本地投递，本站点跳过，避免重复下发
        if (StringUtils.isNotBlank(this.nodeName) && StringUtils.equals(this.nodeName, mqttMessage.getNode())) {
            return;
        }
        // 投递到本站点设备
        String topic = mqttMessage.getTopic();
        if (StringUtils.isBlank(topic)) {
            log.error("Mqtt cluster message topic is blank.");
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
