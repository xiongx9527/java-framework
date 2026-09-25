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

        validateTransports(runtimes);
        validateEndpoints(runtimes);
        validateKeys(runtimes);
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
        effective.setTransport(configured(item.isTransportExplicit(), item.getTransport())
                ? item.getTransport() : top.getTransport());
        effective.setEndpoint(configured(item.isEndpointExplicit(), item.getEndpoint())
                ? item.getEndpoint() : top.getEndpoint());
        effective.setBasePackages(configured(item.isBasePackagesExplicit(), item.getBasePackages())
                ? item.getBasePackages() : top.getBasePackages());
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
     * transport 必须为 http-sse 或 streamable-http；其它取值（拼写错误等）启动即失败，
     * 避免被静默当作另一种传输处理而导致路由/注册表错位。
     *
     * @param runtimes 全部 runtime
     * @throws IllegalArgumentException 出现不支持的 transport 时
     */
    private void validateTransports(List<McpServerRuntime> runtimes) {
        for (McpServerRuntime runtime : runtimes) {
            String transport = runtime.getTransport();
            if (transport == null || (!"http-sse".equalsIgnoreCase(transport)
                    && !"streamable-http".equalsIgnoreCase(transport))) {
                throw new IllegalArgumentException("MCP server key '" + runtime.getKey()
                        + "' declares unsupported transport '" + transport
                        + "'; supported transports are 'http-sse' and 'streamable-http'");
            }
        }
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
            String transport = runtime.getTransport() == null ? ""
                    : runtime.getTransport().toLowerCase(java.util.Locale.ROOT);
            String signature = transport + "|" + runtime.getEndpoint();
            String previousKey = seen.putIfAbsent(signature, runtime.getKey());
            if (previousKey != null) {
                throw new IllegalArgumentException("MCP endpoint conflict: server key '" + runtime.getKey()
                        + "' declares transport '" + runtime.getTransport() + "' endpoint '" + runtime.getEndpoint()
                        + "', which is already declared by server key '" + previousKey + "'");
            }
        }
    }

    /**
     * runtime key 必须全局唯一：default 与命名 servers 之间、servers 内部均不得重复，
     * 否则以 key 为键的能力归属/集群路由会发生静默覆盖。
     *
     * @param runtimes 全部 runtime
     * @throws IllegalArgumentException 出现重复 key 时
     */
    private void validateKeys(List<McpServerRuntime> runtimes) {
        Map<String, String> seen = new HashMap<>();
        for (McpServerRuntime runtime : runtimes) {
            String previousPeer = seen.put(runtime.getKey(), runtime.getKey());
            if (previousPeer != null) {
                throw new IllegalArgumentException("MCP server key '" + runtime.getKey()
                        + "' is declared more than once; each runtime key must be unique");
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

    /**
     * 字段是否由用户显式配置：setter 被调用过（Spring 绑定或手动赋值）且值非空白。
     * <p>
     * {@code transport}、{@code endpoint}、{@code basePackages} 带有内置默认值，
     * 无法靠「值是否为空」判断命名项是否配过，必须结合 setter 调用标记。
     * </p>
     *
     * @param explicit setter 调用标记
     * @param value    字段值
     * @return 显式配置了有效值返回 {@code true}
     */
    private boolean configured(boolean explicit, String value) {
        return explicit && notBlank(value);
    }
}
