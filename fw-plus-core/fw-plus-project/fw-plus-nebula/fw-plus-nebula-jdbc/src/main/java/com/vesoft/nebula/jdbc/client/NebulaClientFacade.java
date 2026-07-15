package com.vesoft.nebula.jdbc.client;

import com.vesoft.nebula.client.graph.NebulaPoolConfig;
import com.vesoft.nebula.client.graph.data.HostAddress;
import com.vesoft.nebula.client.graph.data.ResultSet;
import com.vesoft.nebula.client.graph.exception.AuthFailedException;
import com.vesoft.nebula.client.graph.exception.ClientServerIncompatibleException;
import com.vesoft.nebula.client.graph.exception.IOErrorException;
import com.vesoft.nebula.client.graph.exception.InvalidConfigException;
import com.vesoft.nebula.client.graph.exception.NotValidConnectionException;
import com.vesoft.nebula.client.graph.net.NebulaPool;
import com.vesoft.nebula.client.graph.net.Session;
import com.vesoft.nebula.jdbc.config.NebulaConnectionConfig;

import java.net.UnknownHostException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class NebulaClientFacade implements AutoCloseable {
    private final NebulaPool pool;
    private Session session;
    private boolean closed;

    public NebulaClientFacade() {
        this(new NebulaPool());
    }

    public NebulaClientFacade(NebulaPool pool) {
        this.pool = pool;
    }

    public void open(NebulaConnectionConfig config) throws SQLException {
        NebulaPoolConfig poolConfig = new NebulaPoolConfig();
        poolConfig.setMinConnSize(config.getMinConnsSize());
        poolConfig.setMaxConnSize(config.getMaxConnsSize());
        poolConfig.setTimeout(config.getTimeout());
        poolConfig.setIdleTime(config.getIdleTime());
        poolConfig.setIntervalIdle(config.getIntervalIdle());
        poolConfig.setWaitTime(config.getWaitTime());

        try {
            pool.init(parseAddresses(config.getGraphAddress()), poolConfig);
            session = pool.getSession(config.getUsername(), config.getPassword(), true);
            ResultSet useResult = session.execute("use " + config.getGraphspace());
            if (!useResult.isSucceeded()) {
                throw new SQLException(String.format("Access graph space [%s] failed. Error code: %d, Error message: %s",
                        config.getGraphspace(), useResult.getErrorCode(), useResult.getErrorMessage()));
            }
            closed = false;
        } catch (UnknownHostException | InvalidConfigException | NotValidConnectionException |
                 IOErrorException | AuthFailedException | ClientServerIncompatibleException exception) {
            throw new SQLException(exception.getMessage(), exception);
        }
    }

    public ResultSet executeNgql(String ngql) throws SQLException {
        try {
            return session.execute(ngql);
        } catch (IOErrorException exception) {
            throw new SQLException(exception.getMessage(), exception);
        }
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        if (session != null) {
            session.release();
        }
        pool.close();
    }

    private static List<HostAddress> parseAddresses(String graphAddress) throws SQLException {
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
            addresses.add(new HostAddress(hostPort[0], Integer.parseInt(hostPort[1])));
        }
        return addresses;
    }
}
