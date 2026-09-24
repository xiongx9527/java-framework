package com.xiongx9527.fw.plus.ai.mcp.spi;

import com.ajaxjs.mcp.protocol.McpRequestRawInfo;
import com.ajaxjs.mcp.protocol.McpResponse;
import com.ajaxjs.mcp.server.McpServer;

/**
 * MCP 服务器注册表
 */
public abstract class McpServerRegistry extends McpServer {

    /**
     * 处理消息
     *
     * @param requestRaw 请求原始数据
     * @return 响应
     */
    public final McpResponse processMessage(McpRequestRawInfo requestRaw) {
        return process(super.processMessage(requestRaw), requestRaw);
    }

    /**
     * 处理消息
     *
     * @param response   响应
     * @param requestRaw 请求原始数据
     * @return 响应
     */
    public abstract McpResponse process(McpResponse response, McpRequestRawInfo requestRaw);

}
