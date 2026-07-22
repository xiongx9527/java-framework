package com.vesoft.nebula.jdbc.client;

import com.vesoft.nebula.jdbc.config.NebulaConnectionConfig;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

class NebulaJava3NativeClient implements NebulaNativeClient {
	private static final String POOL_CLASS = "com.vesoft.nebula.client.graph.net.NebulaPool";
	private static final String POOL_CONFIG_CLASS = "com.vesoft.nebula.client.graph.NebulaPoolConfig";
	private static final String HOST_ADDRESS_CLASS = "com.vesoft.nebula.client.graph.data.HostAddress";

	private Object pool;
	private Object session;
	private boolean closed = true;

	@Override
	public void open(NebulaConnectionConfigHolder configHolder) throws SQLException {
		NebulaConnectionConfig config = configHolder.getConfig();
		pool = ReflectiveNebulaSupport.newInstance(POOL_CLASS);
		Object poolConfig = ReflectiveNebulaSupport.newInstance(POOL_CONFIG_CLASS);
		setInt(poolConfig, "setMinConnSize", config.getMinConnsSize());
		setInt(poolConfig, "setMaxConnSize", config.getMaxConnsSize());
		setInt(poolConfig, "setTimeout", config.getTimeout());
		setInt(poolConfig, "setIdleTime", config.getIdleTime());
		setInt(poolConfig, "setIntervalIdle", config.getIntervalIdle());
		setInt(poolConfig, "setWaitTime", config.getWaitTime());
		Boolean initialized = (Boolean) ReflectiveNebulaSupport.invoke(pool, "init",
				new Class<?>[] {List.class, ReflectiveNebulaSupport.type(POOL_CONFIG_CLASS)},
				new Object[] {hostAddresses(config), poolConfig});
		if (!Boolean.TRUE.equals(initialized)) {
			throw new SQLException("Nebula Java 3.x pool init failed.");
		}
		session = ReflectiveNebulaSupport.invoke(pool, "getSession",
				new Class<?>[] {String.class, String.class, boolean.class},
				new Object[] {config.getUsername(), config.getPassword(), Boolean.TRUE});
		closed = false;
	}

	@Override
	public NebulaNativeResultSet execute(String ngql) throws SQLException {
		checkOpen();
		return new ReflectiveNebulaSupport.ResultSetAdapter(ReflectiveNebulaSupport.invoke(session, "execute",
				new Class<?>[] {String.class}, new Object[] {ngql}));
	}

	@Override
	public Object getNativeClient() throws SQLException {
		return getNativeSession();
	}

	@Override
	public Object getNativeSession() throws SQLException {
		checkOpen();
		return session;
	}

	@Override
	public Object getNativePool() throws SQLException {
		checkOpen();
		return pool;
	}

	@Override
	public boolean isClosed() {
		return closed;
	}

	@Override
	public void close() {
		if (closed) {
			return;
		}
		closed = true;
		try {
			if (session != null) {
				ReflectiveNebulaSupport.invoke(session, "release", new Class<?>[0], new Object[0]);
			}
		} catch (SQLException ignored) {
		}
		try {
			if (pool != null) {
				ReflectiveNebulaSupport.invoke(pool, "close", new Class<?>[0], new Object[0]);
			}
		} catch (SQLException ignored) {
		}
		session = null;
		pool = null;
	}

	private void checkOpen() throws SQLException {
		if (closed || session == null || pool == null) {
			throw new SQLException("Nebula Java 3.x client is not open.");
		}
	}

	private void setInt(Object target, String methodName, int value) throws SQLException {
		if (value >= 0) {
			ReflectiveNebulaSupport.invoke(target, methodName, new Class<?>[] {int.class}, new Object[] {Integer.valueOf(value)});
		}
	}

	private List<Object> hostAddresses(NebulaConnectionConfig config) throws SQLException {
		List<Object> addresses = new ArrayList<Object>();
		for (NebulaHostAddress address : config.getGraphAddresses()) {
			addresses.add(ReflectiveNebulaSupport.newInstance(HOST_ADDRESS_CLASS,
					new Class<?>[] {String.class, int.class},
					new Object[] {address.getHost(), Integer.valueOf(address.getPort())}));
		}
		return addresses;
	}
}
