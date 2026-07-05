package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.protocol;

import lombok.Data;

@Data
public class McpJsonRpcResponse {

	private String jsonrpc = "2.0";

	private Object id;

	private Object result;

	private McpJsonRpcError error;

	public static McpJsonRpcResponse success(Object id, Object result) {
		McpJsonRpcResponse response = new McpJsonRpcResponse();
		response.setId(id);
		response.setResult(result);
		return response;
	}

	public static McpJsonRpcResponse error(Object id, McpJsonRpcError error) {
		McpJsonRpcResponse response = new McpJsonRpcResponse();
		response.setId(id);
		response.setError(error);
		return response;
	}
}
