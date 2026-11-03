/**
 * MCP（Model Context Protocol）契约：客户端、服务端与工具桥接。
 *
 * <p>base 只定义「怎么描述一个 MCP 连接、怎么把 MCP 工具当普通工具用」；
 * 具体的传输实现（STDIO / SSE / Streamable HTTP）在 {@code fw-plus-ai-mcp-server}
 * 与各后端组件子模块，base 不引入任何 MCP SDK。</p>
 */
package com.xiongx9527.fw.plus.ai.base.mcp;
