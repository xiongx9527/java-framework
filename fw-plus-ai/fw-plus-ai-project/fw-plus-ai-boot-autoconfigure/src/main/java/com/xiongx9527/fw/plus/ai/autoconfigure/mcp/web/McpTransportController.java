package com.xiongx9527.fw.plus.ai.autoconfigure.mcp.web;

import com.ajaxjs.mcp.server.ServerStreamableHttp;
import com.ajaxjs.mcp.server.model.HttpResult;
import com.xiongx9527.fw.plus.ai.mcp.spi.ServerSseRegistry;
import com.xiongx9527.fw.plus.ai.mcp.spi.ServerStreamableHttpRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * MCP 传输控制器公共基类，只负责 HTTP 适配，JSON-RPC 分发与会话策略仍由 {@code fw-plus-ai-mcp} 内核承担。
 * <p>
 * 具体端点按 {@code fw-plus.ai.mcp.transport} 拆到两个子控制器，同一时刻只注册一种传输的接口：
 * {@link McpSseController} 提供 HTTP/SSE 的 {@code GET {endpoint}/sse} 与 {@code POST {endpoint}/message}；
 * {@link McpStreamableHttpController} 提供 Streamable HTTP 的 {@code POST/GET/DELETE {endpoint}}。
 * </p>
 */
public abstract class McpTransportController {
    private static final Logger log = LoggerFactory.getLogger(McpTransportController.class);

    private final ApplicationContext context;

    /**
     * MCP 端点基础路径，SSE 的消息端点挂在其下。
     */
    @Value("${fw-plus.ai.mcp.endpoint:/mcp}")
    private String endpoint = "/mcp";

    protected McpTransportController(ApplicationContext context) {
        this.context = context;
    }

    /**
     * 返回配置的 MCP 端点基础路径。
     */
    protected String endpoint() {
        return endpoint;
    }

    /**
     * 查找当前传输注册的 SSE 注册表，传输未启用时返回 {@code null}。
     */
    protected ServerSseRegistry sseRegistry() {
        return context.getBeanProvider(ServerSseRegistry.class).getIfAvailable();
    }

    /**
     * 查找当前传输注册的 Streamable HTTP 注册表，传输未启用时返回 {@code null}。
     */
    protected ServerStreamableHttpRegistry streamableRegistry() {
        return context.getBeanProvider(ServerStreamableHttpRegistry.class).getIfAvailable();
    }

    protected static Map<String, String> headers(HttpServletRequest request) {
        Map<String, String> values = new LinkedHashMap<>();
        Collections.list(request.getHeaderNames()).forEach(name -> values.put(name, request.getHeader(name)));
        return values;
    }

    protected static void write(HttpResult result, HttpServletResponse response) throws IOException {
        response.setStatus(result.getStatus());
        response.setCharacterEncoding("UTF-8");
        result.getHeaders().forEach(response::setHeader);

        if (result.getContentType() != null)
            response.setContentType(result.getContentType());

        // 通知没有 JSON 响应体，不能写成字符串 "null"。
        if (result.getBody() != null)
            response.getWriter().write(result.getBody());
    }

    static Logger logger() {
        return log;
    }

    /**
     * Streamable HTTP 会话头名，供子控制器读取。
     */
    protected static String sessionIdHeader() {
        return ServerStreamableHttp.SESSION_ID_HEADER;
    }
}
