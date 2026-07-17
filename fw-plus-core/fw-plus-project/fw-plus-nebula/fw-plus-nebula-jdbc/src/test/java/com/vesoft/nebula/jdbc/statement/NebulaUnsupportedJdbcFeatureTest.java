package com.vesoft.nebula.jdbc.statement;

import com.vesoft.nebula.jdbc.NebulaConnection;
import com.vesoft.nebula.jdbc.NebulaDatabaseMetaData;
import org.junit.Test;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;

public class NebulaUnsupportedJdbcFeatureTest {

	@Test
	public void unsupportedConnectionFeaturesShouldThrowSqlFeatureNotSupportedException() throws Exception {
		NebulaConnection connection = mock(NebulaConnection.class, CALLS_REAL_METHODS);

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
				connection.commit();
			}
		});
		assertThrows(SQLFeatureNotSupportedException.class, new SqlRunnable() {
			@Override
			public void run() throws SQLException {
				connection.rollback();
			}
		});
	}

	@Test
	public void unsupportedStatementFeaturesShouldThrowSqlFeatureNotSupportedException() throws Exception {
		final Statement statement = new NebulaStatementImpl(mock(NebulaConnection.class));

		assertThrows(SQLFeatureNotSupportedException.class, new SqlRunnable() {
			@Override
			public void run() throws SQLException {
				statement.addBatch("INSERT VERTEX player(name) VALUES 1:(\"Tim\")");
			}
		});
		assertThrows(SQLFeatureNotSupportedException.class, new SqlRunnable() {
			@Override
			public void run() throws SQLException {
				statement.getGeneratedKeys();
			}
		});
		assertThrows(SQLFeatureNotSupportedException.class, new SqlRunnable() {
			@Override
			public void run() throws SQLException {
				statement.getMoreResults();
			}
		});
	}

	@Test
	public void unsupportedPreparedStatementSqlOverloadsShouldThrowSqlFeatureNotSupportedException() throws Exception {
		final PreparedStatement statement = new NebulaPreparedStatementImpl(
				mock(NebulaConnection.class),
				"SELECT name FROM player WHERE id = ?"
		);

		assertThrows(SQLFeatureNotSupportedException.class, new SqlRunnable() {
			@Override
			public void run() throws SQLException {
				statement.executeQuery("SELECT name FROM player");
			}
		});
		assertThrows(SQLFeatureNotSupportedException.class, new SqlRunnable() {
			@Override
			public void run() throws SQLException {
				statement.executeUpdate("UPDATE player SET age = 19");
			}
		});
		assertThrows(SQLFeatureNotSupportedException.class, new SqlRunnable() {
			@Override
			public void run() throws SQLException {
				statement.execute("SELECT name FROM player", Statement.RETURN_GENERATED_KEYS);
			}
		});
	}

	@Test
	public void unsupportedMetadataCapabilitiesShouldReturnFalseForFrameworkProbes() throws Exception {
		DatabaseMetaData metaData = new NebulaDatabaseMetaData(mock(NebulaConnection.class));

		assertFalse(metaData.supportsBatchUpdates());
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

	private interface SqlRunnable {
		void run() throws SQLException;
	}
}
