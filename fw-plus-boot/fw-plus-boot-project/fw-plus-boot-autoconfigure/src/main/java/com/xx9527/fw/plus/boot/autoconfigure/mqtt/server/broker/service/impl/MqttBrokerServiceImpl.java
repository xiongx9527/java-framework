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

package com.xx9527.fw.plus.boot.autoconfigure.mqtt.server.broker.service.impl;

import com.xx9527.fw.plus.boot.autoconfigure.mqtt.server.broker.enums.RedisKeys;
import com.xx9527.fw.plus.boot.autoconfigure.mqtt.server.model.ServerNode;
import com.xx9527.fw.plus.boot.autoconfigure.mqtt.server.broker.service.IMqttBrokerService;
import net.dreamlu.mica.core.utils.StringPool;
import net.dreamlu.mica.redis.cache.MicaRedisCache;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * mqtt broker 服务
 *
 * @author L.cm
 */
public class MqttBrokerServiceImpl implements IMqttBrokerService , ApplicationContextAware , SmartInitializingSingleton {

	/**
	 * 应用程序上下文实例，用于获取Spring容器中的Bean和服务
	 */
	private ApplicationContext context;

	/**
	 * Redis缓存实例
	 *
	 * 用于提供Redis缓存功能的实例变量，通过该实例可以对Redis进行各种缓存操作。
	 * 该变量被声明为private final，确保只能在当前类中访问且初始化后不可修改。
	 */
	private MicaRedisCache redisCache;

	/**
	 * Redis键名字段
	 *
	 * 用于存储Redis中数据项的键名标识
	 */
	private String redisKey;

	/**
	 * 默认构造函数，用于创建MqttBrokerServiceImpl实例
	 */
	public MqttBrokerServiceImpl() {
	}

	/**
	 * 带redisKey参数的构造函数，用于创建MqttBrokerServiceImpl实例并初始化redisKey
	 *
	 * @param redisKey Redis键值，用于标识缓存中的MQTT代理信息
	 */
	public MqttBrokerServiceImpl(String redisKey) {
		this.redisKey = redisKey;
	}

	/**
	 * 带redisCache参数的构造函数，用于创建MqttBrokerServiceImpl实例并初始化redisCache
	 *
	 * @param redisCache Redis缓存实例，用于操作MQTT代理相关的缓存数据
	 */
	public MqttBrokerServiceImpl(MicaRedisCache redisCache) {
		this.redisCache = redisCache;
	}

	/**
	 * MQTT Broker服务实现类的构造函数
	 *
	 * @param redisCache Redis缓存实例，用于存储和获取MQTT相关的缓存数据
	 */
	public MqttBrokerServiceImpl(MicaRedisCache redisCache,String redisKey) {
		this.redisCache = redisCache;
		this.redisKey = redisKey;
	}

	/**
	 * 获取Redis键值
	 *
	 * @param redisKeyEnum Redis键枚举对象，用于获取键前缀
	 * @param key 键名或键后缀
	 * @return 组合后的Redis键值
	 */
	private String getRedisKey(RedisKeys redisKeyEnum,String key) {
		// 如果当前对象的redisKey属性不为空白，则将其作为前缀与传入的key组合
		if (StringUtils.isNotBlank(this.redisKey)) {
			return redisKeyEnum.getKey(this.redisKey) + key;
		}
		// 否则直接使用枚举对象获取键值
		return redisKeyEnum.getKey(key);
	}

    /**
	 * 获取所有服务节点信息
	 *
	 * @return 服务节点列表，每个节点包含节点名称和对应的数据
	 */
	@Override
	public List<ServerNode> getNodes() {
		// 扫描所有服务节点键
		Set<String> nodeKeySet = this.redisCache.scan(getRedisKey(RedisKeys.SERVER_NODES,StringPool.STAR));
		if (nodeKeySet.isEmpty()) {
			return Collections.emptyList();
		}
		// 截取键名中的节点名称部分，并构建ServerNode对象列表
		int beginIndex = RedisKeys.SERVER_NODES.getKey().length();
		if (StringUtils.isNotBlank(this.redisKey)) {
			beginIndex = RedisKeys.SERVER_NODES.getKey(this.redisKey).length();
		}
		List<ServerNode> list = new ArrayList<>();
		for (String nodeKey : nodeKeySet) {
			String nodeName = nodeKey.substring(beginIndex);
			list.add(new ServerNode(nodeName, this.redisCache.get(nodeKey)));
		}
		return list;
	}

	/**
	 * 获取所有在线客户端数量
	 *
	 * @return 所有节点上的在线客户端总数
	 */
	@Override
	public long getOnlineClientSize() {
		// 扫描所有连接状态键
		Set<String> keySet = this.redisCache.scan(getRedisKey(RedisKeys.CONNECT_STATUS,StringPool.STAR));
		if (keySet.isEmpty()) {
			return 0L;
		}

		// 统计每个节点上客户端集合的大小
		long result = 0;
		for (String redisKey : keySet) {
			Long count = this.redisCache.getSetOps().size(redisKey);
			if (count != null) {
				result += count;
			}
		}
		return result;
	}

	/**
	 * 获取所有在线客户端ID列表
	 *
	 * @return 所有在线客户端ID组成的列表
	 */
	@Override
	public List<String> getOnlineClients() {
		// 扫描所有连接状态键
		Set<String> keySet = this.redisCache.scan(getRedisKey(RedisKeys.CONNECT_STATUS,StringPool.STAR));
		if (keySet.isEmpty()) {
			return Collections.emptyList();
		}

		// 收集所有节点上的客户端成员
		List<String> clientList = new ArrayList<>();
		for (String redisKey : keySet) {
			Set<String> members = this.redisCache.sMembers(redisKey);
			if (members != null && !members.isEmpty()) {
				clientList.addAll(members);
			}
		}
		return clientList;
	}

	/**
	 * 获取指定节点的在线客户端数量
	 *
	 * @param nodeName 节点名称
	 * @return 指定节点上的在线客户端数量
	 */
	@Override
	public long getOnlineClientSize(String nodeName) {
		Long count = this.redisCache.getSetOps().size(getRedisKey(RedisKeys.CONNECT_STATUS,nodeName));
		return count == null ? 0L : count;
	}

	/**
	 * 获取指定节点的所有在线客户端ID列表
	 *
	 * @param nodeName 节点名称
	 * @return 指定节点上的在线客户端ID列表
	 */
	@Override
	public List<String> getOnlineClients(String nodeName) {
		Set<String> members = this.redisCache.sMembers(getRedisKey(RedisKeys.CONNECT_STATUS,nodeName));
		if (members == null || members.isEmpty()) {
			return Collections.emptyList();
		}
		return new ArrayList<>(members);
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
		// 检查并初始化redis缓存实例
		if (this.redisCache == null) {
			this.redisCache = this.context.getBean(MicaRedisCache.class);
		}
	}

}
