package com.xiongx9527.fw.plus.ai.mcp.protocol;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * MCP JSON-RPC 错误
 *
 * @author xiongxiao
 * @date 2025-09-20
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class McpJsonRpcError {
	public static final int PARSE_ERROR = -32700;
	public static final int INVALID_REQUEST = -32600;
	public static final int METHOD_NOT_FOUND = -32601;
	public static final int INVALID_PARAMS = -32602;
	public static final int INTERNAL_ERROR = -32603;
	public static final int RESOURCE_NOT_FOUND = -32002;

	@JsonProperty("code")
	private int code;

	@JsonProperty("message")
	private String message;

	@JsonProperty("data")
	private Object data;

	public McpJsonRpcError() {
	}

	public McpJsonRpcError(int code, String message) {
		this.code = code;
		this.message = message;
	}

	public McpJsonRpcError(int code, String message, Object data) {
		this.code = code;
		this.message = message;
		this.data = data;
	}

	public static McpJsonRpcError parseError() {
		return new McpJsonRpcError(PARSE_ERROR, "Parse error");
	}

	public static McpJsonRpcError invalidRequest() {
		return new McpJsonRpcError(INVALID_REQUEST, "Invalid request");
	}

	public static McpJsonRpcError methodNotFound(String method) {
		return new McpJsonRpcError(METHOD_NOT_FOUND, "Method not found: " + method);
	}

	public static McpJsonRpcError invalidParams(String message) {
		return new McpJsonRpcError(INVALID_PARAMS, message);
	}

	public static McpJsonRpcError internalError(String message) {
		return new McpJsonRpcError(INTERNAL_ERROR, message);
	}

	public int getCode() {
		return code;
	}

	public void setCode(int code) {
		this.code = code;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public Object getData() {
		return data;
	}

	public void setData(Object data) {
		this.data = data;
	}
}
