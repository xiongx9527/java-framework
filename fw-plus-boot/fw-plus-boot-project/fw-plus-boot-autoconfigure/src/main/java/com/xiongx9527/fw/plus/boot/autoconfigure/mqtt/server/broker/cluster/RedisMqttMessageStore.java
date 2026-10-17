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

import com.xiongx9527.fw.plus.boot.autoconfigure.mqtt.server.broker.enums.RedisKeys;
import com.xiongx9527.fw.plus.boot.autoconfigure.mqtt.server.broker.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import net.dreamlu.mica.redis.cache.CacheKey;
import net.dreamlu.mica.redis.cache.MicaRedisCache;
import org.dromara.mica.mqtt.core.server.model.Message;
import org.dromara.mica.mqtt.core.server.serializer.IMessageSerializer;
import org.dromara.mica.mqtt.core.server.store.IMqttMessageStore;
import org.dromara.mica.mqtt.core.util.TopicUtil;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * redis mqtt 遗嘱和保留消息存储
 *
 * @author L.cm
 */
@RequiredArgsConstructor
public class RedisMqttMessageStore implements IMqttMessageStore , ApplicationContextAware , SmartInitializingSingleton {

	/**
	 * 应用程序上下文实例，用于获取Spring容器中的Bean和服务
	 */
	private ApplicationContext context;

	/**
	 * Redis缓存实例，用于数据缓存操作
	 */
	private MicaRedisCache redisCache;

	/**
	 * 消息序列化器，用于消息的序列化和反序列化处理
	 */
	private final IMessageSerializer messageSerializer;

	/**
	 * Redis键名字段
	 *
	 * 用于存储Redis中某个特定数据项的键名标识
	 */
	private String redisKey ;

	/**
	 * 构造函数，用于创建RedisMqttMessageStore实例
	 * @param messageSerializer 消息序列化器，用于序列化和反序列化MQTT消息
	 * @param redisKey Redis键名，用于存储MQTT消息的键值标识
	 */
	public RedisMqttMessageStore(IMessageSerializer messageSerializer, String redisKey) {
		this(messageSerializer, null, redisKey);
	}

	/**
	 * 构造函数，用于创建RedisMqttMessageStore实例
	 * @param messageSerializer 消息序列化器，用于序列化和反序列化MQTT消息
	 * @param redisCache Redis缓存实例，用于操作Redis数据库
	 */
	public RedisMqttMessageStore(IMessageSerializer messageSerializer, MicaRedisCache redisCache) {
		this(messageSerializer, redisCache, null);
	}

	/**
	 * 构造函数，用于创建RedisMqttMessageStore实例
	 * @param messageSerializer 消息序列化器，用于序列化和反序列化MQTT消息
	 * @param redisCache Redis缓存实例，用于操作Redis数据库
	 * @param redisKey Redis键名，用于存储MQTT消息的键值标识
	 */
	public RedisMqttMessageStore(IMessageSerializer messageSerializer, MicaRedisCache redisCache, String redisKey) {
		this.redisCache = redisCache;
		this.messageSerializer = messageSerializer;
		this.redisKey = redisKey;
	}

	/**
	 * 添加遗嘱消息
	 *
	 * @param clientId 客户端ID
	 * @param message 遗嘱消息对象
	 * @return 操作结果，始终返回true
	 */
	@Override
	public boolean addWillMessage(String clientId, Message message) {
		this.redisCache.set(RedisKeys.MESSAGE_STORE_WILL.getKey(clientId), message, messageSerializer::serialize);
		return true;
	}

	/**
	 * 清除遗嘱消息
	 *
	 * @param clientId 客户端ID
	 * @return 操作结果，始终返回true
	 */
	@Override
	public boolean clearWillMessage(String clientId) {
		this.redisCache.del(RedisKeys.MESSAGE_STORE_WILL.getKey(clientId));
		return true;
	}

	/**
	 * 获取遗嘱消息
	 *
	 * @param clientId 客户端ID
	 * @return 遗嘱消息对象，如果不存在则返回null
	 */
	@Override
	public Message getWillMessage(String clientId) {
		return this.redisCache.get(RedisKeys.MESSAGE_STORE_WILL.getKey(clientId), messageSerializer::deserialize);
	}

	/**
	 * 添加保留消息
	 *
	 * @param topic 主题
	 * @param timeout 保留消息过期时间（秒），小于等于 0 表示不过期
	 * @param message 保留消息对象
	 * @return 操作结果，始终返回true
	 */
	@Override
	public boolean addRetainMessage(String topic, int timeout, Message message) {
		String key = RedisKeys.MESSAGE_STORE_RETAIN.getKey(topic);
		if (timeout > 0) {
			this.redisCache.set(new CacheKey(key, Duration.ofSeconds(timeout)), message, messageSerializer::serialize);
		} else {
			this.redisCache.set(key, message, messageSerializer::serialize);
		}
		return true;
	}

	/**
	 * 清除保留消息
	 *
	 * @param topic 主题
	 * @return 操作结果，始终返回true
	 */
	@Override
	public boolean clearRetainMessage(String topic) {
		this.redisCache.del(RedisKeys.MESSAGE_STORE_RETAIN.getKey(topic));
		return true;
	}

	/**
	 * 获取匹配主题过滤器的保留消息列表
	 *
	 * @param topicFilter 主题过滤器
	 * @return 匹配的保留消息列表
	 */
	@Override
	public List<Message> getRetainMessage(String topicFilter) {
		List<Message> retainMessageList = new ArrayList<>();
		RedisKeys redisKey = RedisKeys.MESSAGE_STORE_RETAIN;
		String redisKeyPrefix = redisKey.getKey();
		String redisKeyPattern = redisKeyPrefix.concat(RedisUtil.getTopicPattern(topicFilter));
		int keyPrefixLength = redisKeyPrefix.length();

		// 扫描匹配的Redis键并筛选符合主题过滤器的消息
		this.redisCache.scan(redisKeyPattern, (key) -> {
			String keySuffix = key.substring(keyPrefixLength);
			if (TopicUtil.match(topicFilter, keySuffix)) {
				Message message = this.redisCache.get(key, messageSerializer::deserialize);
				if (message != null) {
					retainMessageList.add(message);
				}
			}
		});
		return retainMessageList;
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
	 * 在单例bean实例化完成后执行的回调方法
	 * 用于初始化redis缓存实例，如果redisCache尚未初始化，则从应用上下文中获取MicaRedisCache bean
	 */
	@Override
	public void afterSingletonsInstantiated() {
		// 延迟初始化redis缓存实例
		if (this.redisCache == null) {
			this.redisCache = this.context.getBean(MicaRedisCache.class);
		}
	}

}
