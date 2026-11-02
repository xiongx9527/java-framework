/**
 * MCP（Model Context Protocol）客户端 + 服务端抽象。
 *
 * <p>出处：
 * <ul>
 *   <li>langchain4j：{@code dev.langchain4j.mcp}（McpClient / McpTransport / McpToolProvider / McpServer）</li>
 *   <li>Spring AI：{@code org.springframework.ai.mcp}（McpSyncClient / McpAsyncClient / McpServer / ToolCallback）</li>
 *   <li>AgentScope：通过 MCP 把外部工具接入 / 暴露 agent</li>
 * </ul>
 *
 * <p>客户端（{@link McpClient} / {@link McpToolProvider}）由各后端组件子模块实现；服务端（{@link McpServer}）由框架模块
 * {@code fw-plus-ai-mcp-server} 实现，把 base 工具以 MCP 暴露给 SQLBot 等外部调用方。
 */
package com.xiongx9527.fw.plus.ai.mcp;
