package com.xiongx9527.fw.plus.ai.mcp.spi;

/**
 * 资源读取请求
 *
 * <p>对齐 MCP 规范 resources/read：uri 参数。
 *
 * @author xiongxiao
 * @date 2025-09-20
 */
public class McpResourceReadRequest {
	private String uri;

	public McpResourceReadRequest() {
	}

	public McpResourceReadRequest(String uri) {
		this.uri = uri;
	}

	public static McpResourceReadRequest of(String uri) {
		return new McpResourceReadRequest(uri);
	}

	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}
}
