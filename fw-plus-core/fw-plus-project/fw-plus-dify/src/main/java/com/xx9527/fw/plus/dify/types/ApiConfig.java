package com.xx9527.fw.plus.dify.types;

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
    private long connectTimeoutSeconds = 30L;

    /**
     * read timeoutreadTimeoutSeconds
     */
    private long readTimeoutSeconds = 30L;

    /**
     * write timeoutwriteTimeoutSeconds
     */
    private long writeTimeoutSeconds = 30L;

    /**
     * max idle connections
     */
    private int maxIdleConnections = 5;

    /**
     * keep alive duration minutes
     */
    private int keepAliveDurationMinutes = 5;

    /**
     * retry on connection failure
     */
    private boolean retryOnConnectionFailure = true;
}
