package com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server.runtime;

import com.ajaxjs.mcp.server.McpServer;
import com.ajaxjs.mcp.server.common.ServerConfig;
import com.ajaxjs.mcp.server.feature.FeatureMgr;

/**
 * MCP server 实例工厂：runtime 层只依赖此扩展点，插件可提供 {@link McpServer} 子类接管消息处理。
 */
@FunctionalInterface
public interface McpServerFactory {

    /**
     * 内核默认实现。
     */
    McpServerFactory DEFAULT = (config, featureMgr) -> new McpServer();

    /**
     * 创建 server 实例；返回的实例会被 {@link McpRuntimeFactory} 补齐 config 与 featureMgr。
     *
     * @param config     server 配置
     * @param featureMgr 能力管理器
     * @return server 实例，不可为 {@code null}
     */
    McpServer create(ServerConfig config, FeatureMgr featureMgr);
}
