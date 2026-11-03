package com.xiongx9527.fw.plus.ai.base.mcp;

/**
 * MCP 传输方式。
 */
public enum McpTransport {

    /** 本地子进程，按行收发 JSON-RPC。 */
    STDIO,

    /** HTTP + Server-Sent Events（旧版远程传输）。 */
    SSE,

    /** Streamable HTTP（新版远程传输，单端点）。 */
    STREAMABLE_HTTP
}
