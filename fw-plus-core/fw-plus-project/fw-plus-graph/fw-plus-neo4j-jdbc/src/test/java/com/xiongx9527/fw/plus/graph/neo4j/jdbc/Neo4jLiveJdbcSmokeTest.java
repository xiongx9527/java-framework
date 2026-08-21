package com.xiongx9527.fw.plus.graph.neo4j.jdbc;

import org.junit.Assume;
import org.junit.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class Neo4jLiveJdbcSmokeTest {

    @Test
    public void shouldConnectAndRunCypherAgainstLiveNeo4j() throws Exception {
        Assume.assumeTrue(Boolean.getBoolean("neo4j.jdbc.live"));
        String host = System.getProperty("neo4j.jdbc.live.host", "127.0.0.1");
        String port = System.getProperty("neo4j.jdbc.live.port", "7687");
        String database = System.getProperty("neo4j.jdbc.live.database", "neo4j");
        String user = System.getProperty("neo4j.jdbc.live.user", "neo4j");
        String password = System.getProperty("neo4j.jdbc.live.password", "");
        String url = "jdbc:neo4j://" + host + ":" + port + "/" + database;

        Properties properties = new Properties();
        properties.setProperty("user", user);
        properties.setProperty("password", password);

        try (Connection connection = DriverManager.getConnection(url, properties);
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("RETURN 1 AS one")) {
            assertEquals("Neo4j", connection.getMetaData().getDatabaseProductName());
            assertTrue(resultSet.next());
            assertEquals(1, resultSet.getInt("one"));
        }
    }

    @Test
    public void shouldRunSqlSubsetAgainstLiveNeo4j() throws Exception {
        Assume.assumeTrue(Boolean.getBoolean("neo4j.jdbc.live"));
        String host = System.getProperty("neo4j.jdbc.live.host", "127.0.0.1");
        String port = System.getProperty("neo4j.jdbc.live.port", "7687");
        String database = System.getProperty("neo4j.jdbc.live.database", "neo4j");
        String user = System.getProperty("neo4j.jdbc.live.user", "neo4j");
        String password = System.getProperty("neo4j.jdbc.live.password", "");
        String url = "jdbc:neo4j://" + host + ":" + port + "/" + database;
        String id = "jdbc-" + UUID.randomUUID().toString();

        Properties properties = new Properties();
        properties.setProperty("user", user);
        properties.setProperty("password", password);

        try (Connection connection = DriverManager.getConnection(url, properties);
             Statement cleanup = connection.createStatement()) {
            cleanup.executeUpdate("MATCH (n:FwPlusNeo4jJdbcSmoke {id: '" + id + "'}) DELETE n");

            PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO FwPlusNeo4jJdbcSmoke(id, name, age) VALUES (?, ?, ?)");
            insert.setString(1, id);
            insert.setString(2, "Alice");
            insert.setInt(3, 30);
            assertTrue(insert.executeUpdate() > 0);
            insert.close();

            PreparedStatement select = connection.prepareStatement(
                    "SELECT name, age FROM FwPlusNeo4jJdbcSmoke WHERE id = ?");
            select.setString(1, id);
            ResultSet resultSet = select.executeQuery();
            assertTrue(resultSet.next());
            assertEquals("Alice", resultSet.getString("name"));
            assertEquals(30, resultSet.getInt("age"));
            assertFalse(resultSet.next());
            resultSet.close();
            select.close();

            PreparedStatement update = connection.prepareStatement(
                    "UPDATE FwPlusNeo4jJdbcSmoke SET age = ? WHERE id = ?");
            update.setInt(1, 31);
            update.setString(2, id);
            assertTrue(update.executeUpdate() > 0);
            update.close();

            PreparedStatement delete = connection.prepareStatement(
                    "DELETE FROM FwPlusNeo4jJdbcSmoke WHERE id = ?");
            delete.setString(1, id);
            assertTrue(delete.executeUpdate() > 0);
            delete.close();
        }
    }

    @Test
    public void shouldCommitAndRollbackJdbcTransactionsAgainstLiveNeo4j() throws Exception {
        Assume.assumeTrue(Boolean.getBoolean("neo4j.jdbc.live"));
        String host = System.getProperty("neo4j.jdbc.live.host", "127.0.0.1");
        String port = System.getProperty("neo4j.jdbc.live.port", "7687");
        String database = System.getProperty("neo4j.jdbc.live.database", "neo4j");
        String user = System.getProperty("neo4j.jdbc.live.user", "neo4j");
        String password = System.getProperty("neo4j.jdbc.live.password", "");
        String url = "jdbc:neo4j://" + host + ":" + port + "/" + database;
        String rollbackId = "jdbc-rollback-" + UUID.randomUUID().toString();
        String commitId = "jdbc-commit-" + UUID.randomUUID().toString();

        Properties properties = new Properties();
        properties.setProperty("user", user);
        properties.setProperty("password", password);

        try (Connection connection = DriverManager.getConnection(url, properties);
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("MATCH (n:FwPlusNeo4jJdbcTxSmoke) DELETE n");

            connection.setAutoCommit(false);
            PreparedStatement insertRollback = connection.prepareStatement(
                    "INSERT INTO FwPlusNeo4jJdbcTxSmoke(id, name) VALUES (?, ?)");
            insertRollback.setString(1, rollbackId);
            insertRollback.setString(2, "Rollback");
            insertRollback.executeUpdate();
            insertRollback.close();
            connection.rollback();

            ResultSet rollbackCheck = statement.executeQuery(
                    "MATCH (n:FwPlusNeo4jJdbcTxSmoke {id: '" + rollbackId + "'}) RETURN count(n) AS count");
            assertTrue(rollbackCheck.next());
            assertEquals(0, rollbackCheck.getInt("count"));
            rollbackCheck.close();

            PreparedStatement insertCommit = connection.prepareStatement(
                    "INSERT INTO FwPlusNeo4jJdbcTxSmoke(id, name) VALUES (?, ?)");
            insertCommit.setString(1, commitId);
            insertCommit.setString(2, "Commit");
            insertCommit.executeUpdate();
            insertCommit.close();
            connection.commit();
            connection.setAutoCommit(true);

            ResultSet commitCheck = statement.executeQuery(
                    "MATCH (n:FwPlusNeo4jJdbcTxSmoke {id: '" + commitId + "'}) RETURN count(n) AS count");
            assertTrue(commitCheck.next());
            assertEquals(1, commitCheck.getInt("count"));
            commitCheck.close();

            statement.executeUpdate("MATCH (n:FwPlusNeo4jJdbcTxSmoke) DELETE n");
        }
    }
}
