package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.notify;

import org.springframework.context.ApplicationEvent;

/**
 * MCP 能力变更事件：本地能力刷新后发布，供集群广播端与其它扩展点订阅。
 *
 * @param capability tools / resources / prompts
 */
public class MagicMcpCapabilityChangedEvent extends ApplicationEvent {

	private final String capability;

	public MagicMcpCapabilityChangedEvent(String capability) {
		super(capability);
		this.capability = capability;
	}

	public String getCapability() {
		return capability;
	}
}
