package com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.redis;

import com.alibaba.fastjson2.JSON;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.broadcast.WsLocalBroadcastTarget;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;

import java.nio.charset.StandardCharsets;

/**
 * Redis 广播频道监听器：把其它实例发布的广播投递到本实例的会话。
 *
 * <p>只做本地投递，不再回发 Redis，否则会形成广播循环。投递经由
 * {@link WsLocalBroadcastTarget} 交给处理器，避免为跨包调用而扩大处理器方法的可见性。</p>
 *
 * @author zzy
 */
@Slf4j
public class WsBroadcastMessageListener implements MessageListener {

	private final String currentServerId;

	private final WsLocalBroadcastTarget target;

	public WsBroadcastMessageListener(String currentServerId, WsLocalBroadcastTarget target) {
		this.currentServerId = currentServerId;
		this.target = target;
	}

	/**
	 * 处理从 Redis 接收到的广播消息。
	 *
	 * @param message 从 Redis 接收到的消息
	 * @param pattern 消息匹配的模式
	 */
	@Override
	public void onMessage(Message message, byte[] pattern) {
		try {
			String messageBody = new String(message.getBody(), StandardCharsets.UTF_8);
			WsBroadcastMessage broadcastMessage = JSON.parseObject(messageBody, WsBroadcastMessage.class);

			if (broadcastMessage == null) {
				log.warn("Received null broadcast message from Redis");
				return;
			}

			String excludedServerId = broadcastMessage.getExcludedServerId();
			if (excludedServerId != null && excludedServerId.equals(currentServerId)) {
				log.debug("Skipping message for excluded server: {}", excludedServerId);
				return;
			}

			// 校验必要字段，防止反序列化出空字段导致下游 NPE
			if (broadcastMessage.getAppId() == null || broadcastMessage.getMessage() == null) {
				log.warn("Received broadcast message with null appId or message, ignoring");
				return;
			}

			if (StringUtils.isBlank(broadcastMessage.getClientId())) {
				target.sendToAllClients(broadcastMessage.getAppId(), broadcastMessage.getMessage());
			} else {
				target.sendToClient(broadcastMessage.getAppId(), broadcastMessage.getClientId(),
						broadcastMessage.getMessage());
			}
		} catch (Exception e) {
			log.error("Error processing Redis broadcast message", e);
		}
	}
}
