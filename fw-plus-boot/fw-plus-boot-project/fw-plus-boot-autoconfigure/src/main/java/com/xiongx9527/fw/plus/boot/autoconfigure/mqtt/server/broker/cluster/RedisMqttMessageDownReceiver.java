/*
 * Copyright (c) 2019-2029, Dreamlu 卢春梦 (596392912@qq.com & dreamlu.net).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.xiongx9527.fw.plus.boot.autoconfigure.mqtt.server.broker.cluster;

import com.xiongx9527.fw.plus.boot.autoconfigure.mqtt.server.broker.listener.RedisMessageListener;
import lombok.extern.slf4j.Slf4j;
import org.dromara.mica.mqtt.core.server.MqttServer;
import org.dromara.mica.mqtt.core.server.serializer.IMessageSerializer;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * 下行消息，发送到设备
 *
 * @author L.cm
 */
@Slf4j
public class RedisMqttMessageDownReceiver extends RedisMessageListenerContainer implements ApplicationContextAware  , SmartInitializingSingleton {

	/**
	 * 应用程序上下文对象，用于管理Spring容器中的bean实例
	 * 该字段存储了当前应用程序的上下文环境，可以通过它获取容器中管理的各种bean
	 */
	private ApplicationContext applicationContext;

	/** 消息序列化器，用于将消息对象序列化为字节流或反序列化 */
	private final IMessageSerializer messageSerializer;

	/** MQTT服务器实例，用于处理MQTT协议相关的服务端功能 */
	private final MqttServer mqttServer;

	/**
	 * 通道名称，标识消息发送的目标通道
	 */
	private final String channel;

	/**
	 * Redis连接工厂实例
	 *
	 * 用于创建和管理Redis数据库连接的工厂对象，
	 * 通过该工厂可以获取Redis连接并进行相关操作
	 */
	private RedisConnectionFactory redisConnectionFactory;

	/**
	 * 构造函数
	 * @param messageSerializer 消息序列化器，用于序列化和反序列化MQTT消息
	 * @param mqttServer MQTT服务器实例，用于发布消息到设备
	 */
	public RedisMqttMessageDownReceiver(IMessageSerializer messageSerializer, MqttServer mqttServer, String channel) {
		this.messageSerializer = messageSerializer;
		this.mqttServer = mqttServer;
        this.channel = channel;
    }


	/**
	 * RedisMQTT消息下行接收器构造函数
	 *
	 * @param messageSerializer 消息序列化器，用于处理消息的序列化和反序列化
	 * @param mqttServer MQTT服务器实例，用于管理MQTT连接和消息发布
	 * @param channel Redis订阅频道名称，指定监听的Redis频道
	 * @param redisConnectionFactory Redis连接工厂，用于创建和管理Redis连接
	 */
	public RedisMqttMessageDownReceiver(IMessageSerializer messageSerializer, MqttServer mqttServer, String channel,RedisConnectionFactory redisConnectionFactory) {
		this.messageSerializer = messageSerializer;
		this.mqttServer = mqttServer;
		this.channel = channel;
		this.redisConnectionFactory = redisConnectionFactory;
		this.setConnectionFactory(redisConnectionFactory);
	}

	/**
	 * 设置应用程序上下文
	 *
	 * @param applicationContext 应用程序上下文对象
	 * @throws BeansException bean异常
	 */
	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	/**
	 * 在单例bean实例化完成后执行的回调方法
	 * 主要用于初始化Redis连接工厂和添加消息监听器
	 */
	@Override
	public void afterSingletonsInstantiated() {
		// 如果Redis连接工厂为空，则从应用上下文中获取
		if (this.redisConnectionFactory == null && this.applicationContext != null) {
			this.redisConnectionFactory = this.applicationContext.getBean(RedisConnectionFactory.class);
		}
		if (this.redisConnectionFactory == null) {
			throw new IllegalStateException("No RedisConnectionFactory available for MQTT Redis receiver.");
		}
		this.setConnectionFactory(this.redisConnectionFactory);
		// 添加Redis消息监听器，监听指定频道的消息（下行通道不做来源节点过滤）
		this.addMessageListener(new RedisMessageListener(messageSerializer, mqttServer),new ChannelTopic(channel));
	}


	/**
	 * MQTT下行消息接收处理方法
	 * 监听Redis流中的下行消息，反序列化后发布到对应的MQTT主题
	 * @param mapRecord Redis流记录，包含消息主题和消息体的映射关系
	 */
	/**
	@RStreamListener(
		 name = RedisKeys.REDIS_CHANNEL_DOWN_KEY,
		 messageModel = MessageModel.BROADCASTING,
		 readRawBytes = true
	)
	public void mqttMessageDownReceiver(MapRecord<String, String, byte[]> mapRecord) {
		// 手动序列化和反序列化，避免 redis 序列化不一致问题
		Map<String, byte[]> recordValue = mapRecord.getValue();
		recordValue.forEach((key, messageBody) -> {
			// 手动序列化和反序列化，避免 redis 序列化不一致问题
			Message mqttMessage = this.messageSerializer.deserialize(messageBody);
			if (mqttMessage == null) {
				return;
			}
			// 下行消息，发送到设备
			String topic = mqttMessage.getTopic();
			if (StringUtils.isBlank(topic)) {
				log.error("Mqtt down stream topic is blank.");
				return;
			}
			String clientId = mqttMessage.getClientId();
			byte[] payload = mqttMessage.getPayload();
			MqttQoS mqttQoS = MqttQoS.valueOf(mqttMessage.getQos());
			boolean retain = mqttMessage.isRetain();
			if (StringUtils.isBlank(clientId)) {
				this.mqttServer.publishAll(topic, payload, mqttQoS, retain);
			} else {
				this.mqttServer.publish(clientId, topic, payload, mqttQoS, retain);
			}
		});
	}
	**/



}
