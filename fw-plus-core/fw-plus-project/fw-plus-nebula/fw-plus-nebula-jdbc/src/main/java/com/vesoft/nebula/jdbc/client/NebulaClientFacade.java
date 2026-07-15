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

        Session openedSession = null;
        boolean poolInitialized = false;
        try {
            poolInitialized = pool.init(parseAddresses(config.getGraphAddress()), poolConfig);
            if (!poolInitialized) {
                poolInitialized = true;
                throw new SQLException("Initialize Nebula graph pool failed for graphAddress: " + config.getGraphAddress());
            }
            openedSession = pool.getSession(config.getUsername(), config.getPassword(), true);
            ResultSet useResult = openedSession.execute("use " + config.getGraphspace());
            if (!useResult.isSucceeded()) {
                throw new SQLException(String.format("Access graph space [%s] failed. Error code: %d, Error message: %s",
                        config.getGraphspace(), useResult.getErrorCode(), useResult.getErrorMessage()));
            }
            session = openedSession;
            closed = false;
        } catch (SQLException exception) {
            releaseFailedOpen(openedSession, poolInitialized);
            throw exception;
        } catch (UnknownHostException | InvalidConfigException | NotValidConnectionException |
                 IOErrorException | AuthFailedException | ClientServerIncompatibleException exception) {
            releaseFailedOpen(openedSession, poolInitialized);
            throw new SQLException(exception.getMessage(), exception);
        }
    }

    public ResultSet executeNgql(String ngql) throws SQLException {
        if (closed) {
            throw new SQLException("Nebula client facade is closed.");
        }
        if (session == null) {
            throw new SQLException("Nebula client facade is not open.");
        }
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

    private void releaseFailedOpen(Session openedSession, boolean poolInitialized) {
        if (openedSession != null) {
            openedSession.release();
        }
        if (poolInitialized) {
            pool.close();
        }
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
            try {
                addresses.add(new HostAddress(hostPort[0], Integer.parseInt(hostPort[1])));
            } catch (NumberFormatException exception) {
                throw new SQLException("Invalid graphAddress port in address: " + addressPart, exception);
            }
        }
        return addresses;
    }
}
