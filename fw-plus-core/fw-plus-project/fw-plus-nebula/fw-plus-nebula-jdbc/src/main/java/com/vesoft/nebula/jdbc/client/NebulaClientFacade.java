package com.vesoft.nebula.jdbc.client;

import com.vesoft.nebula.jdbc.config.NebulaConnectionConfig;

import java.sql.SQLException;

public class NebulaClientFacade implements AutoCloseable {
    private final NebulaPoolFactory poolFactory;
    private NebulaNativeClient nativeClient;
    private boolean closed = true;

    public NebulaClientFacade() {
        this(new NebulaPoolFactory());
    }

    NebulaClientFacade(NebulaPoolFactory poolFactory) {
        this.poolFactory = poolFactory;
    }

    public NebulaClientFacade(NebulaNativeClient nativeClient) {
        this.poolFactory = null;
        this.nativeClient = nativeClient;
    }

    public void open(NebulaConnectionConfig config) throws SQLException {
        try {
            if (poolFactory != null) {
                nativeClient = poolFactory.create();
            }
            if (nativeClient == null) {
                throw new SQLException("Nebula native client is not configured.");
            }
            nativeClient.open(new NebulaConnectionConfigHolder(config));
            NebulaNativeResultSet useResult = nativeClient.execute("USE " + config.getGraphspace());
            if (!useResult.isSucceeded()) {
                throw new SQLException(String.format("Access graph space [%s] failed. Error code: %s, Error message: %s",
                        config.getGraphspace(), useResult.getErrorCode(), useResult.getErrorMessage()));
            }
            closed = false;
        } catch (SQLException exception) {
            releaseFailedOpen();
            throw exception;
        } catch (Exception exception) {
            releaseFailedOpen();
            throw new SQLException(exception.getMessage(), exception);
        }
    }

    public NebulaNativeResultSet executeNgql(String ngql) throws SQLException {
        checkOpen();
        return nativeClient.execute(ngql);
    }

    public Object getNativeClient() throws SQLException {
        checkOpen();
        return nativeClient.getNativeClient();
    }

    /**
     * @deprecated nebula-java 5.x no longer exposes Session. Use {@link #getNativeClient()}.
     */
    @Deprecated
    public Object getNativeSession() throws SQLException {
        checkOpen();
        return nativeClient.getNativeSession();
    }

    public Object getNativePool() throws SQLException {
        checkOpen();
        return nativeClient.getNativePool();
    }

    private void checkOpen() throws SQLException {
        if (closed) {
            throw new SQLException("Nebula client facade is closed.");
        }
        if (nativeClient == null || nativeClient.isClosed()) {
            throw new SQLException("Nebula client facade is not open.");
        }
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        if (nativeClient != null) {
            nativeClient.close();
            nativeClient = null;
        }
    }

    private void releaseFailedOpen() {
        try {
            if (nativeClient != null) {
                nativeClient.close();
            }
        } finally {
            nativeClient = null;
            closed = true;
        }
    }

    public boolean isClosed() {
        return closed;
    }
}
