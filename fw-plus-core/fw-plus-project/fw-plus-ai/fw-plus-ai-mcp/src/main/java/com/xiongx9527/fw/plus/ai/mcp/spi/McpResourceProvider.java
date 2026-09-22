package com.xiongx9527.fw.plus.ai.mcp.spi;

import java.util.List;

/**
 * MCP 资源提供者接口
 *
 * <p>对齐 MCP 规范 resources 能力：
 * <ul>
 *   <li>{@code resources/list} - 列举静态资源</li>
 *   <li>{@code resources/templates/list} - 列举资源模板</li>
 *   <li>{@code resources/read} - 按 URI 读取资源</li>
 * </ul>
 *
 * @author xiongxiao
 * @date 2025-09-20
 */
public interface McpResourceProvider {

	/**
	 * 列出静态资源
	 *
	 * @param context 提供者上下文
	 * @return 资源列表，空列表表示无静态资源
	 */
	List<McpResourceInfo> listResources(McpProviderContext context);

	/**
	 * 列出资源模板
	 *
	 * @param context 提供者上下文
	 * @return 资源模板列表，空列表表示无模板
	 */
	List<McpResourceTemplate> listTemplates(McpProviderContext context);

	/**
	 * 读取资源内容
	 *
	 * @param context 提供者上下文
	 * @param request 资源读取请求
	 * @return 资源内容
	 */
	McpResourceContent readResource(McpProviderContext context, McpResourceReadRequest request);
}
