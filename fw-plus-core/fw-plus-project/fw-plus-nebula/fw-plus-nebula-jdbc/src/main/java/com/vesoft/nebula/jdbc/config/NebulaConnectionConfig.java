package com.vesoft.nebula.jdbc.config;

public class NebulaConnectionConfig {
    private final String graphAddress;
    private final String graphspace;
    private final String username;
    private final String password;
    private final int minConnsSize;
    private final int maxConnsSize;
    private final int timeout;
    private final int idleTime;
    private final int intervalIdle;
    private final int waitTime;

    public NebulaConnectionConfig(String graphAddress, String graphspace, String username, String password,
                                  int minConnsSize, int maxConnsSize, int timeout, int idleTime,
                                  int intervalIdle, int waitTime) {
        this.graphAddress = graphAddress;
        this.graphspace = graphspace;
        this.username = username;
        this.password = password;
        this.minConnsSize = minConnsSize;
        this.maxConnsSize = maxConnsSize;
        this.timeout = timeout;
        this.idleTime = idleTime;
        this.intervalIdle = intervalIdle;
        this.waitTime = waitTime;
    }

    public String getGraphAddress() {
        return graphAddress;
    }

    public String getGraphspace() {
        return graphspace;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public int getMinConnsSize() {
        return minConnsSize;
    }

    public int getMaxConnsSize() {
        return maxConnsSize;
    }

    public int getTimeout() {
        return timeout;
    }

    public int getIdleTime() {
        return idleTime;
    }

    public int getIntervalIdle() {
        return intervalIdle;
    }

    public int getWaitTime() {
        return waitTime;
    }
}
