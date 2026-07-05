package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.auth;

public class McpAuthorizationException extends RuntimeException {

	private final int status;

	public McpAuthorizationException(int status, String message) {
		super(message);
		this.status = status;
	}

	public int getStatus() {
		return status;
	}
}
