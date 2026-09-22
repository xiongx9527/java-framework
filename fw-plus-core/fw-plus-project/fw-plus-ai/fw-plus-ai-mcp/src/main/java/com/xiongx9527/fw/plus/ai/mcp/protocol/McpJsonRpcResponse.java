package com.xiongx9527.fw.plus.ai.mcp.protocol;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * MCP JSON-RPC 响应
 *
 * @author xiongxiao
 * @date 2025-09-20
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class McpJsonRpcResponse {
	@JsonProperty("jsonrpc")
	private String jsonrpc = "2.0";

	@JsonProperty("id")
	private Object id;

	@JsonProperty("result")
	private Object result;

	@JsonProperty("error")
	private McpJsonRpcError error;

	public McpJsonRpcResponse() {
	}

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

	public static McpJsonRpcResponse error(Object id, int code, String message) {
		return error(id, new McpJsonRpcError(code, message));
	}

	public String getJsonrpc() {
		return jsonrpc;
	}

	public Object getId() {
		return id;
	}

	public void setId(Object id) {
		this.id = id;
	}

	public Object getResult() {
		return result;
	}

	public void setResult(Object result) {
		this.result = result;
	}

	public McpJsonRpcError getError() {
		return error;
	}

	public void setError(McpJsonRpcError error) {
		this.error = error;
	}

	/**
	 * 是否为错误响应
	 *
	 * <p>标记 {@link com.fasterxml.jackson.annotation.JsonIgnore}：避免与
	 * {@link #getError()} 形成同名 "error" 属性导致 Jackson 序列化冲突。
	 */
	@JsonIgnore
	public boolean isError() {
		return error != null;
	}
}
