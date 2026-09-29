package com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server.contracts;

import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server.auth.McpAuthService;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server.auth.McpPrincipal;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server.auth.McpRequest;

/**
 * 测试用认证：恒返回匿名主体，模拟"开了认证但 token 无效"。
 */
public class RejectAuthService implements McpAuthService {

	@Override
	public McpPrincipal authenticate(McpRequest request) {
		return McpPrincipal.ANONYMOUS;
	}
}
