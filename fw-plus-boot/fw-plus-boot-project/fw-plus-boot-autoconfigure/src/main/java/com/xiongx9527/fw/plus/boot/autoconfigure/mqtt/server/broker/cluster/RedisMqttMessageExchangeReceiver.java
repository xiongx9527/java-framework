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
import net.dreamlu.iot.mqtt.core.server.MqttServer;
import net.dreamlu.iot.mqtt.core.server.cluster.MqttClusterMessageListener;
import net.dreamlu.iot.mqtt.core.server.serializer.IMessageSerializer;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * 监听集群消息，上行和内部集群通道
 *
 * @author L.cm
 */
public class RedisMqttMessageExchangeReceiver extends RedisMessageListenerContainer implements ApplicationContextAware, SmartInitializingSingleton {

	/**
	 * 应用程序上下文对象，用于管理Spring容器中的bean实例
	 * 该字段存储了当前应用程序的上下文环境，可以通过它获取容器中管理的各种bean
	 */
	private ApplicationContext applicationContext;

	/**
	 * 消息序列化器实例
	 * 用于处理消息的序列化和反序列化操作
	 */
	private final IMessageSerializer messageSerializer;

	/**
	 * MQTT集群消息监听器实例
	 * 用于监听和处理MQTT集群中的消息事件
	 */
	private final MqttClusterMessageListener clusterMessageListener;

	/**
	 * 通道名称，标识消息发送的目标通道
	 */
	private final String channel;


	/**
	 * Redis连接工厂实例
	 *
	 * 用于创建和管理Redis数据库连接的工厂对象，
	 * 提供了与Redis服务器建立连接的能力
	 */
	private RedisConnectionFactory redisConnectionFactory;

	/**
	 * 构造函数
	 * @param messageSerializer 消息序列化器，用于序列化和反序列化MQTT消息
	 * @param mqttServer MQTT服务器实例，用于处理集群消息
	 */
	public RedisMqttMessageExchangeReceiver(IMessageSerializer messageSerializer, MqttServer mqttServer, String channel) {
		this.messageSerializer = messageSerializer;
		this.clusterMessageListener = new MqttClusterMessageListener(mqttServer);
        this.channel = channel;
    }


	/**
	 * 构造函数，用于创建RedisMqttMessageExchangeReceiver实例
	 *
	 * @param messageSerializer 消息序列化器，用于序列化和反序列化消息
	 * @param mqttServer MQTT服务器实例，用于处理MQTT协议相关操作
	 * @param channel 通道名称，用于指定消息传输的通道
	 * @param redisConnectionFactory Redis连接工厂，用于创建Redis连接
	 */
	public RedisMqttMessageExchangeReceiver(IMessageSerializer messageSerializer, MqttServer mqttServer, String channel,RedisConnectionFactory redisConnectionFactory) {
		this.messageSerializer = messageSerializer;
		this.clusterMessageListener = new MqttClusterMessageListener(mqttServer);
		this.channel = channel;
		this.redisConnectionFactory = redisConnectionFactory;
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
	 * 主要用于初始化Redis连接工厂和消息监听器
	 */
	@Override
	public void afterSingletonsInstantiated() {
		// 如果Redis连接工厂未初始化，则从应用上下文中获取
		if (this.redisConnectionFactory == null) {
			this.redisConnectionFactory = this.applicationContext.getBean(RedisConnectionFactory.class);
		}
		this.setConnectionFactory(this.redisConnectionFactory);
		// 添加消息监听器，监听指定频道的消息
		this.addMessageListener(new RedisMessageListener(messageSerializer, null, clusterMessageListener),new ChannelTopic(channel));
	}

	/**
	 * MQTT消息上行接收处理方法
	 * 监听Redis指定频道的消息流，接收并处理MQTT消息
	 * @param mapRecord Redis流记录，包含频道名称和消息内容
	 */
	/**
	@RStreamListener(
			name = RedisKeys.REDIS_CHANNEL_EXCHANGE_KEY,
			messageModel = MessageModel.BROADCASTING,
			readRawBytes = true
	)
	public void mqttMessageUpReceiver(MapRecord<String, String, byte[]> mapRecord) {
		// 手动序列化和反序列化，避免 redis 序列化不一致问题
		Map<String, byte[]> recordValue = mapRecord.getValue();
		recordValue.forEach((key, messageBody) -> {
			// 手动序列化和反序列化，避免 redis 序列化不一致问题
			Message mqttMessage = this.messageSerializer.deserialize(messageBody);
			if (mqttMessage == null) {
				return;
			}
			this.clusterMessageListener.onMessage(mqttMessage);
		});
	}
	**/



}
