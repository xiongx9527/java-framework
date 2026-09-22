package com.xiongx9527.fw.plus.ai.mcp.transport;

import com.xiongx9527.fw.plus.ai.mcp.server.McpAsyncServerImpl;

/**
 * Transport 生命周期接口
 *
 * <p>解决"transport 持有 server、server 构造依赖 transport"的循环依赖：
 * 先创建 transport，再用它构造 {@code McpServerBuilder}，调用 {@code build()}
 * 得到 server，然后通过 {@link #bindServer(McpAsyncServerImpl)} 把 server 注入回 transport。
 *
 * <p>框架无关：core 传输实现与上层自动配置均只依赖该接口。
 *
 * @author xiongxiao
 * @date 2025-09-21
 */
public interface McpTransportLifecycle {

	/**
	 * 绑定已构建完成的 server 实例
	 *
	 * @param server 已构建并准备启动的 server
	 */
	void bindServer(McpAsyncServerImpl server);
}
