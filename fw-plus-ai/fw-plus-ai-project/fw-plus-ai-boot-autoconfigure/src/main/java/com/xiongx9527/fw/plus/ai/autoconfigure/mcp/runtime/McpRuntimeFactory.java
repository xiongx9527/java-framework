package com.xiongx9527.fw.plus.ai.autoconfigure.mcp.runtime;

import com.ajaxjs.mcp.server.McpServer;
import com.ajaxjs.mcp.server.common.ServerConfig;
import com.ajaxjs.mcp.server.feature.FeatureMgr;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.auth.McpAuthService;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.cluster.McpRedisBroadcaster;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.cluster.RedisServerSseRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.cluster.RedisServerStreamableHttpRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.config.McpProperties;
import com.xiongx9527.fw.plus.ai.mcp.feature.McpFeatureScanner;
import com.xiongx9527.fw.plus.ai.mcp.spi.BaseServerRegistry;
import com.xiongx9527.fw.plus.ai.mcp.support.DefaultServerSseRegistry;
import com.xiongx9527.fw.plus.ai.mcp.support.DefaultServerStreamableHttpRegistry;
import org.springframework.beans.factory.ObjectProvider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * MCP runtime 工厂：按配置构建 default 与命名 runtime 列表，并校验 endpoint 唯一性。
 * <p>
 * default runtime 由顶层配置构建（key 为 {@link McpFeatureScanner#DEFAULT_KEY}）；{@code servers[]}
 * 每项构建一个命名 runtime，未配置的字段继承顶层默认值。endpoint 唯一性按「同 transport 下
 * default+servers 不重复」校验，冲突时抛出 {@link IllegalArgumentException}。
 * </p>
 */
public class McpRuntimeFactory {

    /**
     * 构建全部 runtime。
     *
     * @param top               顶层配置
     * @param authService       认证 SPI；为 {@code null} 时按放行处理
     * @param broadcasterProvider Redis 广播器提供者；Redis 未就绪时为 {@code null}
     * @return 按顺序排列的 runtime 列表，首位恒为 default
     */
    public List<McpServerRuntime> createRuntimes(McpProperties top, McpAuthService authService,
            ObjectProvider<McpRedisBroadcaster> broadcasterProvider) {
        McpRedisBroadcaster broadcaster = broadcasterProvider == null ? null : broadcasterProvider.getIfAvailable();

        List<McpServerRuntime> runtimes = new ArrayList<>();
        runtimes.add(createRuntime(McpFeatureScanner.DEFAULT_KEY, inherited(top, top), authService, broadcaster));

        if (top.getServers() != null) {
            for (McpProperties item : top.getServers()) {
                String key = item.getKey();
                if (key == null || key.trim().isEmpty()) {
                    throw new IllegalArgumentException(
                            "fw-plus.ai.mcp.servers[].key must not be blank");
                }
                runtimes.add(createRuntime(key.trim(), inherited(top, item), authService, broadcaster));
            }
        }

        validateEndpoints(runtimes);
        return runtimes;
    }

    private McpServerRuntime createRuntime(String key, McpProperties effective, McpAuthService authService,
            McpRedisBroadcaster broadcaster) {
        ServerConfig config = new ServerConfig();
        if (effective.getServer() != null) {
            config.setName(effective.getServer().getName());
            config.setVersion(effective.getServer().getVersion());
        }

        FeatureMgr featureMgr = new FeatureMgr();
        McpServer server = new McpServer();
        server.setServerConfig(config);
        server.setFeatureMgr(featureMgr);

        BaseServerRegistry registry = registry(server, effective.getTransport(), broadcaster, key);
        return new McpServerRuntime(key, effective, server, featureMgr, registry, authService,
                splitBasePackages(effective.getBasePackages()));
    }

    private BaseServerRegistry registry(McpServer server, String transport, McpRedisBroadcaster broadcaster, String key) {
        if ("streamable-http".equalsIgnoreCase(transport)) {
            if (broadcaster != null) {
                return new RedisServerStreamableHttpRegistry(server, broadcaster, key);
            }
            return new DefaultServerStreamableHttpRegistry(server, UUID.randomUUID().toString());
        }
        if (broadcaster != null) {
            return new RedisServerSseRegistry(server, broadcaster, key);
        }
        return new DefaultServerSseRegistry(server, UUID.randomUUID().toString());
    }

    /**
     * 命名项未显式配置的字段继承顶层默认值。
     *
     * @param top  顶层配置
     * @param item 命名项配置，可为顶层自身（此时原样继承）
     * @return 合并后的生效配置
     */
    private McpProperties inherited(McpProperties top, McpProperties item) {
        McpProperties effective = new McpProperties();
        effective.setEnabled(top.isEnabled());
        effective.setKey(item.getKey() == null || item.getKey().trim().isEmpty() ? top.getKey() : item.getKey());
        effective.setServer(inheritedServer(top.getServer(), item.getServer()));
        effective.setTransport(notBlank(item.getTransport()) ? item.getTransport() : top.getTransport());
        effective.setEndpoint(notBlank(item.getEndpoint()) ? item.getEndpoint() : top.getEndpoint());
        effective.setBasePackages(notBlank(item.getBasePackages()) ? item.getBasePackages() : top.getBasePackages());
        effective.setThreadPoolSize(item.getThreadPoolSize() > 0 ? item.getThreadPoolSize() : top.getThreadPoolSize());
        return effective;
    }

    private McpProperties.Server inheritedServer(McpProperties.Server top, McpProperties.Server item) {
        McpProperties.Server effective = new McpProperties.Server();
        if (item != null) {
            effective.setName(notBlank(item.getName()) ? item.getName() : top.getName());
            effective.setVersion(notBlank(item.getVersion()) ? item.getVersion() : top.getVersion());
        } else {
            effective.setName(top.getName());
            effective.setVersion(top.getVersion());
        }
        return effective;
    }

    /**
     * 同一 transport 下 endpoint 必须唯一；不同 transport 允许复用同一 endpoint。
     *
     * @param runtimes 全部 runtime
     * @throws IllegalArgumentException 出现冲突时
     */
    private void validateEndpoints(List<McpServerRuntime> runtimes) {
        Map<String, String> seen = new HashMap<>();
        for (McpServerRuntime runtime : runtimes) {
            String signature = runtime.getTransport() + "|" + runtime.getEndpoint();
            String previousKey = seen.putIfAbsent(signature, runtime.getKey());
            if (previousKey != null) {
                throw new IllegalArgumentException("MCP endpoint conflict: server key '" + runtime.getKey()
                        + "' declares transport '" + runtime.getTransport() + "' endpoint '" + runtime.getEndpoint()
                        + "', which is already declared by server key '" + previousKey + "'");
            }
        }
    }

    private List<String> splitBasePackages(String basePackages) {
        List<String> packages = new ArrayList<>();
        if (basePackages == null) {
            return packages;
        }
        for (String candidate : basePackages.split(",")) {
            String trimmed = candidate.trim();
            if (!trimmed.isEmpty()) {
                packages.add(trimmed);
            }
        }
        return packages;
    }

    private boolean notBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }
}