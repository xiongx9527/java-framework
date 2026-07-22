package com.vesoft.nebula.jdbc;

import org.junit.Assume;
import org.junit.Test;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class NebulaLiveJdbcSmokeTest {

	@Test
	public void shouldConnectToLiveNebulaAndExecuteYieldOne() throws Exception {
		Assume.assumeTrue(Boolean.getBoolean("nebula.jdbc.live"));

		String host = System.getProperty("nebula.jdbc.live.host", "127.0.0.1");
		String port = System.getProperty("nebula.jdbc.live.port", "9669");
		String space = System.getProperty("nebula.jdbc.live.space");
		Assume.assumeTrue("Set -Dnebula.jdbc.live.space=<space> to run live JDBC smoke test.",
				space != null && space.trim().length() > 0);
		Class.forName(NebulaDriver.class.getName());

		Properties properties = new Properties();
		properties.setProperty("user", System.getProperty("nebula.jdbc.live.user", "root"));
		properties.setProperty("password", System.getProperty("nebula.jdbc.live.password", "nebula"));
		properties.setProperty("timeout", System.getProperty("nebula.jdbc.live.timeout", "3000"));

		try (Connection connection = DriverManager.getConnection(
				"jdbc:nebula://" + host + ":" + port + "/" + space, properties);
			 Statement statement = connection.createStatement();
			 ResultSet resultSet = statement.executeQuery("YIELD 1 AS value")) {

			assertTrue(resultSet.next());
			assertEquals(1, resultSet.getInt("value"));
		}
	}

	@Test
	public void shouldReadLiveNebulaMetadataForIdentityAndIndexes() throws Exception {
		Assume.assumeTrue(Boolean.getBoolean("nebula.jdbc.live"));
		Assume.assumeTrue(Boolean.getBoolean("nebula.jdbc.live.metadata"));

		String host = System.getProperty("nebula.jdbc.live.host", "127.0.0.1");
		String port = System.getProperty("nebula.jdbc.live.port", "9669");
		String space = System.getProperty("nebula.jdbc.live.space");
		Assume.assumeTrue("Set -Dnebula.jdbc.live.space=<space> to run live JDBC smoke test.",
				space != null && space.trim().length() > 0);
		Class.forName(NebulaDriver.class.getName());

		Properties properties = new Properties();
		properties.setProperty("user", System.getProperty("nebula.jdbc.live.user", "root"));
		properties.setProperty("password", System.getProperty("nebula.jdbc.live.password", "nebula"));
		properties.setProperty("timeout", System.getProperty("nebula.jdbc.live.timeout", "3000"));

		String suffix = String.valueOf(System.currentTimeMillis());
		String tagName = "jdbc_meta_tag_" + suffix;
		String edgeName = "jdbc_meta_edge_" + suffix;
		String tagIndexName = "jdbc_meta_tag_idx_" + suffix;
		String edgeIndexName = "jdbc_meta_edge_idx_" + suffix;

		try (Connection connection = DriverManager.getConnection(
				"jdbc:nebula://" + host + ":" + port + "/" + space, properties)) {
			NebulaConnection nebulaConnection = connection.unwrap(NebulaConnection.class);
			try {
				nebulaConnection.execute("CREATE TAG " + tagName + "(name string, age int64)");
				nebulaConnection.execute("CREATE EDGE " + edgeName + "(since int64)");
				nebulaConnection.execute("CREATE TAG INDEX " + tagIndexName + " ON " + tagName + "(name(16))");
				nebulaConnection.execute("CREATE EDGE INDEX " + edgeIndexName + " ON " + edgeName + "(since)");

				DatabaseMetaData metaData = connection.getMetaData();
				ResultSet tagKeys = metaData.getPrimaryKeys(null, null, tagName);
				assertTrue(tagKeys.next());
				assertEquals("__vid", tagKeys.getString("COLUMN_NAME"));

				ResultSet edgeKeys = metaData.getPrimaryKeys(null, null, edgeName);
				assertTrue(edgeKeys.next());
				assertEquals("__src", edgeKeys.getString("COLUMN_NAME"));

				ResultSet tagIndexes = metaData.getIndexInfo(null, null, tagName, false, false);
				assertTrue(tagIndexes.next());
				assertEquals(tagIndexName, tagIndexes.getString("INDEX_NAME"));
				assertEquals("name", tagIndexes.getString("COLUMN_NAME"));
				assertTrue(tagIndexes.getBoolean("NON_UNIQUE"));

				ResultSet uniqueTagIndexes = metaData.getIndexInfo(null, null, tagName, true, false);
				assertTrue(!uniqueTagIndexes.next());
			} finally {
				dropQuietly(nebulaConnection, "DROP TAG INDEX " + tagIndexName);
				dropQuietly(nebulaConnection, "DROP EDGE INDEX " + edgeIndexName);
				dropQuietly(nebulaConnection, "DROP TAG " + tagName);
				dropQuietly(nebulaConnection, "DROP EDGE " + edgeName);
			}
		}
	}

	private void dropQuietly(NebulaConnection connection, String nGql) {
		try {
			connection.execute(nGql);
		} catch (Exception ignored) {
		}
	}
}
