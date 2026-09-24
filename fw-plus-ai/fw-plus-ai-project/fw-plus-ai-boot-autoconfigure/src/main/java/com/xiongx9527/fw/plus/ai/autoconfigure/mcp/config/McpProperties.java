package com.xiongx9527.fw.plus.ai.autoconfigure.mcp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * MCP 配置属性。
 */
@ConfigurationProperties("fw-plus.ai.mcp")
public class McpProperties {

    private boolean enabled = true;

    private Server server = new Server();

    private String transport = "http-sse";

    private String basePackages; // 逗号分隔的多个包路径，与 @SpringBootConfiguration 启动类所在包取并集

    private String endpoint = "/mcp";

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
    }

    public String getBasePackages() {
        return basePackages;
    }

    public void setBasePackages(String basePackages) {
        this.basePackages = basePackages;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
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
