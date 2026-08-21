package com.xiongx9527.fw.plus.ai.askdata.tool;

/**
 * 工具执行后的不可变脱敏结果。
 *
 * <p>内容仅用于后续受控 Flow 和审计摘要，不应包含凭据、未授权数据或可直接绕过校验执行的指令。</p>
 */
public final class AskDataToolResult {

	private final String content;

	private AskDataToolResult(String content) {
		this.content = content;
	}

	public static AskDataToolResult success(String content) {
		return new AskDataToolResult(content);
	}

	public String getContent() {
		return content;
	}
}
