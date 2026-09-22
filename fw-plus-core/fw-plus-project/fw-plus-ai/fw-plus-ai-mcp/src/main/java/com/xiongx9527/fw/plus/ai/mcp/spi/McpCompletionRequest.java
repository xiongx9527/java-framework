package com.xiongx9527.fw.plus.ai.mcp.spi;

/**
 * 补全请求
 *
 * <p>对齐 MCP 规范 completion/complete 请求参数：
 * ref（引用对象）+ argument（当前正在补全的参数名和已输入值）。
 *
 * @author xiongxiao
 * @date 2025-09-20
 */
public class McpCompletionRequest {
	private McpCompletionRef ref;
	private String argumentName;
	private String argumentValue;

	public McpCompletionRequest() {
	}

	public McpCompletionRequest(McpCompletionRef ref, String argumentName, String argumentValue) {
		this.ref = ref;
		this.argumentName = argumentName;
		this.argumentValue = argumentValue;
	}

	public static McpCompletionRequest of(McpCompletionRef ref, String argumentName, String argumentValue) {
		return new McpCompletionRequest(ref, argumentName, argumentValue);
	}

	public McpCompletionRef getRef() {
		return ref;
	}

	public void setRef(McpCompletionRef ref) {
		this.ref = ref;
	}

	public String getArgumentName() {
		return argumentName;
	}

	public void setArgumentName(String argumentName) {
		this.argumentName = argumentName;
	}

	public String getArgumentValue() {
		return argumentValue;
	}

	public void setArgumentValue(String argumentValue) {
		this.argumentValue = argumentValue;
	}
}
