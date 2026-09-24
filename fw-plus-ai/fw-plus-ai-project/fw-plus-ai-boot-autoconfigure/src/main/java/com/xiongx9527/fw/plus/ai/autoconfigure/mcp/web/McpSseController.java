package com.xiongx9527.fw.plus.ai.autoconfigure.mcp.web;

import com.ajaxjs.mcp.server.error.JsonRpcErrorException;
import com.xiongx9527.fw.plus.ai.mcp.spi.ServerSseRegistry;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.UUID;

/**
 * HTTP/SSE 传输控制器，仅在 {@code fw-plus.ai.mcp.transport=http-sse} 时注册。
 * <p>
 * {@code GET {endpoint}/sse} 建立事件流并下发客户端后续 POST 消息使用的端点路径；
 * {@code POST {endpoint}/message?uuid=} 投递 JSON-RPC 消息，响应经事件流回送给该会话。
 * </p>
 */
@RestController
public class McpSseController extends McpTransportController {

    public McpSseController(org.springframework.context.ApplicationContext context) {
        super(context);
    }

    /**
     * 建立 SSE 事件流，并下发客户端后续 POST 消息使用的端点路径。
     */
    @GetMapping(value = "${fw-plus.ai.mcp.endpoint:/mcp}/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public void streamSse(HttpServletRequest request, HttpServletResponse response) throws IOException {
        ServerSseRegistry sse = sseRegistry();
        if (sse == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        if (!sse.isOriginAllowed(request.getHeader("Origin"))) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        response.setContentType(MediaType.TEXT_EVENT_STREAM_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Connection", "keep-alive");

        String uuid = UUID.randomUUID().toString();
        PrintWriter writer = response.getWriter();

        try {
            sse.openSession(uuid, writer, endpoint() + "/message?uuid=" + uuid);

            // 连接生命周期由内核维护，这里只负责持有写通道直至对端断开。
            while (sse.isSessionOpen(uuid) && !Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        } finally {
            sse.removeConnection(uuid);
        }
    }

    /**
     * 接收 SSE 客户端的 JSON-RPC 消息，响应经事件流回送给该会话。
     */
    @PostMapping("${fw-plus.ai.mcp.endpoint:/mcp}/message")
    public void handleMessage(@RequestBody String json, @RequestParam String uuid,
                              HttpServletRequest request, HttpServletResponse response) throws IOException {
        ServerSseRegistry sse = sseRegistry();
        if (sse == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        if (!sse.isOriginAllowed(request.getHeader("Origin"))) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        try {
            sse.handle(uuid, json);
        } catch (JsonRpcErrorException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(e.toJson());
        }
    }
}
