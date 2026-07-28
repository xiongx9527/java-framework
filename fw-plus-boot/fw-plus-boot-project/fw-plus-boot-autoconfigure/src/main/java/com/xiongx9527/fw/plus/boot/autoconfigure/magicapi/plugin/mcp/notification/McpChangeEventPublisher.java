package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.notification;

public interface McpChangeEventPublisher {

	String getOriginId();

	void publish(McpChangeEvent event);
}
