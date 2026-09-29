package com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server.scanner.ghost;

import com.ajaxjs.mcp.server.feature.annotation.McpService;
import com.ajaxjs.mcp.server.feature.annotation.Tool;
import com.xiongx9527.fw.plus.ai.mcp.server.annotation.McpServer;

@McpService
@McpServer("ghost")
public class GhostService {

	@Tool("ghostTool")
	public String ghostTool() {
		return "ghost";
	}
}