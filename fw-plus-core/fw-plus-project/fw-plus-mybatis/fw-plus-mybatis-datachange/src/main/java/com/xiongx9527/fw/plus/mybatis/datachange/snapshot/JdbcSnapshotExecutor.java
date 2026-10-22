package com.xiongx9527.fw.plus.mybatis.datachange.snapshot;

import com.xiongx9527.fw.plus.mybatis.datachange.api.DataChangeErrorCode;
import com.xiongx9527.fw.plus.mybatis.datachange.api.DataChangeException;
import com.xiongx9527.fw.plus.mybatis.datachange.api.RowIdentity;
import com.xiongx9527.fw.plus.mybatis.datachange.dialect.SnapshotQuery;
import com.xiongx9527.fw.plus.mybatis.datachange.interceptor.DataChangeScope;
import org.apache.ibatis.logging.jdbc.ConnectionLogger;
import org.apache.ibatis.session.Configuration;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class JdbcSnapshotExecutor {
	private final SnapshotParameterBinder binder;

	public JdbcSnapshotExecutor() {
		this(new SnapshotParameterBinder());
	}

	JdbcSnapshotExecutor(SnapshotParameterBinder binder) {
		this.binder = binder;
	}

	public List<RowSnapshot> query(Connection connection, SnapshotQuery query, JdbcTableMetadata metadata,
			Configuration configuration, int maxRows) {
		if (connection == null || query == null || metadata == null || configuration == null) {
			throw new IllegalArgumentException("snapshot arguments must not be null");
		}
		if (maxRows <= 0) {
			throw new IllegalArgumentException("maxRows must be greater than zero");
		}
		validateKeyColumns(metadata.getKeyColumns());
		try (DataChangeScope ignored = DataChangeScope.suppress();
			 PreparedStatement statement = unlogged(connection).prepareStatement(query.getSql())) {
			statement.setMaxRows(maxRows == Integer.MAX_VALUE ? Integer.MAX_VALUE : maxRows + 1);
			binder.bind(statement, query.getParameters(), configuration);
			try (ResultSet rows = statement.executeQuery()) {
				return read(rows, metadata.getKeyColumns(), maxRows);
			}
		} catch (RuntimeException exception) {
			throw sanitize(exception);
		} catch (SQLException exception) {
			throw executionFailed();
		}
	}

	private Connection unlogged(Connection connection) {
		Connection current = connection;
		while (current != null && Proxy.isProxyClass(current.getClass())) {
			InvocationHandler handler = Proxy.getInvocationHandler(current);
			if (!(handler instanceof ConnectionLogger)) { break; }
			Connection unwrapped = ((ConnectionLogger) handler).getConnection();
			if (unwrapped == current) { break; }
			current = unwrapped;
		}
		return current;
	}

	private DataChangeException sanitize(RuntimeException exception) {
		if (exception instanceof DataChangeException) {
			DataChangeErrorCode code = ((DataChangeException) exception).getErrorCode();
			if (code == DataChangeErrorCode.SNAPSHOT_LIMIT_EXCEEDED
					|| code == DataChangeErrorCode.UNIQUE_KEY_UNAVAILABLE
						|| code == DataChangeErrorCode.INVALID_UNIQUE_KEY
						|| code == DataChangeErrorCode.ROW_IDENTITY_UNAVAILABLE) {
				return new DataChangeException(code, code.name());
			}
		}
		return executionFailed();
	}

	private DataChangeException executionFailed() {
		return new DataChangeException(DataChangeErrorCode.SNAPSHOT_QUERY_FAILED, "SNAPSHOT_QUERY_FAILED");
	}

	private List<RowSnapshot> read(ResultSet rows, List<String> keyColumns, int maxRows) throws SQLException {
		ResultSetMetaData resultMetadata = rows.getMetaData();
		Map<String, Column> columns = columns(resultMetadata);
		List<Column> keys = keyColumns(keyColumns, columns);
		List<RowSnapshot> snapshots = new ArrayList<RowSnapshot>();
		Set<RowIdentity> identities = new HashSet<RowIdentity>();
		while (rows.next()) {
			if (snapshots.size() == maxRows) {
				throw new DataChangeException(DataChangeErrorCode.SNAPSHOT_LIMIT_EXCEEDED,
						"SNAPSHOT_LIMIT_EXCEEDED");
			}
			Map<String, Object> values = new LinkedHashMap<String, Object>();
			for (Column column : columns.values()) {
				values.put(column.label, rows.getObject(column.index));
			}
			Map<String, Object> keyValues = new LinkedHashMap<String, Object>();
			for (Column key : keys) {
				Object value = rows.getObject(key.index);
				if (value == null) {
					throw invalidIdentity();
				}
				keyValues.put(key.keyName, value);
			}
			RowSnapshot snapshot = new RowSnapshot(keyValues, values);
			if (!identities.add(snapshot.getIdentity())) {
				throw invalidIdentity();
			}
			snapshots.add(snapshot);
		}
		return snapshots;
	}

	private Map<String, Column> columns(ResultSetMetaData metadata) throws SQLException {
		Map<String, Column> columns = new LinkedHashMap<String, Column>();
		for (int index = 1; index <= metadata.getColumnCount(); index++) {
			String label = metadata.getColumnLabel(index);
			String normalized = normalize(label);
			if (normalized == null || columns.containsKey(normalized)) {
				throw invalidIdentity();
			}
			columns.put(normalized, new Column(index, label));
		}
		return columns;
	}

	private List<Column> keyColumns(List<String> keyNames, Map<String, Column> columns) {
		List<Column> keys = new ArrayList<Column>();
		for (String keyName : keyNames) {
			Column selected = columns.get(normalize(keyName));
			if (selected == null) {
				throw invalidIdentity();
			}
			keys.add(selected.asKey(keyName));
		}
		return keys;
	}

	private void validateKeyColumns(List<String> keyColumns) {
		if (keyColumns == null || keyColumns.isEmpty()) {
			throw new DataChangeException(DataChangeErrorCode.UNIQUE_KEY_UNAVAILABLE,
					"UNIQUE_KEY_UNAVAILABLE");
		}
		Set<String> seen = new HashSet<String>();
		for (String key : keyColumns) {
			String normalized = normalize(key);
			if (normalized == null || !seen.add(normalized)) {
				throw invalidIdentity();
			}
		}
	}

	private String normalize(String value) {
		if (value == null || value.trim().isEmpty()) { return null; }
		return value.trim().toLowerCase(Locale.ENGLISH);
	}

	private DataChangeException invalidIdentity() {
		return new DataChangeException(DataChangeErrorCode.INVALID_UNIQUE_KEY, "INVALID_UNIQUE_KEY");
	}

	private static final class Column {
		private final int index;
		private final String label;
		private final String keyName;

		private Column(int index, String label) {
			this(index, label, null);
		}

		private Column(int index, String label, String keyName) {
			this.index = index;
			this.label = label;
			this.keyName = keyName;
		}

		private Column asKey(String name) {
			return new Column(index, label, name);
		}
	}
}
