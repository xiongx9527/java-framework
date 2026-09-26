package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.notify;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 订阅 MCP 广播频道，处理「能力变更」类集群消息：其它节点改动了 Magic-API 资源时，
 * 本节点据此刷新各实例的工具/资源/提示词快照，并向本机客户端广播对应的 list_changed。
 *
 * <p>与 starter 的 {@code McpRedisMessageConsumer} 订阅同一频道（Redis pub/sub 多订阅者），
 * 但只接管 {@code type == capability-changed} 的消息：MCP 协议消息仍由 starter 消费。
 * 消息携带来源节点 {@code serverId}，与自身一致时忽略，避免本节点重复刷新。</p>
 *
 * <p>无 Redis（单节点）时本类不注册，能力变更只在本节点生效。</p>
 */
public class MagicMcpClusterChangeListener implements MessageListener {

	private static final Logger log = LoggerFactory.getLogger(MagicMcpClusterChangeListener.class);

	/**
	 * 能力变更消息类型。
	 */
	public static final String MESSAGE_TYPE = "capability-changed";

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	private final String serverId;

	private final MagicMcpNotifier notifier;

	public MagicMcpClusterChangeListener(String serverId, MagicMcpNotifier notifier) {
		this.serverId = serverId;
		this.notifier = notifier;
	}

	@Override
	public void onMessage(Message message, byte[] pattern) {
		JsonNode payload = parse(message);
		if (payload == null) {
			return;
		}

		String source = payload.path("serverId").asText(null);
		if (serverId != null && serverId.equals(source)) {
			// 本节点自己发出的，本地已在发送前刷新
			return;
		}
		if (!MESSAGE_TYPE.equals(payload.path("type").asText(null))) {
			return;
		}

		String capability = payload.path("capability").asText(null);
		try {
			if ("resources".equals(capability)) {
				notifier.refreshResources();
			} else if ("prompts".equals(capability)) {
				notifier.refreshPrompts();
			} else if ("tools".equals(capability)) {
				notifier.refreshTools();
			} else {
				log.debug("忽略未知能力的集群变更消息，capability={}", capability);
			}
		} catch (RuntimeException e) {
			log.warn("处理集群能力变更消息失败，capability={}", capability, e);
		}
	}

	private JsonNode parse(Message message) {
		if (message == null || message.getBody() == null) {
			return null;
		}
		try {
			return OBJECT_MAPPER.readTree(new String(message.getBody(), StandardCharsets.UTF_8));
		} catch (IOException e) {
			log.debug("无法解析 MCP 广播消息体，忽略", e);
			return null;
		}
	}
}
