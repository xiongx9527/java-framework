package com.xiongx9527.fw.plus.dify.types;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class ApiConfig {
    /**
     * Dify Server Address
     */
    private String server;

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
