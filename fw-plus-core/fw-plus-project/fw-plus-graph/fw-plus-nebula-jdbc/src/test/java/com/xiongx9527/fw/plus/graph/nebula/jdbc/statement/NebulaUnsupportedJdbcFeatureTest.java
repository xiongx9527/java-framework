package com.xiongx9527.fw.plus.graph.nebula.jdbc.statement;

import com.xiongx9527.fw.plus.graph.nebula.jdbc.NebulaConnection;
import com.xiongx9527.fw.plus.graph.nebula.jdbc.NebulaDatabaseMetaData;
import com.xiongx9527.fw.plus.graph.nebula.jdbc.lifecycle.NebulaResourceLifecycle;
import com.xiongx9527.fw.plus.graph.nebula.jdbc.utils.ExceptionBuilder;
import org.junit.Test;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Savepoint;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;

public class NebulaUnsupportedJdbcFeatureTest {

	@Test
	public void unsupportedExceptionShouldUseCommonJdbcMessage() {
		SQLException exception = ExceptionBuilder.buildUnsupportedOperationException();

		assertTrue(exception instanceof SQLFeatureNotSupportedException);
		assertEquals("Unsupported JDBC feature", exception.getMessage());
	}

	@Test
	public void unsupportedConnectionFeaturesShouldThrowSqlFeatureNotSupportedException() throws Exception {
		NebulaConnection connection = realMethodConnection();

		assertTrue(connection.getAutoCommit());
		assertEquals(ResultSet.CLOSE_CURSORS_AT_COMMIT, connection.getHoldability());
		connection.setHoldability(ResultSet.CLOSE_CURSORS_AT_COMMIT);
		assertThrows(SQLFeatureNotSupportedException.class, new SqlRunnable() {
			@Override
			public void run() throws SQLException {
				connection.prepareCall("CALL p()");
			}
		});
		assertThrows(SQLFeatureNotSupportedException.class, new SqlRunnable() {
			@Override
			public void run() throws SQLException {
				connection.prepareStatement("INSERT VERTEX player(name) VALUES 1:(\"Tim\")", Statement.RETURN_GENERATED_KEYS);
			}
		});
		assertThrows(SQLFeatureNotSupportedException.class, new SqlRunnable() {
			@Override
			public void run() throws SQLException {
				connection.setAutoCommit(false);
			}
		});
		assertThrows(SQLFeatureNotSupportedException.class, new SqlRunnable() {
			@Override
			public void run() throws SQLException {
				connection.commit();
			}
		});
		assertThrows(SQLFeatureNotSupportedException.class, new SqlRunnable() {
			@Override
			public void run() throws SQLException {
				connection.rollback();
			}
		});
		assertThrows(SQLFeatureNotSupportedException.class, new SqlRunnable() {
			@Override
			public void run() throws SQLException {
				connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
			}
		});
		assertThrows(SQLFeatureNotSupportedException.class, new SqlRunnable() {
			@Override
			public void run() throws SQLException {
				connection.rollback(mock(Savepoint.class));
			}
		});
		assertThrows(SQLFeatureNotSupportedException.class, new SqlRunnable() {
			@Override
			public void run() throws SQLException {
				connection.setHoldability(ResultSet.HOLD_CURSORS_OVER_COMMIT);
			}
		});
	}

	@Test
	public void unsupportedStatementFeaturesShouldExposeExplicitSemantics() throws Exception {
		final Statement statement = new NebulaStatementImpl(mock(NebulaConnection.class));

		statement.addBatch("INSERT VERTEX player(name) VALUES 1:(\"Tim\")");
		statement.clearBatch();
		assertEquals(0, statement.executeBatch().length);
		assertThrows(SQLFeatureNotSupportedException.class, new SqlRunnable() {
			@Override
			public void run() throws SQLException {
				statement.getGeneratedKeys();
			}
		});
		assertFalse(statement.getMoreResults());
		assertEquals(-1, statement.getUpdateCount());
	}

	@Test
	public void preparedStatementSqlOverloadsShouldKeepJdbcPreparedStatementContract() throws Exception {
		final PreparedStatement statement = new NebulaPreparedStatementImpl(
				mock(NebulaConnection.class),
				"SELECT name FROM player WHERE id = ?"
		);

		assertCannotPassSqlToPreparedStatement(new SqlRunnable() {
			@Override
			public void run() throws SQLException {
				statement.executeQuery("SELECT name FROM player");
			}
		});
		assertCannotPassSqlToPreparedStatement(new SqlRunnable() {
			@Override
			public void run() throws SQLException {
				statement.executeUpdate("UPDATE player SET age = 19");
			}
		});
		assertCannotPassSqlToPreparedStatement(new SqlRunnable() {
			@Override
			public void run() throws SQLException {
				statement.execute("SELECT name FROM player", Statement.RETURN_GENERATED_KEYS);
			}
		});
	}

	@Test
	public void unsupportedMetadataCapabilitiesShouldReturnFalseForFrameworkProbes() throws Exception {
		DatabaseMetaData metaData = new NebulaDatabaseMetaData(mock(NebulaConnection.class));

		assertTrue(metaData.supportsBatchUpdates());
		assertFalse(metaData.supportsGetGeneratedKeys());
		assertFalse(metaData.supportsMultipleOpenResults());
		assertFalse(metaData.supportsMultipleResultSets());
		assertFalse(metaData.supportsTransactions());
		assertFalse(metaData.supportsTransactionIsolationLevel(Connection.TRANSACTION_READ_COMMITTED));
	}

	private static <T extends SQLException> T assertThrows(Class<T> type, SqlRunnable runnable) throws Exception {
		try {
			runnable.run();
			fail("Expected " + type.getName());
		} catch (SQLException exception) {
			assertTrue(type.isInstance(exception));
			return type.cast(exception);
		}
		return null;
	}

	private static void assertCannotPassSqlToPreparedStatement(SqlRunnable runnable) throws Exception {
		try {
			runnable.run();
			fail("Expected SQLException");
		} catch (SQLException exception) {
			assertFalse(exception instanceof SQLFeatureNotSupportedException);
			assertTrue(exception.getMessage().contains("cannot be called on PreparedStatement"));
		}
	}

	private static NebulaConnection realMethodConnection() throws Exception {
		NebulaConnection connection = mock(NebulaConnection.class, CALLS_REAL_METHODS);
		Field lifecycle = NebulaConnection.class.getDeclaredField("lifecycle");
		lifecycle.setAccessible(true);
		lifecycle.set(connection, new NebulaResourceLifecycle());
		return connection;
	}

	private interface SqlRunnable {
		void run() throws SQLException;
	}
}
