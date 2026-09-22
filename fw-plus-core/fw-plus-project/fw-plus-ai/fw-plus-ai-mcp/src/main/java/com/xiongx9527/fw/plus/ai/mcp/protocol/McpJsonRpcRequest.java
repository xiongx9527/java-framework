package com.xiongx9527.fw.plus.ai.mcp.protocol;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * MCP JSON-RPC 请求
 *
 * @author xiongxiao
 * @date 2025-09-20
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class McpJsonRpcRequest {
	@JsonProperty("jsonrpc")
	private String jsonrpc;

	@JsonProperty("id")
	private Object id;

	@JsonProperty("method")
	private String method;

	@JsonProperty("params")
	private Map<String, Object> params;

	public McpJsonRpcRequest() {
	}

	public String getJsonrpc() {
		return jsonrpc;
	}

	public void setJsonrpc(String jsonrpc) {
		this.jsonrpc = jsonrpc;
	}

	public Object getId() {
		return id;
	}

	public void setId(Object id) {
		this.id = id;
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
}
