package com.xiongx9527.fw.plus.ai.mcp;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/** MCP 客户端连接配置（stdio / http-sse 两种传输），镜像 langchain4j {@code langchain4j-mcp} 的传输配置（Spring AI 等价 McpClient 配置）。 */
public class McpClientConfig {
    public enum TransportType { STDIO, HTTP }
    private final TransportType type;
    private final String command;
    private final List<String> args;
    private final Map<String, String> env;
    private final String url;
    private final Map<String, String> headers;
    private McpClientConfig(Builder b) {
        this.type = b.type; this.command = b.command; this.args = b.args;
        this.env = b.env; this.url = b.url; this.headers = b.headers;
    }
    public TransportType type() { return type; }
    public String command() { return command; }
    public List<String> args() { return args; }
    public Map<String, String> env() { return env; }
    public String url() { return url; }
    public Map<String, String> headers() { return headers; }
    public static Builder stdio(String command) { return new Builder(TransportType.STDIO).command(command); }
    public static Builder http(String url) { return new Builder(TransportType.HTTP).url(url); }
    public static class Builder {
        private TransportType type; private String command; private List<String> args = Collections.emptyList();
        private Map<String, String> env = Collections.emptyMap(); private String url;
        private Map<String, String> headers = Collections.emptyMap();
        private Builder(TransportType t) { this.type = t; }
        public Builder command(String c) { this.command = c; return this; }
        public Builder args(List<String> a) { this.args = a; return this; }
        public Builder env(Map<String, String> e) { this.env = e; return this; }
        public Builder url(String u) { this.url = u; return this; }
        public Builder headers(Map<String, String> h) { this.headers = h; return this; }
        public McpClientConfig build() { return new McpClientConfig(this); }
    }
}
