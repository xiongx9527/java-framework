package com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server.auth;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 一次 MCP 请求的栈无关描述，供认证 SPI 消费。
 * <p>
 * 认证服务处理一次请求所需的协议、会话、正文与 header 上下文。
 * </p>
 */
public class McpRequest {

    private final String sessionId;

    private final String runtimeKey;

    private final String transport;

    private final String endpoint;

    private final String body;

    private final Map<String, String> headers;

    public McpRequest(String sessionId, String transport, String endpoint, String body, Map<String, String> headers) {
        this(null, sessionId, transport, endpoint, body, headers);
    }

    public McpRequest(String runtimeKey, String sessionId, String transport, String endpoint, String body,
            Map<String, String> headers) {
        this.runtimeKey = runtimeKey;
        this.sessionId = sessionId;
        this.transport = transport;
        this.endpoint = endpoint;
        this.body = body;
        this.headers = headers == null
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<>(headers));
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getRuntimeKey() {
        return runtimeKey;
    }

    public String getTransport() {
        return transport;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public String getBody() {
        return body;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }
}
