package com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server.runtime;

import com.ajaxjs.mcp.server.McpServer;
import com.ajaxjs.mcp.server.common.ServerConfig;
import com.ajaxjs.mcp.server.feature.FeatureMgr;
import com.ajaxjs.mcp.transport.McpTransportSync;
import com.xiongx9527.fw.plus.auth.core.AuthService;
import com.xiongx9527.fw.plus.auth.core.InstanceAuthRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server.cluster.McpRedisBroadcaster;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server.cluster.RedisServerSseRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server.cluster.RedisServerStreamableHttpRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server.config.McpProperties;
import com.xiongx9527.fw.plus.ai.mcp.server.feature.McpFeatureScanner;
import com.xiongx9527.fw.plus.ai.mcp.server.spi.BaseServerRegistry;
import com.xiongx9527.fw.plus.ai.mcp.server.support.DefaultServerSseRegistry;
import com.xiongx9527.fw.plus.ai.mcp.server.support.DefaultServerStreamableHttpRegistry;
import org.springframework.beans.factory.ObjectProvider;

import java.util.ArrayList;
import java.util.Collections;
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
     * MCP server 实例工厂。默认创建内核 {@link McpServer}；插件可提供子类以接管消息处理。
     */
    private final McpServerFactory serverFactory;

    public McpRuntimeFactory() {
        this(McpServerFactory.DEFAULT);
    }

    public McpRuntimeFactory(McpServerFactory serverFactory) {
        this.serverFactory = serverFactory == null ? McpServerFactory.DEFAULT : serverFactory;
    }

    /**
     * 构建全部 runtime。
     *
     * @param top               顶层配置
     * @param authRegistry      按实例 provider 解析认证 SPI；未配置或解析失败时拒绝请求
     * @param broadcasterProvider Redis 广播器提供者；Redis 未就绪时为 {@code null}
     * @return 按顺序排列的 runtime 列表，首位恒为 default
     */
    public List<McpServerRuntime> createRuntimes(McpProperties top, InstanceAuthRegistry authRegistry,
            ObjectProvider<McpRedisBroadcaster> broadcasterProvider) {
        McpRedisBroadcaster broadcaster = broadcasterProvider == null ? null : broadcasterProvider.getIfAvailable();

        List<McpServerRuntime> runtimes = new ArrayList<>();
        runtimes.add(createRuntime(McpFeatureScanner.DEFAULT_KEY, inherited(top, top), authRegistry,
                broadcaster));

        if (top.getServers() != null) {
            for (McpProperties item : top.getServers()) {
                String key = item.getKey();
                if (key == null || key.trim().isEmpty()) {
                    throw new IllegalArgumentException(
                            "fw-plus.ai.mcp.servers[].key must not be blank");
                }
                runtimes.add(createRuntime(key.trim(), inherited(top, item), authRegistry, broadcaster));
            }
        }

        validateTransports(runtimes);
        validateEndpoints(runtimes);
        validateKeys(runtimes);
        return runtimes;
    }

    public McpServerRuntime createRuntime(String key, McpProperties effective,
            InstanceAuthRegistry authRegistry,
            McpRedisBroadcaster broadcaster) {
        return createRuntime(key, effective, authRegistry, broadcaster, serverFactory);
    }

    /**
     * 构建单个 runtime，允许调用方指定该实例的 server 工厂。
     *
     * @param key         实例 key
     * @param effective   该实例的生效配置
     * @param authRegistry 认证 SPI 注册表
     * @param broadcaster Redis 广播器，可为 {@code null}
     * @param factory     server 工厂；为 {@code null} 时回落到默认工厂
     * @return runtime 实例
     */
    public McpServerRuntime createRuntime(String key, McpProperties effective,
            InstanceAuthRegistry authRegistry,
            McpRedisBroadcaster broadcaster, McpServerFactory factory) {
        AuthService authService = authRegistry == null
                ? null : authRegistry.resolve(key, toCoreAuth(effective.getAuth()));
        return createRuntime(key, effective, broadcaster, factory, authService);
    }

    /**
     * 构建 runtime 并绑定已解析的认证服务。
     */
    public McpServerRuntime createRuntime(String key, McpProperties effective,
            McpRedisBroadcaster broadcaster, McpServerFactory factory, AuthService authService) {
        ServerConfig config = new ServerConfig();
        if (effective.getServer() != null) {
            config.setName(effective.getServer().getName());
            config.setVersion(effective.getServer().getVersion());
        }

        FeatureMgr featureMgr = new FeatureMgr();
        McpServer server = (factory == null ? serverFactory : factory).create(config, featureMgr);
        server.setServerConfig(config);
        server.setFeatureMgr(featureMgr);

        BaseServerRegistry registry = registry(server, effective.getTransport(), broadcaster, key);
        if (registry instanceof McpTransportSync) {
            server.setTransport((McpTransportSync) registry);
        }
        return new McpServerRuntime(key, effective, server, featureMgr, registry,
                authService,
                splitBasePackages(effective.getBasePackages()));
    }

    /**
     * 按顶层配置继承规则构建单个 runtime，并执行 transport/endpoint/key 校验。
     *
     * @param top         顶层配置
     * @param item        实例配置
     * @param authRegistry 按实例 provider 解析的认证 SPI 注册表
     * @param broadcaster Redis 广播器，可为 {@code null}
     * @return runtime 实例
     */
    public McpServerRuntime createRuntime(McpProperties top, McpProperties item,
            InstanceAuthRegistry authRegistry,
            McpRedisBroadcaster broadcaster) {
        return createRuntime(top, item, authRegistry, broadcaster, null);
    }

    /**
     * 按顶层配置继承规则构建单个 runtime，允许指定该实例的 server 工厂。
     * <p>
     * 认证服务按实例生效配置中的 provider 解析（见 {@link InstanceAuthRegistry#resolve}），
     * 未配置 provider 时 runtime 保持未认证。
     * </p>
     *
     * @param top         顶层配置
     * @param item        实例配置
     * @param authRegistry 认证 SPI 注册表
     * @param broadcaster Redis 广播器，可为 {@code null}
     * @param factory     server 工厂；为 {@code null} 时回落到默认工厂
     * @return runtime 实例
     */
    public McpServerRuntime createRuntime(McpProperties top, McpProperties item,
            InstanceAuthRegistry authRegistry,
            McpRedisBroadcaster broadcaster, McpServerFactory factory) {
        String key = item.getKey() == null || item.getKey().trim().isEmpty() ? top.getKey() : item.getKey().trim();
        McpProperties effective = inherited(top, item);
        AuthService authService = authRegistry == null
                ? null : authRegistry.resolve(key, toCoreAuth(effective.getAuth()));
        McpServerRuntime runtime = createRuntime(key, effective, broadcaster, factory, authService);
        validateTransports(Collections.singletonList(runtime));
        validateEndpoints(Collections.singletonList(runtime));
        validateKeys(Collections.singletonList(runtime));
        return runtime;
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
        effective.setAuth(item == top ? top.getAuth() : item.getAuth());
        effective.setScopeCheckEnabled(item.isScopeCheckEnabled() || top.isScopeCheckEnabled());
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

    private com.xiongx9527.fw.plus.auth.core.config.InstanceAuthProperties toCoreAuth(McpProperties.Auth auth) {
        if (auth == null) {
            return null;
        }
        com.xiongx9527.fw.plus.auth.core.config.InstanceAuthProperties core =
                new com.xiongx9527.fw.plus.auth.core.config.InstanceAuthProperties();
        core.setProvider(auth.getProvider());
        return core;
    }
}
