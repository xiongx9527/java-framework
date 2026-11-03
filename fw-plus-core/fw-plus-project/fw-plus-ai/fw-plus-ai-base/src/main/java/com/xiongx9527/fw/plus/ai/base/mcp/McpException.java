package com.xiongx9527.fw.plus.ai.base.mcp;

import com.xiongx9527.fw.plus.common.exception.BizException;

/**
 * MCP 连接或调用失败。
 */
public class McpException extends BizException {

    private static final long serialVersionUID = 1L;

    /** 状态码：服务端 + 第三方服务 + 003。 */
    public static final int CODE = 50_008_003;

    public McpException(String message) {
        super(CODE, message);
    }

    public McpException(String message, Throwable cause) {
        super(CODE, message, cause);
    }
}
