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

package com.xiongx9527.fw.plus.boot.autoconfigure.mqtt.server.broker.listener;

import com.xiongx9527.fw.plus.boot.autoconfigure.mqtt.server.broker.enums.RedisKeys;
import lombok.RequiredArgsConstructor;
import org.dromara.mica.mqtt.core.server.MqttServerCreator;
import org.dromara.mica.mqtt.core.server.event.IMqttConnectStatusListener;
import net.dreamlu.mica.redis.cache.MicaRedisCache;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import net.dreamlu.mica.net.core.ChannelContext;

/**
 * mqtt 连接监听，此处也可以添加发送到 mq 的逻辑，方便影子服务处理
 *
 * @author L.cm
 */
@RequiredArgsConstructor
public class RedisMqttConnectStatusListener implements IMqttConnectStatusListener, ApplicationContextAware, SmartInitializingSingleton, DisposableBean {

	/**
	 * 应用程序上下文实例，用于获取Spring容器中的Bean和服务
	 */
	private ApplicationContext context;

	/**
	 * Redis缓存实例，用于操作Redis缓存数据
	 */
	private MicaRedisCache redisCache;

	/**
	 * MQTT服务器创建器，用于创建和配置MQTT服务器实例
	 */
	private MqttServerCreator serverCreator;

	/**
	 * Redis键名字段
	 *
	 * 用于存储Redis中数据项的键名标识
	 */
	private String redisKey ;

	/**
	 * RedisMqttConnectStatusListener构造函数
	 *
	 * @param serverCreator MQTT服务器创建器，用于创建和配置MQTT服务器实例
	 * @param redisCache Redis缓存操作对象，用于与Redis进行数据交互
	 * @param redisKey Redis中存储MQTT连接状态的键名
	 */
	public RedisMqttConnectStatusListener(MqttServerCreator serverCreator, MicaRedisCache redisCache, String redisKey) {
		this.serverCreator = serverCreator;
		this.redisCache = redisCache;
		this.redisKey = redisKey;
	}

	/**
	 * 设备上下线存储，key 的值为 前缀:nodeName
	 *
	 * @return redis key
	 */
	private String getRedisKey() {
		String redisKey = RedisKeys.CONNECT_STATUS.getKey(this.serverCreator.getNodeName());
		if (StringUtils.isNotBlank(this.redisKey)) {
			redisKey += ":" +this.redisKey;
		}
		return redisKey;
	}

	/**
	 * 设备上线处理方法
	 * 将设备clientId添加到Redis集合中，表示设备已在线
	 *
	 * @param context Channel上下文对象
	 * @param clientId 客户端唯一标识符
	 * @param username 用户名
	 */
	@Override
	public void online(ChannelContext context, String clientId, String username) {
		this.redisCache.sAdd(getRedisKey(), clientId);
	}

	/**
	 * 设备下线处理方法
	 * 从Redis集合中移除设备clientId，表示设备已离线
	 *
	 * @param context Channel上下文对象
	 * @param clientId 客户端唯一标识符
	 * @param username 用户名
	 * @param reason 下线原因
	 */
	@Override
	public void offline(ChannelContext context, String clientId, String username, String reason) {
		this.redisCache.sRem(getRedisKey(), clientId);
	}

	/**
	 * 销毁方法
	 * 在应用停机时删除Redis中的设备状态集合
	 *
	 * @throws Exception 删除操作可能抛出的异常
	 */
	@Override
	public void destroy() throws Exception {
		// 停机时删除集合
		this.redisCache.del(getRedisKey());
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
	 * Spring容器初始化完成后执行的方法
	 * 获取MqttServerCreator实例并赋值给serverCreator属性
	 */
	@Override
	public void afterSingletonsInstantiated() {
		if (this.serverCreator == null) {
			this.serverCreator = this.context.getBean(MqttServerCreator.class);
		}
		if (this.redisCache == null) {
			this.redisCache = this.context.getBean(MicaRedisCache.class);
		}
	}

}
