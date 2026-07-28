package com.xiongx9527.fw.plus.graph.nebula.jdbc.client;

import com.xiongx9527.fw.plus.graph.nebula.jdbc.config.NebulaConnectionConfig;

import java.sql.SQLException;

class NebulaJava5NativeClient implements NebulaNativeClient {
	private static final String POOL_CLASS = "com.vesoft.nebula.driver.graph.net.NebulaPool";

	private Object pool;
	private Object client;
	private boolean closed = true;

	@Override
	public void open(NebulaConnectionConfigHolder configHolder) throws SQLException {
		NebulaConnectionConfig config = configHolder.getConfig();
		Object builder = ReflectiveNebulaSupport.invokeStatic(POOL_CLASS, "builder",
				new Class<?>[] {String.class, String.class, String.class},
				new Object[] {graphAddress(config), config.getUsername(), config.getPassword()});
		setInt(builder, "withMinClientSize", config.getMinConnsSize());
		setInt(builder, "withMaxClientSize", config.getMaxConnsSize());
		if (config.getTimeout() > 0) {
			setLong(builder, "withConnectTimeoutMills", config.getTimeout());
			setLong(builder, "withRequestTimeoutMills", config.getTimeout());
		}
		if (config.getWaitTime() > 0) {
			setLong(builder, "withMaxWaitMills", config.getWaitTime());
			ReflectiveNebulaSupport.invoke(builder, "withBlockWhenExhausted",
					new Class<?>[] {boolean.class}, new Object[] {Boolean.TRUE});
		}
		if (config.getIdleTime() > 0) {
			setLong(builder, "withMinEvictableIdleTimeMillis", config.getIdleTime());
		}
		if (config.getIntervalIdle() > 0) {
			setLong(builder, "withIdleEvictScheduleMills", config.getIntervalIdle());
		}
		pool = ReflectiveNebulaSupport.invoke(builder, "build", new Class<?>[0], new Object[0]);
		client = ReflectiveNebulaSupport.invoke(pool, "getClient", new Class<?>[0], new Object[0]);
		closed = false;
	}

	@Override
	public NebulaNativeResultSet execute(String ngql) throws SQLException {
		checkOpen();
		return new ReflectiveNebulaSupport.ResultSetAdapter(ReflectiveNebulaSupport.invoke(client, "execute",
				new Class<?>[] {String.class}, new Object[] {ngql}));
	}

	@Override
	public Object getNativeClient() throws SQLException {
		checkOpen();
		return client;
	}

	@Override
	public Object getNativeSession() throws SQLException {
		return getNativeClient();
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
			if (client != null && pool != null) {
				ReflectiveNebulaSupport.invoke(pool, "returnClient",
						new Class<?>[] {client.getClass()}, new Object[] {client});
			}
		} catch (SQLException ignored) {
		}
		try {
			if (pool != null) {
				ReflectiveNebulaSupport.invoke(pool, "close", new Class<?>[0], new Object[0]);
			}
		} catch (SQLException ignored) {
		}
		client = null;
		pool = null;
	}

	private void checkOpen() throws SQLException {
		if (closed || client == null || pool == null) {
			throw new SQLException("Nebula Java 5.x client is not open.");
		}
	}

	private void setInt(Object target, String methodName, int value) throws SQLException {
		if (value >= 0) {
			ReflectiveNebulaSupport.invoke(target, methodName, new Class<?>[] {int.class}, new Object[] {Integer.valueOf(value)});
		}
	}

	private void setLong(Object target, String methodName, long value) throws SQLException {
		ReflectiveNebulaSupport.invoke(target, methodName, new Class<?>[] {long.class}, new Object[] {Long.valueOf(value)});
	}

	private String graphAddress(NebulaConnectionConfig config) {
		String graphAddress = config.getGraphAddress();
		if (graphAddress != null && graphAddress.startsWith("//")) {
			return graphAddress.substring(2);
		}
		return graphAddress;
	}
}
