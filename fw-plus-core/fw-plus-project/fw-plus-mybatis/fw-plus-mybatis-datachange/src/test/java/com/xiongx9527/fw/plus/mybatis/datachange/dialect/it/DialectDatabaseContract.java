package com.xiongx9527.fw.plus.mybatis.datachange.dialect.it;

import com.xiongx9527.fw.plus.mybatis.datachange.api.TableChange;
import com.xiongx9527.fw.plus.mybatis.datachange.api.TableIdentifier;
import com.xiongx9527.fw.plus.mybatis.datachange.config.TableMonitor;
import com.xiongx9527.fw.plus.mybatis.datachange.dialect.DataChangeDialect;
import com.xiongx9527.fw.plus.mybatis.datachange.dialect.DataChangeDialectRegistry;
import com.xiongx9527.fw.plus.mybatis.datachange.diff.ChangeDiffEngine;
import com.xiongx9527.fw.plus.mybatis.datachange.snapshot.RowSnapshot;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

abstract class DialectDatabaseContract {
	private final String environmentPrefix;
	private final DataChangeDialect dialect;
	private String jdbcUrl;
	private String username;
	private String password;

	DialectDatabaseContract(String environmentPrefix, DataChangeDialect dialect) {
		this.environmentPrefix = environmentPrefix;
		this.dialect = dialect;
	}

	final void runContract() throws Exception {
		loadEnvironment();
		String suffix = Long.toHexString(System.nanoTime());
		String schema = supportsCreatedSchema() ? "dc_" + suffix : null;
		String table = "dc_event_" + suffix;
		try (Connection connection = open()) {
			assertThat(new DataChangeDialectRegistry(Collections.singletonList(dialect)).select(connection))
					.isSameAs(dialect);
			createObjects(connection, schema, table);
			verifyDmlContract(connection, schema, table);
		} finally {
			cleanup(schema, table);
		}
	}

	protected abstract boolean supportsCreatedSchema();
	protected abstract String createTableSql(String qualifiedTable);
	protected abstract List<String> createTriggerSql(String schema, String table, String qualifiedTable);
	protected abstract String upsertSql(String qualifiedTable);
	protected abstract String mergeSql(String qualifiedTable);
	protected abstract String qualified(String schema, String table);

	protected String createSchemaSql(String schema) { return "CREATE SCHEMA " + schema; }
	protected List<String> dropExtraSql(String schema, String table, String qualifiedTable) {
		return Collections.emptyList();
	}

	protected int[] acceptedUpsertCounts() { return new int[]{1}; }

	protected void verifyReturning(Connection connection, String qualifiedTable) throws Exception { }

	private void createObjects(Connection connection, String schema, String table) throws Exception {
		try (Statement statement = connection.createStatement()) {
			if (schema != null) { statement.execute(createSchemaSql(schema)); }
			String qualifiedTable = qualified(schema, table);
			statement.execute(createTableSql(qualifiedTable));
			for (String sql : createTriggerSql(schema, table, qualifiedTable)) { statement.execute(sql); }
		}
	}

	private void verifyDmlContract(Connection connection, String schema, String table) throws Exception {
		String qualifiedTable = qualified(schema, table);
		TableMonitor monitor = monitor(table);
		TableIdentifier identifier = new TableIdentifier(null, schema, table);

		List<RowSnapshot> beforeInsert = snapshots(connection, qualifiedTable);
		long generatedKey = insertAndGetGeneratedKey(connection, qualifiedTable);
		List<RowSnapshot> afterInsert = snapshots(connection, qualifiedTable);
		assertEventRows(identifier, monitor, beforeInsert, afterInsert, 1);
		assertThat(value(afterInsert, "status")).isEqualTo("NEW");
		verifyReturning(connection, qualifiedTable);

		List<RowSnapshot> beforeUpdate = snapshots(connection, qualifiedTable);
		assertThat(update(connection, "UPDATE " + qualifiedTable
				+ " SET amount=? WHERE tenant_id=? AND order_no=?", 20, 7L, "A")).isEqualTo(1);
		List<RowSnapshot> afterUpdate = snapshots(connection, qualifiedTable);
		assertEventRows(identifier, monitor, beforeUpdate, afterUpdate, 1);
		assertThat(((Number) value(afterUpdate, "touched")).longValue()).isGreaterThan(0L);

		List<RowSnapshot> beforeUpsert = snapshots(connection, qualifiedTable);
		int upsertCount = update(connection, upsertSql(qualifiedTable), generatedKey, 7L, "A", 30);
		assertThat(upsertCount).isIn(boxed(acceptedUpsertCounts()));
		List<RowSnapshot> afterUpsert = snapshots(connection, qualifiedTable);
		assertEventRows(identifier, monitor, beforeUpsert, afterUpsert, 1);

		List<RowSnapshot> beforeMerge = snapshots(connection, qualifiedTable);
		assertThat(update(connection, mergeSql(qualifiedTable), 40, 7L, "A")).isEqualTo(1);
		List<RowSnapshot> afterMerge = snapshots(connection, qualifiedTable);
		assertEventRows(identifier, monitor, beforeMerge, afterMerge, 1);

		connection.setAutoCommit(false);
		assertThat(update(connection, "INSERT INTO " + qualifiedTable
				+ " (tenant_id, order_no, amount) VALUES (?, ?, ?)", 9L, "ROLLBACK", 90)).isEqualTo(1);
		assertThat(snapshots(connection, qualifiedTable)).hasSizeGreaterThan(afterMerge.size());
		connection.rollback();
		connection.setAutoCommit(true);
		assertThat(snapshots(connection, qualifiedTable)).hasSameSizeAs(afterMerge);

		List<RowSnapshot> concurrentBefore = snapshots(connection, qualifiedTable);
		try (Connection concurrent = open()) {
			assertThat(update(concurrent, "UPDATE " + qualifiedTable
					+ " SET amount=? WHERE tenant_id=? AND order_no=?", 50, 7L, "A")).isEqualTo(1);
		}
		List<RowSnapshot> concurrentAfter = snapshots(connection, qualifiedTable);
		assertEventRows(identifier, monitor, concurrentBefore, concurrentAfter, 1);

		List<RowSnapshot> beforeDelete = snapshots(connection, qualifiedTable);
		assertThat(update(connection, "DELETE FROM " + qualifiedTable
				+ " WHERE tenant_id=? AND order_no=?", 7L, "A")).isEqualTo(1);
		assertEventRows(identifier, monitor, beforeDelete, snapshots(connection, qualifiedTable), 1);
	}

