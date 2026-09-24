package com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.handler;

import com.alibaba.fastjson2.JSON;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.broadcast.WsLocalBroadcastTarget;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.config.WsConfig;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.redis.WsBroadcastMessage;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.redis.WsBroadcastMessageListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 支持 Redis 的 WebSocket 处理器：通过 Redis 频道实现跨实例广播。
 *
 * @author zzy
 */
@Slf4j
public class RedisWebSocketHandler extends AbstractWebSocketHandler {

	/**
	 * Redis消息监听器，用于处理分布式环境下的WebSocket消息
	 */
	private final MessageListener redisMessageListener;

	/**
	 * Redis模板，用于发送消息到Redis
	 */
	@Autowired(required = true)
	private StringRedisTemplate stringRedisTemplate;

	public RedisWebSocketHandler(WsConfig wsConfig) {
		super(wsConfig);
		// 监听器位于 redis 包，无法直接调用本类继承的包私有本地投递方法；
		// 这里以目标接口注入，投递仍经由可覆写的 broadcastMessageTo*LocalOnly。
		this.redisMessageListener = new WsBroadcastMessageListener(getWebSocketServerId(), new WsLocalBroadcastTarget() {
			@Override
			public boolean sendToClient(String appId, String clientId, String message) {
				return broadcastMessageToClientLocalOnly(appId, clientId, message);
			}

			@Override
			public void sendToAllClients(String appId, String message) {
				broadcastMessageToAllClientLocalOnly(appId, message);
			}
		});
	}

	/**
	 * 通过Redis向指定客户端广播消息
	 *
	 * @param excludedServerId 需要排除的服务器ID，不会向该服务器上的客户端发送消息
	 * @param appId            应用ID
	 * @param clientId         客户端ID
	 * @param message          要发送的消息内容
	 */
	@Override
	void broadcastMessageToClient(String excludedServerId, String appId, String clientId, String message) {
		try {
			WsBroadcastMessage broadcastMessage =
					new WsBroadcastMessage(excludedServerId, appId, clientId, message);
			String broadcastChannel = getBroadcastChannelName();
			stringRedisTemplate.convertAndSend(broadcastChannel, JSON.toJSONString(broadcastMessage));

			log.debug("Broadcast message sent to Redis channel: {}, excludedServerId: {}, clientId: {}, appId: {}",
					broadcastChannel, excludedServerId, clientId, appId);
		} catch (Exception e) {
			log.error("Failed to send broadcast message to client via Redis - clientId: {}, appId: {}", clientId, appId, e);
		}
	}

	/**
	 * 向所有客户端广播消息
	 *
	 * @param excludedServerId 需要排除的服务器ID，该服务器的客户端将不会收到广播消息
	 * @param appId            应用程序ID，用于标识消息所属的应用
	 * @param message          要广播的消息内容
	 */
	@Override
	void broadcastMessageToAllClient(String excludedServerId, String appId, String message) {
		broadcastMessageToClient(excludedServerId, appId, null, message);
	}

	/**
	 * 获取Redis消息监听器
	 *
	 * @return Redis消息监听器
	 */
	public MessageListener getRedisMessageListener() {
		return redisMessageListener;
	}
}
