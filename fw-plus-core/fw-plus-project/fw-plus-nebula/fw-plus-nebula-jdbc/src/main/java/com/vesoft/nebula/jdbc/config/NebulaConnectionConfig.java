package com.vesoft.nebula.jdbc.config;

import com.vesoft.nebula.client.graph.data.HostAddress;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

public class NebulaConnectionConfig {
    private final String graphspace;
    private final String username;
    private final String password;
    private final List<HostAddress> graphAddresses;
    private final int minConnsSize;
    private final int maxConnsSize;
    private final int timeout;
    private final int idleTime;
    private final int intervalIdle;
    private final int waitTime;
    private final Properties mergedProperties;

    public NebulaConnectionConfig(String graphspace, String username, String password,
                                  List<HostAddress> graphAddresses, int minConnsSize,
                                  int maxConnsSize, int timeout, int idleTime,
                                  int intervalIdle, int waitTime, Properties mergedProperties) {
        this.graphspace = graphspace;
        this.username = username;
        this.password = password;
        this.graphAddresses = Collections.unmodifiableList(new ArrayList<>(graphAddresses));
        this.minConnsSize = minConnsSize;
        this.maxConnsSize = maxConnsSize;
        this.timeout = timeout;
        this.idleTime = idleTime;
        this.intervalIdle = intervalIdle;
        this.waitTime = waitTime;
        this.mergedProperties = copyProperties(mergedProperties);
    }

    public NebulaConnectionConfig(String graphAddress, String graphspace, String username, String password,
                                  int minConnsSize, int maxConnsSize, int timeout, int idleTime,
                                  int intervalIdle, int waitTime) throws SQLException {
        this(graphspace, username, password, parseGraphAddresses(graphAddress), minConnsSize, maxConnsSize,
                timeout, idleTime, intervalIdle, waitTime, new Properties());
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

    public List<HostAddress> getGraphAddresses() {
        return new ArrayList<>(graphAddresses);
    }

    public String getGraphAddress() {
        StringBuilder graphAddress = new StringBuilder("//");
        for (int i = 0; i < graphAddresses.size(); i++) {
            if (i > 0) {
                graphAddress.append(',');
            }
            HostAddress address = graphAddresses.get(i);
            graphAddress.append(address.getHost()).append(':').append(address.getPort());
        }
        return graphAddress.toString();
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

    public Properties getMergedProperties() {
        return copyProperties(mergedProperties);
    }

    private static Properties copyProperties(Properties source) {
        Properties copy = new Properties();
        if (source != null) {
            copy.putAll(source);
        }
        return copy;
    }

    private static List<HostAddress> parseGraphAddresses(String graphAddress) throws SQLException {
        if (graphAddress == null || !graphAddress.startsWith("//")) {
            throw new SQLException("graphAddress must start with //");
        }
        String[] addressParts = graphAddress.substring(2).split(",");
        List<HostAddress> addresses = new ArrayList<>();
        for (String addressPart : addressParts) {
            String[] hostPort = addressPart.split(":");
            if (hostPort.length != 2) {
                throw new SQLException("graphAddress has wrong host format.");
            }
            try {
                addresses.add(new HostAddress(hostPort[0], Integer.parseInt(hostPort[1])));
            } catch (NumberFormatException exception) {
                throw new SQLException("Invalid graphAddress port in address: " + addressPart, exception);
            }
        }
        return addresses;
    }
}
