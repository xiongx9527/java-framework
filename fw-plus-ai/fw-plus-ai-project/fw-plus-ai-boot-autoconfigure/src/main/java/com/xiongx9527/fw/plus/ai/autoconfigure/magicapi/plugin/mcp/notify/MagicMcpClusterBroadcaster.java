package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.notify;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server.cluster.McpRedisBroadcaster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 能力变更的集群广播端：监听插件本地能力变更事件，转发到 MCP 频道。
 *
 * <p>payload 携带来源节点 id 与能力类型；其它节点由 {@link MagicMcpClusterChangeListener}
 * 落地。仅在 Redis 就绪时注册，单节点时无订阅者即仅本地生效。</p>
 */
public class MagicMcpClusterBroadcaster implements ApplicationListener<MagicMcpCapabilityChangedEvent> {

	private static final Logger log = LoggerFactory.getLogger(MagicMcpClusterBroadcaster.class);

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	private final McpRedisBroadcaster broadcaster;

	public MagicMcpClusterBroadcaster(McpRedisBroadcaster broadcaster) {
		this.broadcaster = broadcaster;
	}

	@Override
	public void onApplicationEvent(MagicMcpCapabilityChangedEvent event) {
		Map<String, String> payload = new LinkedHashMap<>();
		payload.put("type", MagicMcpClusterChangeListener.MESSAGE_TYPE);
		payload.put("serverId", broadcaster.getServerId());
		payload.put("capability", event.getCapability());

		try {
			broadcaster.broadcast(null, OBJECT_MAPPER.writeValueAsString(payload));
		} catch (JsonProcessingException e) {
			throw new IllegalArgumentException("Unable to serialize capability change payload", e);
		} catch (RuntimeException e) {
			log.warn("广播 MCP 能力变更失败，capability={}", event.getCapability(), e);
		}
	}
}
