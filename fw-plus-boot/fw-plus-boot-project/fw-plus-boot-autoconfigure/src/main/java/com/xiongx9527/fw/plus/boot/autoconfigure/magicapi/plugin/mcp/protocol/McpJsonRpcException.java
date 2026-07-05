package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.protocol;

public class McpJsonRpcException extends RuntimeException {

	private final int code;

	private final Object data;

	public McpJsonRpcException(int code, String message) {
		this(code, message, null);
	}

	public McpJsonRpcException(int code, String message, Object data) {
		super(message);
		this.code = code;
		this.data = data;
	}

	public int getCode() {
		return code;
	}

	public Object getData() {
		return data;
	}
}
