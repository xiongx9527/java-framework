package com.xiongx9527.fw.plus.ai.base.mcp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * MCP 客户端配置：一条连接怎么建。
 *
 * <p>STDIO 用 {@code command / args / env}；远程传输用 {@code url / headers}。
 * 两套字段放在同一个配置里，由 {@link McpTransport} 决定哪些生效——避免为每种传输
 * 各建一个配置类型，调用方（配置中心 / 数据库）只需填一条记录。</p>
 */
public final class McpClientConfig {

    private final String name;

    private final McpTransport transport;

    private final String command;

    private final List<String> args;

    private final Map<String, String> env;

    private final String url;

    private final Map<String, String> headers;

    private final long timeoutMs;

    private McpClientConfig(Builder builder) {
        this.name = builder.name;
        this.transport = builder.transport == null ? McpTransport.STDIO : builder.transport;
        this.command = builder.command;
        this.args = Collections.unmodifiableList(new ArrayList<String>(builder.args));
        this.env = Collections.unmodifiableMap(new LinkedHashMap<String, String>(builder.env));
        this.headers = Collections.unmodifiableMap(new LinkedHashMap<String, String>(builder.headers));
        this.url = builder.url;
        this.timeoutMs = builder.timeoutMs;
    }

    public static Builder builder() {
        return new Builder();
    }

    /** 连接名（用于日志与工具名前缀）。 */
    public String getName() {
        return name;
    }

    public McpTransport getTransport() {
        return transport;
    }

    public String getCommand() {
        return command;
    }

    public List<String> getArgs() {
        return args;
    }

    public Map<String, String> getEnv() {
        return env;
    }

    public String getUrl() {
        return url;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public long getTimeoutMs() {
        return timeoutMs;
    }

    public static final class Builder {

        private final List<String> args = new ArrayList<String>();

        private final Map<String, String> env = new LinkedHashMap<String, String>();

        private final Map<String, String> headers = new LinkedHashMap<String, String>();

        private String name;

        private McpTransport transport = McpTransport.STDIO;

        private String command;

        private String url;

        private long timeoutMs = 30000L;

        private Builder() {
        }

        public Builder name(String value) {
            this.name = value;
            return this;
        }

        public Builder transport(McpTransport value) {
            this.transport = value;
            return this;
        }

        public Builder command(String value) {
            this.command = value;
            return this;
        }

        public Builder args(List<String> value) {
            this.args.clear();
            if (value != null) {
                this.args.addAll(value);
            }
            return this;
        }

        public Builder env(String key, String value) {
            if (key != null) {
                this.env.put(key, value);
            }
            return this;
        }

        public Builder url(String value) {
            this.url = value;
            return this;
        }

        public Builder header(String key, String value) {
            if (key != null) {
                this.headers.put(key, value);
            }
            return this;
        }

        public Builder timeoutMs(long value) {
            this.timeoutMs = value;
            return this;
        }

        public McpClientConfig build() {
            return new McpClientConfig(this);
        }
    }
}
