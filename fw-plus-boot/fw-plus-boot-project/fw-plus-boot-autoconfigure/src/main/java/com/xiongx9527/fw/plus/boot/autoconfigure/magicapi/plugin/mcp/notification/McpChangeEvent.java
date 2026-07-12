package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.notification;

import java.util.HashMap;
import java.util.Map;

public class McpChangeEvent {

	private String originId;

	private String method;

	private Map<String, Object> params;

	public McpChangeEvent() {
	}

	public McpChangeEvent(String originId, Map<String, Object> message) {
		this.originId = originId;
		if (message != null) {
			this.method = String.valueOf(message.get("method"));
			Object params = message.get("params");
			if (params instanceof Map) {
				this.params = new HashMap<>((Map<String, Object>) params);
			}
		}
	}

	public String getOriginId() {
		return originId;
	}

	public void setOriginId(String originId) {
		this.originId = originId;
	}

	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
	}

	public Map<String, Object> getParams() {
		return params;
	}

	public void setParams(Map<String, Object> params) {
		this.params = params;
	}

	public Map<String, Object> toMessage() {
		Map<String, Object> message = new HashMap<>();
		message.put("jsonrpc", "2.0");
		message.put("method", method);
		if (params != null && !params.isEmpty()) {
			message.put("params", params);
		}
		return message;
	}
}
