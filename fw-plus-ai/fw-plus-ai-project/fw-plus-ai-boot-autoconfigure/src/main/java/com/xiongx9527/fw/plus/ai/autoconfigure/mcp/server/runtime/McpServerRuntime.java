package com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server.runtime;

import com.ajaxjs.mcp.server.McpServer;
import com.ajaxjs.mcp.server.feature.FeatureMgr;
import com.xiongx9527.fw.plus.auth.core.AuthService;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server.config.McpProperties;
import com.xiongx9527.fw.plus.ai.mcp.server.spi.BaseServerRegistry;
import com.xiongx9527.fw.plus.ai.mcp.server.spi.ServerSseRegistry;
import com.xiongx9527.fw.plus.ai.mcp.server.spi.ServerStreamableHttpRegistry;

import java.util.Collections;
import java.util.List;

/**
 * 单个 MCP server runtime：一个 key 对应一套独立的 {@link McpServer}、{@link FeatureMgr}
 * 与传输注册表。各 runtime 的会话表相互隔离，互不可见。
 * <p>
 * key 为 {@code default} 的 runtime 由顶层配置构建；其余来自 {@code servers[]}。
 * </p>
 */
public class McpServerRuntime {

    private final String key;

    private final McpProperties properties;

    private final McpServer server;

    private final FeatureMgr featureMgr;

    private final BaseServerRegistry registry;

    private final AuthService authService;

    private final List<String> basePackages;

    public McpServerRuntime(String key, McpProperties properties, McpServer server, FeatureMgr featureMgr,
                            BaseServerRegistry registry, AuthService authService, List<String> basePackages) {
        this.key = key;
        this.properties = properties;
        this.server = server;
        this.featureMgr = featureMgr;
        this.registry = registry;
        this.authService = authService;
        this.basePackages = basePackages == null ? Collections.<String>emptyList() : basePackages;
    }

    public String getKey() {
        return key;
    }

    public McpProperties getProperties() {
        return properties;
    }

    public McpServer getServer() {
        return server;
    }

    public FeatureMgr getFeatureMgr() {
        return featureMgr;
    }

    public BaseServerRegistry getRegistry() {
        return registry;
    }

    public AuthService getAuthService() {
        return authService;
    }

    public String getEndpoint() {
        return properties == null ? null : properties.getEndpoint();
    }

    public String getTransport() {
        return properties == null ? null : properties.getTransport();
    }

    public List<String> getBasePackages() {
        return basePackages;
    }

    /**
     * 本 runtime 的会话表是否持有指定会话。
     *
     * @param sessionId 会话 id
     * @return 持有返回 {@code true}；不支持的注册表类型返回 {@code false}
     */
    public boolean hasSession(String sessionId) {
        if (registry instanceof ServerSseRegistry) {
            return ((ServerSseRegistry) registry).hasSession(sessionId);
        }
        if (registry instanceof ServerStreamableHttpRegistry) {
            return ((ServerStreamableHttpRegistry) registry).hasSession(sessionId);
        }
        return false;
    }
}