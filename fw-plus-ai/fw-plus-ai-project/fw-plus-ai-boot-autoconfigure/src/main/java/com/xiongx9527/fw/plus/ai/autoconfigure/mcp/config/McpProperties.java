package com.xiongx9527.fw.plus.ai.autoconfigure.mcp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * MCP 配置属性。
 * <p>
 * 顶层配置即 default runtime（key 为 {@code default}）；{@code servers[]} 追加命名 runtime，
 * 每项可覆盖 {@code server/transport/endpoint/basePackages}，未配置的字段继承顶层默认值。
 * </p>
 */
@ConfigurationProperties("fw-plus.ai.mcp")
public class McpProperties {

    private boolean enabled = true;

    private String key;

    private Server server = new Server();

    private String transport = "http-sse";

    private String basePackages; // 逗号分隔的多个包路径，与 @SpringBootConfiguration 启动类所在包取并集

    private String endpoint = "/mcp";

    /**
     * 命名 server 列表；每项为一个可独立启动的 MCP runtime。
     */
    private List<McpProperties> servers = new ArrayList<>();

    /**
     * WebFlux 栈同步内核调用使用的专用线程池大小。
     */
    private int threadPoolSize = 16;

    /**
     * 已显式设置的字段名；用于区分「用户配了这个字段」与「沿用内置默认值」。
     */
    private final Set<String> explicitFields = new HashSet<>();

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Server getServer() {
        return server;
    }

    public void setServer(Server server) {
        this.server = server;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getTransport() {
        return transport;
    }

    public void setTransport(String transport) {
        this.transport = transport;
        this.explicitFields.add("transport");
    }

    public String getBasePackages() {
        return basePackages;
    }

    public void setBasePackages(String basePackages) {
        this.basePackages = basePackages;
        this.explicitFields.add("basePackages");
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
        this.explicitFields.add("endpoint");
    }

    /**
     * {@code transport} 是否被显式设置过（Spring 绑定或手动赋值）。
     *
     * @return 显式设置过返回 {@code true}
     */
    public boolean isTransportExplicit() {
        return explicitFields.contains("transport");
    }

    /**
     * {@code endpoint} 是否被显式设置过（Spring 绑定或手动赋值）。
     *
     * @return 显式设置过返回 {@code true}
     */
    public boolean isEndpointExplicit() {
        return explicitFields.contains("endpoint");
    }

    /**
     * {@code basePackages} 是否被显式设置过（Spring 绑定或手动赋值）。
     *
     * @return 显式设置过返回 {@code true}
     */
    public boolean isBasePackagesExplicit() {
        return explicitFields.contains("basePackages");
    }

    public List<McpProperties> getServers() {
        return servers;
    }

    public void setServers(List<McpProperties> servers) {
        this.servers = servers;
    }

    public int getThreadPoolSize() {
        return threadPoolSize;
    }

    public void setThreadPoolSize(int threadPoolSize) {
        this.threadPoolSize = threadPoolSize;
    }

    public static class Server {

        private String name = "fw-plus-mcp-server";

        private String version = "1.0.0";

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }
    }
}
