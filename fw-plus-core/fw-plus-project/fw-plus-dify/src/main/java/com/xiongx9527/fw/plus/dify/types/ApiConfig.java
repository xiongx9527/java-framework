package com.xiongx9527.fw.plus.dify.types;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class ApiConfig {
    /**
     * Dify Server Address
     * <p>默认 https://api.dify.ai/v1（Dify 官方云服务），自部署时传入自有基址覆盖。</p>
     */
    @Builder.Default
    private String server = "https://api.dify.ai/v1";

    /**
     * Api Key
     */
    private String apiKey;

    /**
     * refreshToken
     */
    private String refreshToken;

    /**
     * connect timeoutconnectTimeoutSeconds
     */
    @Builder.Default
    private long connectTimeoutSeconds = 30L;

    /**
     * read timeoutreadTimeoutSeconds
     */
    @Builder.Default
    private long readTimeoutSeconds = 30L;

    /**
     * write timeoutwriteTimeoutSeconds
     */
    @Builder.Default
    private long writeTimeoutSeconds = 30L;

    /**
     * max idle connections
     */
    @Builder.Default
    private int maxIdleConnections = 5;

    /**
     * keep alive duration minutes
     */
    @Builder.Default
    private int keepAliveDurationMinutes = 5;

    /**
     * retry on connection failure
     */
    @Builder.Default
    private boolean retryOnConnectionFailure = true;
}
