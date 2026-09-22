package com.xiongx9527.fw.plus.ai.mcp.spi;

import java.util.List;

/**
 * MCP 补全提供者接口
 *
 * <p>对齐 MCP 规范 completion/complete：针对 prompt 参数引用或 resource 路径引用的补全。
 * 每个 Provider 通过 {@link #supportedRefs()} 声明可服务的引用集合。
 *
 * @author xiongxiao
 * @date 2025-09-20
 */
public interface McpCompletionProvider {

	/**
	 * 声明本 Provider 支持的补全引用列表（prompt 参数或 resource 路径）。
	 *
	 * @return 支持的引用列表，空列表表示不限制
	 */
	List<McpCompletionRef> supportedRefs();

	/**
	 * 执行补全
	 *
	 * @param context 提供者上下文
	 * @param request 补全请求
	 * @return 补全结果
	 */
	McpCompletionResult complete(McpProviderContext context, McpCompletionRequest request);
}
