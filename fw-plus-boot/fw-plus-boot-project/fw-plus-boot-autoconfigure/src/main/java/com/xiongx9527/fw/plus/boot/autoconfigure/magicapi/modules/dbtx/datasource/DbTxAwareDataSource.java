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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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

	private boolean removing;

	private int activeLeaseCount;

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
			if (activeLeaseCount > 0) {
				throw new IOException("DataSource has active local transactions: " + key);
			}
			closed = true;
			List<Map.Entry<String, DataSource>> prepared =
					new ArrayList<Map.Entry<String, DataSource>>(preparedDataSources.entrySet());
			Exception failure = null;
			for (Map.Entry<String, DataSource> entry : prepared) {
				try {
					adapters.get(entry.getKey()).close(entry.getValue());
				} catch (Exception e) {
					failure = append(failure, e);
				}
			}
			try {
				closeRawDataSource();
			} catch (Exception e) {
				failure = append(failure, e);
			}
			if (failure != null) {
				throw new IOException("Failed to close DataSource: " + key, failure);
			}
		}
	}

	public void beforeRemove() throws Exception {
		synchronized (preparedDataSources) {
			if (activeLeaseCount > 0) {
				throw new IllegalStateException("DataSource has active local transactions: " + key);
			}
			Exception failure = null;
			for (Map.Entry<String, DataSource> entry : preparedDataSources.entrySet()) {
				try {
					adapters.get(entry.getKey()).beforeRemove(entry.getValue());
				} catch (Exception e) {
					failure = append(failure, e);
				}
			}
			if (failure != null) {
				throw failure;
			}
		}
	}

	public void startRemove() throws SQLException {
		synchronized (preparedDataSources) {
			ensureOpen();
			if (activeLeaseCount > 0) {
				throw new SQLException("DataSource has active local transactions: " + key);
			}
			removing = true;
		}
	}

	public Lease acquireLease() throws SQLException {
		synchronized (preparedDataSources) {
			ensureOpen();
			activeLeaseCount++;
			return new Lease(this);
		}
	}

	public void cancelRemove() {
		synchronized (preparedDataSources) {
			if (!closed) {
				removing = false;
			}
		}
	}

	private void releaseLease() {
		synchronized (preparedDataSources) {
			if (activeLeaseCount <= 0) {
				throw new IllegalStateException("DataSource lease underflow: " + key);
			}
			activeLeaseCount--;
		}
	}

	private static Exception append(Exception failure, Exception next) {
		if (failure == null) {
			return next;
		}
		failure.addSuppressed(next);
		return failure;
	}

	private void closeRawDataSource() throws Exception {
		for (DbTxDataSourceAdapter adapter : adapters.values()) {
			if (adapter.closeRawDataSource(rawDataSource)) {
				return;
			}
		}
		if (rawDataSource instanceof AutoCloseable) {
			((AutoCloseable) rawDataSource).close();
		}
	}

	private void ensureOpen() throws SQLException {
		if (closed) {
			throw new SQLException("DataSource is closed: " + key);
		}
		if (removing) {
			throw new SQLException("DataSource is being removed: " + key);
		}
	}

	public static final class Lease implements AutoCloseable {

		private DbTxAwareDataSource owner;

		private Lease(DbTxAwareDataSource owner) {
			this.owner = owner;
		}

		@Override
		public void close() {
			DbTxAwareDataSource current = owner;
			if (current != null) {
				owner = null;
				current.releaseLease();
			}
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
