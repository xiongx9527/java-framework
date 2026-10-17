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

import net.dreamlu.mica.redis.stream.DefaultRStreamTemplate;
import net.dreamlu.mica.redis.stream.RStreamTemplate;
import org.apache.commons.collections4.MapUtils;
import org.dromara.mica.mqtt.core.server.enums.MessageType;
import org.dromara.mica.mqtt.core.server.model.Message;
import org.dromara.mica.mqtt.core.server.pipeline.MqttPublishPipelineHandler;
import org.dromara.mica.mqtt.core.server.pipeline.PublishContext;
import org.dromara.mica.mqtt.core.server.serializer.IMessageSerializer;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Map;
import java.util.Objects;

/**
 * redis 消息转发器（发布管线处理器）。
 *
 * <p>mica-mqtt 2.6.x 移除了 {@code IMqttMessageDispatcher}，发布流程改为发布管线。
 * 本类实现 {@link MqttPublishPipelineHandler}，在本地投递（Remain/MessageListener/
 * SubscriptionForward，order 100/200/300）之后，把客户端上行 PUBLISH 转发到 Redis
 * 集群通道，由其它节点消费并投递给各自本地的订阅者。</p>
 *
 * @author L.cm
 */
public class RedisMqttMessageDispatcher implements MqttPublishPipelineHandler, ApplicationContextAware, SmartInitializingSingleton {

	/**
	 * 转发顺序：必须晚于本地投递处理器（SubscriptionForwardHandler order=300），
	 * 确保本地订阅者的投递不依赖 Redis 的可用性。
	 */
	private static final int ORDER = 1000;

	/**
	 * 应用程序上下文实例，用于获取Spring容器中的Bean和服务
	 */
	private ApplicationContext context;

	/**
	 * Redis流模板，用于操作Redis流数据结构
	 */
	private RStreamTemplate streamTemplate;

	/**
	 * 消息序列化器，用于将消息对象序列化为字节数组或反序列化
	 */
	private final IMessageSerializer messageSerializer;

	/**
	 * 通道名称，标识消息发送的目标通道
	 */
	private final String channel;

	/**
	 * 构造函数，初始化Redis MQTT消息分发器
	 *
	 * @param messageSerializer 消息序列化器，用于序列化和反序列化消息
	 * @param channel Redis发布/订阅频道名称，不能为null
	 * @throws NullPointerException 当channel参数为null时抛出
	 */
	public RedisMqttMessageDispatcher(IMessageSerializer messageSerializer, String channel) {
		this.messageSerializer = messageSerializer;
		this.channel = Objects.requireNonNull(channel, "Redis pub/sub channel is null.");
	}

	/**
	 * 构造Redis MQTT消息分发器
	 *
	 * @param streamTemplate Redis Stream模板，用于消息的发送操作
	 * @param messageSerializer 消息序列化器，用于将消息对象序列化为字节数组
	 * @param channel Redis Stream的通道名称，不能为空
	 * @throws NullPointerException 当channel参数为null时抛出
	 */
	public RedisMqttMessageDispatcher(RStreamTemplate streamTemplate, IMessageSerializer messageSerializer, String channel) {
		this.streamTemplate = streamTemplate;
		this.messageSerializer = messageSerializer;
		this.channel = Objects.requireNonNull(channel, "Redis pub/sub channel is null.");
	}

	/**
	 * 发布管线处理：把客户端上行 PUBLISH 组装为集群消息并转发到 Redis。
	 *
	 * @param context 发布上下文
	 * @return 始终返回 true（不中断后续处理器）
	 */
	@Override
	public boolean handle(PublishContext context) {
		Message message = new Message();
		message.setNode(context.getNodeName());
		message.setMessageType(MessageType.UP_STREAM);
		message.setFromClientId(context.getClientId());
		message.setFromUsername(context.getUsername());
		message.setTopic(context.getTopic());
		message.setQos(context.getQos().value());
		message.setRetain(context.isRetain());
		message.setPayload(context.getPayload());
		message.setPeerHost(context.getPeerHost());
		message.setTimestamp(context.getTimestamp());
		message.setPublishReceivedAt(context.getPublishReceivedAt());
		return send(message);
	}

	@Override
	public int getOrder() {
		return ORDER;
	}

	/**
	 * 发送MQTT消息到Redis Stream
	 * 手动序列化和反序列化，避免 redis 序列化不一致问题
	 *
	 * @param message 要发送的MQTT消息对象，不能为空
	 * @return 总是返回true，表示消息发送成功
	 */
	public boolean send(Message message) {
		// 手动序列化和反序列化，避免 redis 序列化不一致问题
		String topic = message.getTopic();
		String key = topic == null ? message.getClientId() : topic;
		long maxLen = 10_0000;
		this.streamTemplate.send(this.channel, key, message, this.messageSerializer::serialize, maxLen);
		return true;
	}

	/**
	 * 设置应用程序上下文
	 *
	 * @param applicationContext 应用程序上下文对象
	 * @throws BeansException 当设置上下文过程中发生错误时抛出
	 */
	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.context = applicationContext;
	}

	/**
	 * 在所有单例bean实例化完成后执行的回调方法
	 * 用于初始化streamTemplate，如果尚未设置则从应用上下文中获取RStreamTemplate bean
	 */
	@Override
	public void afterSingletonsInstantiated() {
		// 如果streamTemplate未初始化，则从上下文中获取RStreamTemplate bean进行赋值
		if (this.streamTemplate == null && this.context != null) {
			try {
				this.streamTemplate = this.context.getBean(RStreamTemplate.class);
			} catch (Exception e) {
				Map<String, RedisTemplate> map = MapUtils.emptyIfNull(this.context.getBeansOfType(RedisTemplate.class));
				for (RedisTemplate redisTemplate : map.values()) {
					if (!(redisTemplate instanceof StringRedisTemplate)) {
						this.streamTemplate = new DefaultRStreamTemplate(redisTemplate);
						break;
					}
				}
			}
		}
		if (this.streamTemplate == null) {
			throw new IllegalStateException("No RStreamTemplate or non-String RedisTemplate available for MQTT Redis dispatcher.");
		}
	}

}
