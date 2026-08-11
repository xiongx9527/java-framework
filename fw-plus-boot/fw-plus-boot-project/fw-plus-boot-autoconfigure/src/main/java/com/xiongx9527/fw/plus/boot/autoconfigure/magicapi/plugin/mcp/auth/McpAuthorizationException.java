package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.auth;

import com.xiongx9527.fw.plus.common.exception.BizException;

public class McpAuthorizationException extends BizException {

	private final int status;

	public McpAuthorizationException(int status, String message) {
		super(message);
		this.status = status;
	}

	public int getStatus() {
		return status;
	}
}