	private int update(Connection connection, String sql, Object... parameters) throws Exception {
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			for (int index = 0; index < parameters.length; index++) {
				statement.setObject(index + 1, parameters[index]);
			}
			return statement.executeUpdate();
		}
	}

	protected long insertAndGetGeneratedKey(Connection connection, String qualifiedTable) throws Exception {
		try (PreparedStatement insert = connection.prepareStatement(
				"INSERT INTO " + qualifiedTable + " (tenant_id, order_no, amount) VALUES (?, ?, ?)",
				Statement.RETURN_GENERATED_KEYS)) {
			insert.setLong(1, 7L);
			insert.setString(2, "A");
			insert.setInt(3, 10);
			assertThat(insert.executeUpdate()).isEqualTo(1);
			try (ResultSet keys = insert.getGeneratedKeys()) {
				assertThat(keys.next()).isTrue();
				long generatedKey = keys.getLong(1);
				assertThat(generatedKey).isPositive();
				return generatedKey;
			}
		}
	}

	private List<RowSnapshot> snapshots(Connection connection, String qualifiedTable) throws Exception {
		String sql = "SELECT tenant_id, order_no, id, amount, status, touched FROM " + qualifiedTable
				+ " ORDER BY tenant_id, order_no";
		try (PreparedStatement statement = connection.prepareStatement(sql);
			 ResultSet rows = statement.executeQuery()) {
			List<RowSnapshot> snapshots = new ArrayList<RowSnapshot>();
			ResultSetMetaData metadata = rows.getMetaData();
			while (rows.next()) {
				Map<String, Object> values = new LinkedHashMap<String, Object>();
				for (int index = 1; index <= metadata.getColumnCount(); index++) {
					values.put(metadata.getColumnLabel(index).toLowerCase(Locale.ENGLISH), rows.getObject(index));
				}
				Map<String, Object> keys = new LinkedHashMap<String, Object>();
				keys.put("tenant_id", values.get("tenant_id"));
				keys.put("order_no", values.get("order_no"));
				snapshots.add(new RowSnapshot(keys, values));
			}
			return snapshots;
		}
	}

	private void assertEventRows(TableIdentifier table, TableMonitor monitor, List<RowSnapshot> before,
			List<RowSnapshot> after, int expected) {
		TableChange event = new ChangeDiffEngine().diff(table, monitor,
				Collections.<String, String>emptyMap(), before, after);
		assertThat(event.getRows()).hasSize(expected);
	}

	private Object value(List<RowSnapshot> rows, String column) {
		assertThat(rows).isNotEmpty();
		return rows.get(0).getValues().get(column);
	}

	private TableMonitor monitor(String table) {
		TableMonitor monitor = new TableMonitor();
		monitor.setName(table);
		monitor.setKeyColumns(Arrays.asList("tenant_id", "order_no"));
		monitor.setIncludeColumns(Arrays.asList("id", "amount", "status", "touched"));
		return monitor;
	}

	private Integer[] boxed(int[] values) {
		Integer[] boxed = new Integer[values.length];
		for (int index = 0; index < values.length; index++) { boxed[index] = Integer.valueOf(values[index]); }
		return boxed;
	}

	private void loadEnvironment() {
		jdbcUrl = required(environmentPrefix + "_JDBC_URL");
		username = required(environmentPrefix + "_USERNAME");
		password = required(environmentPrefix + "_PASSWORD");
	}

	private String required(String name) {
		String value = System.getenv(name);
		if (value == null) { throw new IllegalStateException("Missing required environment variable: " + name); }
		return value;
	}

	private Connection open() throws Exception {
		return DriverManager.getConnection(jdbcUrl, username, password);
	}

	private void cleanup(String schema, String table) {
		if (jdbcUrl == null) { return; }
		try (Connection connection = open(); Statement statement = connection.createStatement()) {
			String qualifiedTable = qualified(schema, table);
			for (String sql : dropExtraSql(schema, table, qualifiedTable)) {
				try { statement.execute(sql); } catch (Exception ignored) { }
			}
			try { statement.execute("DROP TABLE " + qualifiedTable); } catch (Exception ignored) { }
			if (schema != null) {
				try { statement.execute("DROP SCHEMA " + schema); } catch (Exception ignored) { }
			}
		} catch (Exception ignored) {
			// Cleanup cannot replace the contract failure and only targets generated object names.
		}
	}
}
