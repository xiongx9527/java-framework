package com.xiongx9527.fw.plus.ai.autoconfigure.mcp.web;

import com.ajaxjs.mcp.server.model.HttpResult;
import com.xiongx9527.fw.plus.ai.mcp.spi.ServerStreamableHttpRegistry;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Streamable HTTP 传输控制器，仅在 {@code fw-plus.ai.mcp.transport=streamable-http} 时注册。
 * <p>
 * 所有 JSON-RPC 消息走同一个端点 {@code {endpoint}}：POST 投递消息，DELETE 终止会话，
 * GET 建立服务器主动推送的事件流。请求头原样交给内核做协议与会话校验。
 * </p>
 */
@RestController
public class McpStreamableHttpController extends McpTransportController {

    public McpStreamableHttpController(org.springframework.context.ApplicationContext context) {
        super(context);
    }

    /**
     * Streamable HTTP 消息入口。
     */
    @PostMapping("${fw-plus.ai.mcp.endpoint:/mcp}")
    public void post(@RequestBody(required = false) String body, HttpServletRequest request,
                     HttpServletResponse response) throws IOException {
        ServerStreamableHttpRegistry transport = streamableRegistry();
        if (transport == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        write(transport.post(body == null ? "" : body, headers(request)), response);
    }

    /**
     * 终止一个 Streamable HTTP 会话。
     */
    @DeleteMapping("${fw-plus.ai.mcp.endpoint:/mcp}")
    public void delete(HttpServletRequest request, HttpServletResponse response) throws IOException {
        ServerStreamableHttpRegistry transport = streamableRegistry();
        if (transport == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        write(transport.delete(request.getHeader(sessionIdHeader()), headers(request)), response);
    }

    /**
     * 建立 Streamable HTTP 的 GET 事件流，用于服务器主动推送。
     */
    @GetMapping("${fw-plus.ai.mcp.endpoint:/mcp}")
    public void get(HttpServletRequest request, HttpServletResponse response) throws IOException {
        ServerStreamableHttpRegistry transport = streamableRegistry();
        if (transport == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        response.setCharacterEncoding("UTF-8");
        AsyncContext async = request.startAsync();
        // 会话过期与心跳由内核负责，适配层不设置异步超时。
        async.setTimeout(0);
        String session = request.getHeader(sessionIdHeader());
        AtomicBoolean ended = new AtomicBoolean();
        PrintWriter writer = new PrintWriter(response.getWriter()) {
            @Override
            public void close() {
                if (ended.compareAndSet(false, true)) {
                    super.close();
                    try {
                        async.complete();
                    } catch (IllegalStateException ignored) {
                        // 异步上下文已由其它路径结束
                    }
                }
            }
        };

        async.addListener(new AsyncListener() {
            private void cleanup() {
                if (session != null)
                    transport.closeEventStream(session, writer);
                writer.close();
            }

            @Override
            public void onComplete(AsyncEvent event) {
                cleanup();
            }

            @Override
            public void onTimeout(AsyncEvent event) {
                cleanup();
            }

            @Override
            public void onError(AsyncEvent event) {
                cleanup();
            }

            @Override
            public void onStartAsync(AsyncEvent event) {
                // 无需处理
            }
        });

        try {
            // 心跳与适配层并发提交响应头时会互相覆盖，需要与内核同步。
            synchronized (transport) {
                HttpResult result = transport.openEventStream(session, writer, headers(request));
                response.setHeader("Cache-Control", "no-cache");
                response.setHeader("X-Accel-Buffering", "no");
                write(result, response);
                response.flushBuffer();

                if (result.getStatus() != HttpServletResponse.SC_OK)
                    writer.close();
            }
        } catch (IOException | RuntimeException failure) {
            if (session != null)
                transport.closeEventStream(session, writer);

            writer.close();
            throw failure;
        }
    }
}
