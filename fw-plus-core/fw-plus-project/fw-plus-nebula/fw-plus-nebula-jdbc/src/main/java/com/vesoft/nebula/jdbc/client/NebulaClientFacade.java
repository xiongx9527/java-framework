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

public class NebulaClientFacade implements AutoCloseable {
    private final NebulaPoolFactory poolFactory;
    private NebulaPool pool;
    private Session session;
    private boolean closed;

    public NebulaClientFacade() {
        this(new NebulaPoolFactory());
    }

    NebulaClientFacade(NebulaPoolFactory poolFactory) {
        this.poolFactory = poolFactory;
    }

    public NebulaClientFacade(NebulaPool pool) {
        this.poolFactory = null;
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
        boolean poolCreated = pool != null;
        if (poolFactory != null) {
            pool = poolFactory.create();
            poolCreated = true;
        }
        try {
            if (!pool.init(config.getGraphAddresses(), poolConfig)) {
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
            releaseFailedOpen(openedSession, poolCreated);
            throw exception;
        } catch (UnknownHostException | InvalidConfigException | NotValidConnectionException |
                 IOErrorException | AuthFailedException | ClientServerIncompatibleException exception) {
            releaseFailedOpen(openedSession, poolCreated);
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
        if (pool != null) {
            pool.close();
        }
    }

    private void releaseFailedOpen(Session openedSession, boolean poolCreated) {
        if (openedSession != null) {
            openedSession.release();
        }
        if (poolCreated) {
            pool.close();
        }
    }

    public boolean isClosed() {
        return closed;
    }
}
