package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.datasource;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.model.DbTxContext;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.model.DbTxContextHolder;

import javax.sql.DataSource;
import java.io.Closeable;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

public class DbTxAwareDataSource implements DataSource, Closeable {

	private static final String LOCAL_COORDINATOR = "local";

	private final String key;

	private final DataSource rawDataSource;

	private final Map<String, DbTxDataSourceAdapter> adapters;

	private final Map<String, DataSource> preparedDataSources = new LinkedHashMap<>();

	private volatile boolean closed;

	public DbTxAwareDataSource(String key, DataSource rawDataSource,
			Collection<? extends DbTxDataSourceAdapter> adapters) {
		if (key == null || key.trim().isEmpty()) {
			throw new IllegalArgumentException("DataSource key must not be blank");
		}
		if (rawDataSource == null) {
			throw new IllegalArgumentException("Raw DataSource must not be null");
		}
		this.key = key;
		this.rawDataSource = rawDataSource;
		this.adapters = adapters(adapters);
	}

	public DataSource prepare(String coordinator) throws SQLException {
		String normalizedCoordinator = normalizeCoordinator(coordinator);
		if (LOCAL_COORDINATOR.equals(normalizedCoordinator)) {
			return rawDataSource;
		}
		synchronized (preparedDataSources) {
			ensureOpen();
			DataSource prepared = preparedDataSources.get(normalizedCoordinator);
			if (prepared != null) {
				return prepared;
			}
			DbTxDataSourceAdapter adapter = adapters.get(normalizedCoordinator);
			if (adapter == null) {
				throw new SQLException("No DataSource adapter for coordinator: " + coordinator);
			}
			prepared = adapter.prepare(key, rawDataSource);
			if (prepared == null) {
				throw new SQLException("DataSource adapter returned null: " + coordinator);
			}
			preparedDataSources.put(normalizedCoordinator, prepared);
			return prepared;
		}
	}

	@Override
	public Connection getConnection() throws SQLException {
		return currentDataSource().getConnection();
	}

	@Override
	public Connection getConnection(String username, String password) throws SQLException {
		return currentDataSource().getConnection(username, password);
	}

	private DataSource currentDataSource() throws SQLException {
		DbTxContext context = DbTxContextHolder.current();
		if (context == null) {
			ensureOpen();
			return rawDataSource;
		}
		if (!context.containsDataSource(key)) {
			throw new SQLException("DataSource is not declared in the active dbtx: " + key);
		}
		String coordinator = normalizeCoordinator(context.getCoordinator());
		return LOCAL_COORDINATOR.equals(coordinator) ? rawDataSource : prepare(coordinator);
	}

	@Override
	public void close() throws IOException {
		synchronized (preparedDataSources) {
			if (closed) {
				return;
			}
			try {
				for (Map.Entry<String, DataSource> entry : preparedDataSources.entrySet()) {
					if (entry.getValue() != rawDataSource) {
						adapters.get(entry.getKey()).close(entry.getValue());
					}
				}
				closeRawDataSource();
				closed = true;
			} catch (Exception e) {
				if (e instanceof IOException) {
					throw (IOException) e;
				}
				throw new IOException("Failed to close DataSource: " + key, e);
			}
		}
	}

	private void closeRawDataSource() throws Exception {
		if (rawDataSource instanceof AutoCloseable) {
			((AutoCloseable) rawDataSource).close();
		}
	}

	private void ensureOpen() throws SQLException {
		if (closed) {
			throw new SQLException("DataSource is closed: " + key);
		}
	}

	private static Map<String, DbTxDataSourceAdapter> adapters(
			Collection<? extends DbTxDataSourceAdapter> adapters) {
		if (adapters == null || adapters.isEmpty()) {
			return Collections.emptyMap();
		}
		Map<String, DbTxDataSourceAdapter> registered = new LinkedHashMap<>();
		for (DbTxDataSourceAdapter adapter : adapters) {
			if (adapter == null) {
				throw new IllegalArgumentException("DataSource adapter must not be null");
			}
			String coordinator = normalizeCoordinator(adapter.getCoordinator());
			if (LOCAL_COORDINATOR.equals(coordinator)) {
				throw new IllegalArgumentException("The local coordinator cannot have a DataSource adapter");
			}
			if (registered.put(coordinator, adapter) != null) {
				throw new IllegalArgumentException("Duplicate DataSource adapter: " + coordinator);
			}
		}
		return Collections.unmodifiableMap(registered);
	}

	private static String normalizeCoordinator(String coordinator) {
		if (coordinator == null || coordinator.trim().isEmpty()) {
			return "";
		}
		return coordinator.trim().toLowerCase(Locale.ENGLISH);
	}

	@Override
	public PrintWriter getLogWriter() throws SQLException {
		return rawDataSource.getLogWriter();
	}

	@Override
	public void setLogWriter(PrintWriter out) throws SQLException {
		rawDataSource.setLogWriter(out);
	}

	@Override
	public void setLoginTimeout(int seconds) throws SQLException {
		rawDataSource.setLoginTimeout(seconds);
	}

	@Override
	public int getLoginTimeout() throws SQLException {
		return rawDataSource.getLoginTimeout();
	}

	@Override
	public Logger getParentLogger() throws SQLFeatureNotSupportedException {
		return rawDataSource.getParentLogger();
	}

	@Override
	public <T> T unwrap(Class<T> iface) throws SQLException {
		if (iface.isInstance(this)) {
			return iface.cast(this);
		}
		return rawDataSource.unwrap(iface);
	}

	@Override
	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		return iface.isInstance(this) || rawDataSource.isWrapperFor(iface);
	}
}
