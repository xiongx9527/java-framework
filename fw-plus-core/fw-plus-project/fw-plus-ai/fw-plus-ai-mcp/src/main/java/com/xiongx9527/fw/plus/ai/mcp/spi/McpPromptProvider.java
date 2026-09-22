package com.xiongx9527.fw.plus.ai.mcp.spi;

import java.util.List;
import java.util.Map;

/**
 * MCP 提示词提供者接口
 *
 * @author xiongxiao
 * @date 2025-09-20
 */
public interface McpPromptProvider {

	/**
	 * 列出可用的提示词模板
	 *
	 * @param context 提供者上下文
	 * @return 提示词列表
	 */
	List<McpPromptInfo> listPrompts(McpProviderContext context);

	/**
	 * 获取提示词内容
	 *
	 * @param context 提供者上下文
	 * @param request 提示词请求
	 * @return 提示词结果
	 */
	McpPromptResult getPrompt(McpProviderContext context, McpPromptRequest request);
}
