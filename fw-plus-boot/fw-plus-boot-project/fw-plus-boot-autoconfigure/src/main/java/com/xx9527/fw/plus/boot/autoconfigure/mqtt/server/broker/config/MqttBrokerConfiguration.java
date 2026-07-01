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

package com.xx9527.fw.plus.boot.autoconfigure.mqtt.server.broker.config;

import com.xx9527.fw.plus.boot.autoconfigure.mqtt.server.broker.cluster.*;
import com.xx9527.fw.plus.boot.autoconfigure.mqtt.server.broker.enums.RedisKeys;
import com.xx9527.fw.plus.boot.autoconfigure.mqtt.server.broker.listener.MqttServerMessageListener;
import com.xx9527.fw.plus.boot.autoconfigure.mqtt.server.broker.listener.RedisMqttConnectStatusListener;
import com.xx9527.fw.plus.boot.autoconfigure.mqtt.server.broker.service.IMqttBrokerService;
import com.xx9527.fw.plus.boot.autoconfigure.mqtt.server.broker.service.impl.MqttBrokerServiceImpl;
import net.dreamlu.iot.mqtt.core.server.MqttServer;
import net.dreamlu.iot.mqtt.core.server.dispatcher.IMqttMessageDispatcher;
import net.dreamlu.iot.mqtt.core.server.event.IMqttConnectStatusListener;
import net.dreamlu.iot.mqtt.core.server.event.IMqttMessageListener;
import net.dreamlu.iot.mqtt.core.server.serializer.IMessageSerializer;
import net.dreamlu.iot.mqtt.core.server.serializer.JsonMessageSerializer;
import net.dreamlu.iot.mqtt.core.server.store.IMqttMessageStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * mica mqtt broker 配置
 *
 * @author L.cm
 */
@Configuration(proxyBeanMethods = false)
public class MqttBrokerConfiguration {

	/**
	 * 创建消息序列化器Bean
	 * @return IMessageSerializer 消息序列化器实例
	 */
	@Bean
	public IMessageSerializer messageSerializer() {
		return new JsonMessageSerializer();
	}

	/**
	 * 创建MQTT连接状态监听器Bean
	 * @return IMqttConnectStatusListener MQTT连接状态监听器实例
	 */
	@Bean
	public IMqttConnectStatusListener mqttBrokerConnectListener() {
		return new RedisMqttConnectStatusListener();
	}

	/**
	 * 创建MQTT消息存储Bean
	 * @param messageSerializer 消息序列化器
	 * @return IMqttMessageStore MQTT消息存储实例
	 */
	@Bean
	public IMqttMessageStore mqttMessageStore(IMessageSerializer messageSerializer) {
		return new RedisMqttMessageStore(messageSerializer);
	}

	/**
	 * 创建Redis MQTT上行消息接收器Bean
	 * @param messageSerializer 消息序列化器
	 * @param mqttServer MQTT服务器实例
	 * @return RedisMqttMessageExchangeReceiver Redis MQTT上行消息接收器实例
	 */
	@Bean
	public RedisMqttMessageExchangeReceiver mqttMessageUpReceiver(IMessageSerializer messageSerializer,MqttServer mqttServer) {
		return new RedisMqttMessageExchangeReceiver(messageSerializer, mqttServer, RedisKeys.REDIS_CHANNEL_EXCHANGE.getKey());
	}

	/**
	 * 创建Redis MQTT下行消息接收器Bean
	 * @param messageSerializer 消息序列化器
	 * @param mqttServer MQTT服务器实例
	 * @return RedisMqttMessageDownReceiver Redis MQTT下行消息接收器实例
	 */
	@Bean
	public RedisMqttMessageDownReceiver mqttMessageDownReceiver(IMessageSerializer messageSerializer,MqttServer mqttServer) {
		return new RedisMqttMessageDownReceiver(messageSerializer, mqttServer,RedisKeys.REDIS_CHANNEL_DOWN.getKey());
	}

	/**
	 * 创建MQTT消息分发器Bean
	 * @param messageSerializer 消息序列化器
	 * @return IMqttMessageDispatcher MQTT消息分发器实例
	 */
	@Bean
	public IMqttMessageDispatcher mqttMessageDispatcher(IMessageSerializer messageSerializer) {
		return new RedisMqttMessageDispatcher(messageSerializer, RedisKeys.REDIS_CHANNEL_EXCHANGE.getKey());
	}

	/**
	 * 创建Redis MQTT服务器管理Bean
	 * @param mqttServer MQTT服务器实例
	 * @return RedisMqttServerManage Redis MQTT服务器管理实例
	 */
	@Bean
	public RedisMqttServerManage mqttServerManage(MqttServer mqttServer) {
		return new RedisMqttServerManage(mqttServer);
	}

	/**
	 * 创建并配置MQTT代理服务Bean
	 *
	 * @return 返回MQTT代理服务实例，用于处理MQTT协议相关的业务逻辑
	 */
	@Bean
	public IMqttBrokerService mqttBrokerService() {
		return new MqttBrokerServiceImpl();
	}

	/**
	 * 创建MQTT消息监听器Bean，当容器中不存在该Bean时才创建
	 * @return IMqttMessageListener MQTT消息监听器实例
	 */
	@Bean
	@ConditionalOnMissingBean
	public IMqttMessageListener mqttMessageListener() {
		return new MqttServerMessageListener();
	}

}
