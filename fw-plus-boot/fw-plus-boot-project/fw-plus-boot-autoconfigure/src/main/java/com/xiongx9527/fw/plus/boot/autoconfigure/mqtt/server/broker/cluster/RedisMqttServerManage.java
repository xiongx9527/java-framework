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
import org.dromara.mica.mqtt.core.server.MqttServer;
import net.dreamlu.mica.core.utils.CharPool;
import net.dreamlu.mica.core.utils.INetUtil;
import net.dreamlu.mica.redis.cache.MicaRedisCache;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * mqtt 服务节点管理
 *
 * @author L.cm
 */
public class RedisMqttServerManage implements ApplicationContextAware,SmartInitializingSingleton, DisposableBean {

	/**
	 * 应用程序上下文实例，用于获取Spring容器中的Bean和服务
	 */
	private ApplicationContext context;

	/** Redis缓存实例，用于操作Redis缓存数据 */
	private MicaRedisCache redisCache;

	/** 节点名称，标识当前节点的唯一名称 */
	private final String nodeName;

	/** 主机名称，表示当前服务部署的主机名 */
	private final String hostName;

	/**
	 * RedisMqttServerManage构造函数
	 *
	 * @param mqttServer MQTT服务器实例，用于获取服务器创建者信息
	 */
	public RedisMqttServerManage(MqttServer mqttServer) {
		this(null, mqttServer, null);
	}

	/**
	 * RedisMqttServerManage构造函数
	 *
	 * @param mqttServer MQTT服务器实例，用于获取服务器创建者信息
	 * @param port MQTT 服务监听端口。mica-mqtt 2.6.x 起端口由协议监听器承载，
	 *             无法从 creator 直接获取，因此由外部传入
	 */
	public RedisMqttServerManage(MqttServer mqttServer, Integer port) {
		this(null, mqttServer, port);
	}

	/**
	 * RedisMqttServerManage构造函数
	 * @param redisCache Redis缓存实例，用于存储服务器节点信息
	 * @param mqttServer MQTT服务器实例，提供服务器创建者信息
	 */
	public RedisMqttServerManage(MicaRedisCache redisCache, MqttServer mqttServer) {
		this(redisCache, mqttServer, null);
	}

	/**
	 * RedisMqttServerManage构造函数
	 * @param redisCache Redis缓存实例，用于存储服务器节点信息
	 * @param mqttServer MQTT服务器实例，提供服务器创建者信息
	 * @param port MQTT 服务监听端口
	 */
	public RedisMqttServerManage(MicaRedisCache redisCache, MqttServer mqttServer, Integer port) {
		this.redisCache = redisCache;
		this.nodeName = mqttServer.getServerCreator().getNodeName();
		this.hostName = getHostName(port);
	}

	/**
	 * 销毁bean时执行的回调方法
	 * 从Redis缓存中删除当前服务器节点信息
	 */
	@Override
	public void destroy() throws Exception {
		this.redisCache.del(RedisKeys.SERVER_NODES.getKey(nodeName));
	}

	/**
	 * 获取服务器主机名
	 * @param port MQTT 服务监听端口，允许为空
	 * @return 返回主机IP地址和端口拼接的字符串，格式为"IP:端口"
	 */
	private String getHostName(Integer port) {
		return INetUtil.getHostIp() + CharPool.COLON + (port == null ? "" : port);
	}

	/**
	 * 设置应用程序上下文
	 *
	 * @param applicationContext 应用程序上下文对象
	 * @throws BeansException 当设置上下文过程中发生错误时抛出
	 */
	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		// 保存应用程序上下文引用
		this.context = applicationContext;
	}


	/**
	 * 在单例bean实例化完成后执行的回调方法
	 * 将当前服务器节点信息存储到Redis缓存中
	 */
	@Override
	public void afterSingletonsInstantiated() {
		if (this.redisCache == null) {
			this.redisCache = this.context.getBean(MicaRedisCache.class);
		}
		this.redisCache.set(RedisKeys.SERVER_NODES.getKey(nodeName), hostName);
	}

}
