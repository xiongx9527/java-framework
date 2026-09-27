package com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.redis;

import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.common.WsBaseService;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.config.WsConfig;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.handler.AbstractWebSocketHandler;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.handler.RedisWebSocketHandler;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Map;

/**
 * WebSocket 广播用的 Redis 消息监听容器。
 *
 * <p>使用有界线程池替代 {@code RedisMessageListenerContainer} 默认的
 * {@code SimpleAsyncTaskExecutor}，避免集群高流量下线程数无上限增长。</p>
 *
 * <p>只有当容器内的处理器确为 {@link RedisWebSocketHandler} 时才绑定广播监听器：
 * 宿主自行提供的处理器不参与 Redis 广播，此时容器保持空绑定。</p>
 *
 * @author zzy
 */
public class WsRedisMessageListenerContainer extends RedisMessageListenerContainer implements WsBaseService {

	private final WsConfig wsConfig;

	private ThreadPoolTaskExecutor taskExecutor;

	public WsRedisMessageListenerContainer(WsConfig wsConfig,
										   RedisConnectionFactory connectionFactory,
										   ObjectProvider<AbstractWebSocketHandler> handlerProvider) {
		this.wsConfig = wsConfig;
		setConnectionFactory(connectionFactory);

		this.taskExecutor = new ThreadPoolTaskExecutor();
		this.taskExecutor.setCorePoolSize(4);
		this.taskExecutor.setMaxPoolSize(16);
		this.taskExecutor.setThreadNamePrefix("redis-ws-");
		this.taskExecutor.initialize();
		setTaskExecutor(this.taskExecutor);

		AbstractWebSocketHandler webSocketHandler = handlerProvider == null ? null : handlerProvider.getIfAvailable();
		if (webSocketHandler instanceof RedisWebSocketHandler) {
			addMessageListener(((RedisWebSocketHandler) webSocketHandler).getRedisMessageListener(),
					new ChannelTopic(getBroadcastChannelName()));
		}
	}

	@Override
	public Map<String, Object> getConfig() {
		return wsConfig == null ? WsBaseService.super.getConfig() : wsConfig.flatten();
	}

	@Override
	public void destroy() throws Exception {
		super.destroy();
		if (this.taskExecutor != null) {
			this.taskExecutor.shutdown();
		}
	}
}
