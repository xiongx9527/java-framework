package com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server.contracts;

import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server.auth.McpAuthService;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server.auth.McpPrincipal;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server.auth.McpRequest;
import com.xiongx9527.fw.plus.auth.core.AuthPrincipal;
import com.xiongx9527.fw.plus.auth.core.AuthRequest;
import com.xiongx9527.fw.plus.auth.core.AuthService;
import com.xiongx9527.fw.plus.auth.core.RejectingAuthService;

/**
 * 旧接口适配：提供基于新核心 AuthService 的旧接口实现，用于测试兼容。
 */
public class RejectAuthService implements McpAuthService {

    private static final AuthService CORE = RejectingAuthService.INSTANCE;

    @Override
    public McpPrincipal authenticate(McpRequest request) {
        AuthPrincipal core = CORE.authenticate(McpAuthService.toCore(request));
        return McpAuthService.toMcpPrincipal(core);
    }
}