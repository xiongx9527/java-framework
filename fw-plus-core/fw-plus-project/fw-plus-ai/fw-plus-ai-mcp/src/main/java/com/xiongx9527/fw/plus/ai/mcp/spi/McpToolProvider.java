package com.xiongx9527.fw.plus.ai.mcp.spi;

import java.util.List;
import java.util.Map;

/**
 * MCP 工具提供者接口
 *
 * @author xiongxiao
 * @date 2025-09-20
 */
public interface McpToolProvider {

	/**
	 * 列出可用的工具
	 *
	 * @param context 提供者上下文
	 * @param pageRequest 分页请求
	 * @return 工具列表
	 */
	List<McpTool> listTools(McpProviderContext context, McpPageRequest pageRequest);

	/**
	 * 调用工具
	 *
	 * @param context 提供者上下文
	 * @param request 工具调用请求
	 * @return 工具执行结果
	 */
	McpToolResult callTool(McpProviderContext context, McpToolCallRequest request);
}
