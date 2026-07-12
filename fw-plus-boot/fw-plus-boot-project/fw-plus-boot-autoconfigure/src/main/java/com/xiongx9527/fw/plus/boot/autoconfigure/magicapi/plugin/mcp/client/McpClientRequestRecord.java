package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.client;

import lombok.Data;

@Data
public class McpClientRequestRecord {

	private Object id;

	private String sessionId;

	private String method;

	private String capability;

	private long createdAt;

	private long timeoutAt;

	private McpClientRequestStatus status = McpClientRequestStatus.PENDING;

	private Object result;

	private Object error;
}
