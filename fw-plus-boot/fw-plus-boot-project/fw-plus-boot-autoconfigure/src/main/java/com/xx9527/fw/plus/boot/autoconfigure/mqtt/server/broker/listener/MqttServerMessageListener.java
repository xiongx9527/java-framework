package com.xx9527.fw.plus.boot.autoconfigure.mqtt.server.broker.listener;

import lombok.extern.slf4j.Slf4j;
import net.dreamlu.iot.mqtt.codec.MqttPublishMessage;
import net.dreamlu.iot.mqtt.codec.MqttQoS;
import net.dreamlu.iot.mqtt.core.server.event.IMqttMessageListener;
import org.tio.core.ChannelContext;

import java.nio.charset.StandardCharsets;

/**
 * 消息监听
 *
 * @author L.cm
 */
@Slf4j
public class MqttServerMessageListener implements IMqttMessageListener {

	/**
	 * 处理MQTT消息接收事件
	 *
	 * @param context Channel上下文对象，包含通道连接信息
	 * @param clientId 客户端唯一标识符
	 * @param topic 消息发布的主题
	 * @param qoS 消息服务质量等级
	 * @param message MQTT发布消息对象，包含消息内容和元数据
	 */
	@Override
	public void onMessage(ChannelContext context, String clientId, String topic, MqttQoS qoS, MqttPublishMessage message) {
		// 记录接收到的MQTT消息信息，包括客户端ID、消息对象和消息负载内容
		log.info("clientId:{} message:{} payload:{}", clientId, message, new String(message.getPayload(), StandardCharsets.UTF_8));
	}


}
