package com.xiongx9527.fw.plus.graph.neo4j.jdbc;

import org.junit.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.RowId;
import java.sql.SQLXML;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;
import java.sql.SQLException;
import java.sql.Struct;
import java.nio.charset.StandardCharsets;
import javax.sql.rowset.serial.SerialBlob;
import javax.sql.rowset.serial.SerialClob;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class Neo4jDriverTest {

    @Test
    public void shouldAcceptNeo4jJdbcUrls() throws Exception {
        Neo4jDriver driver = new Neo4jDriver();

        assertTrue(driver.acceptsURL("jdbc:neo4j://127.0.0.1:7687/neo4j"));
        assertTrue(driver.acceptsURL("jdbc:neo4j+s://db.example.com:7687/sales"));
        assertFalse(driver.acceptsURL("jdbc:mysql://127.0.0.1:3306/test"));
    }

    @Test
    public void shouldRegisterWithDriverManager() throws Exception {
        Neo4jDriver.setExecutorFactory(new FakeNeo4jQueryExecutorFactory());
        try {
            Properties properties = new Properties();
            properties.setProperty("user", "neo4j");
            properties.setProperty("password", "secret");

            Connection connection = DriverManager.getConnection("jdbc:neo4j://localhost:7687/neo4j", properties);

            assertTrue(connection instanceof Neo4jConnection);
            connection.close();
        }
        finally {
            Neo4jDriver.setExecutorFactory(null);
        }
    }

    @Test
    public void shouldExecuteCypherQueryThroughStatement() throws Exception {
        Neo4jDriver.setExecutorFactory(new FakeNeo4jQueryExecutorFactory());
        try (Connection connection = new Neo4jDriver().connect("jdbc:neo4j://localhost:7687/neo4j", new Properties());
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("MATCH (n) RETURN n.name AS name")) {

            assertTrue(resultSet.next());
            assertEquals("Alice", resultSet.getString("name"));
            assertFalse(resultSet.next());
        }
        finally {
            Neo4jDriver.setExecutorFactory(null);
        }
    }

    @Test
    public void shouldBindPreparedStatementParameters() throws Exception {
        FakeNeo4jQueryExecutorFactory factory = new FakeNeo4jQueryExecutorFactory();
        Neo4jDriver.setExecutorFactory(factory);
        try (Connection connection = new Neo4jDriver().connect("jdbc:neo4j://localhost:7687/neo4j", new Properties())) {
            java.sql.PreparedStatement statement = connection.prepareStatement(
                    "MATCH (n {name: ?}) RETURN n.name AS name, ? AS marker");
            statement.setString(1, "Alice");
            statement.setInt(2, 7);

            ResultSet resultSet = statement.executeQuery();

            assertEquals("MATCH (n {name: $p1}) RETURN n.name AS name, $p2 AS marker", factory.getLastCypher());
            assertEquals("Alice", factory.getLastParameters().get("p1"));
            assertEquals(Integer.valueOf(7), factory.getLastParameters().get("p2"));
            assertTrue(resultSet.next());
            assertEquals(7, resultSet.getInt("marker"));
            resultSet.close();
            statement.close();
        }
        finally {
            Neo4jDriver.setExecutorFactory(null);
        }
    }

    @Test
    public void shouldBindNullAndExecuteBatch() throws Exception {
        FakeNeo4jQueryExecutorFactory factory = new FakeNeo4jQueryExecutorFactory();
        Neo4jDriver.setExecutorFactory(factory);
        try (Connection connection = new Neo4jDriver().connect("jdbc:neo4j://localhost:7687/neo4j", new Properties())) {
            java.sql.PreparedStatement statement = connection.prepareStatement("RETURN ? AS marker");
            statement.setNull(1, java.sql.Types.INTEGER);
            statement.addBatch();

            int[] counts = statement.executeBatch();

            assertEquals(1, counts.length);
            assertEquals("RETURN $p1 AS marker", factory.getLastCypher());
            assertTrue(factory.getLastParameters().containsKey("p1"));
            assertEquals(null, factory.getLastParameters().get("p1"));
            statement.close();
        }
        finally {
            Neo4jDriver.setExecutorFactory(null);
        }
    }

    @Test
    public void shouldExposePreparedParameterMetadataAndGeneratedKeysShape() throws Exception {
        Neo4jDriver.setExecutorFactory(new FakeNeo4jQueryExecutorFactory());
        try (Connection connection = new Neo4jDriver().connect("jdbc:neo4j://localhost:7687/neo4j", new Properties());
             Statement statement = connection.createStatement();
             PreparedStatement preparedStatement = connection.prepareStatement(
                     "MATCH (n {name: ?}) RETURN ? AS marker")) {

            ParameterMetaData parameterMetaData = preparedStatement.getParameterMetaData();
            assertEquals(2, parameterMetaData.getParameterCount());
            assertEquals(ParameterMetaData.parameterModeIn, parameterMetaData.getParameterMode(1));
            assertEquals(java.sql.Types.JAVA_OBJECT, parameterMetaData.getParameterType(1));
            assertEquals(Object.class.getName(), parameterMetaData.getParameterClassName(1));

            ResultSet generatedKeys = statement.getGeneratedKeys();
            assertEquals("GENERATED_KEY", generatedKeys.getMetaData().getColumnLabel(1));
            assertFalse(generatedKeys.next());
            generatedKeys.close();
        }
        finally {
            Neo4jDriver.setExecutorFactory(null);
        }
    }

    @Test
    public void shouldExposeStandardStatementResultSemantics() throws Exception {
        Neo4jDriver.setExecutorFactory(new FakeNeo4jQueryExecutorFactory());
        try (Connection connection = new Neo4jDriver().connect("jdbc:neo4j://localhost:7687/neo4j", new Properties());
             Statement statement = connection.createStatement()) {

            assertTrue(statement.execute("MATCH (n) RETURN n.name AS name"));
            assertTrue(statement.getResultSet().next());
            assertEquals(-1, statement.getUpdateCount());

            assertFalse(statement.execute("CREATE (n:Person {id: 'p1'})"));
            assertEquals(null, statement.getResultSet());
            assertEquals(1, statement.getUpdateCount());

            try {
                statement.executeQuery("CREATE (n:Person {id: 'p2'})");
                org.junit.Assert.fail("Expected SQLException");
            }
            catch (SQLException expected) {
                assertTrue(expected.getMessage().contains("did not produce a ResultSet"));
            }

            try {
                statement.executeUpdate("MATCH (n) RETURN n.name AS name");
                org.junit.Assert.fail("Expected SQLException");
            }
            catch (SQLException expected) {
                assertTrue(expected.getMessage().contains("produced a ResultSet"));
            }
        }
        finally {
            Neo4jDriver.setExecutorFactory(null);
        }
    }

    @Test
    public void shouldRejectSqlArgumentsOnPreparedStatementExecutionMethods() throws Exception {
        Neo4jDriver.setExecutorFactory(new FakeNeo4jQueryExecutorFactory());
        try (Connection connection = new Neo4jDriver().connect("jdbc:neo4j://localhost:7687/neo4j", new Properties())) {
            PreparedStatement statement = connection.prepareStatement("MATCH (n) RETURN n.name AS name");
            try {
                statement.executeQuery("MATCH (n) RETURN n.name AS name");
                org.junit.Assert.fail("Expected SQLException");
            }
            catch (SQLException expected) {
                assertTrue(expected.getMessage().contains("cannot be called on PreparedStatement"));
            }
            try {
                statement.executeUpdate("CREATE (n:Person)");
                org.junit.Assert.fail("Expected SQLException");
            }
            catch (SQLException expected) {
                assertTrue(expected.getMessage().contains("cannot be called on PreparedStatement"));
            }
            statement.close();
        }
        finally {
            Neo4jDriver.setExecutorFactory(null);
        }
    }

    @Test
    public void shouldTranslateBasicSelectSqlToCypher() throws Exception {
        FakeNeo4jQueryExecutorFactory factory = new FakeNeo4jQueryExecutorFactory();
        Neo4jDriver.setExecutorFactory(factory);
        try (Connection connection = new Neo4jDriver().connect("jdbc:neo4j://localhost:7687/neo4j", new Properties());
             Statement statement = connection.createStatement()) {

            statement.executeQuery("SELECT name, age FROM Person WHERE age > 18 ORDER BY age DESC LIMIT 10");

            assertEquals("MATCH (n:Person) WHERE n.age > 18 RETURN n.name AS name, n.age AS age ORDER BY n.age DESC LIMIT 10",
                    factory.getLastCypher());
            assertTrue(factory.getLastParameters().isEmpty());
        }
        finally {
            Neo4jDriver.setExecutorFactory(null);
        }
    }

    @Test
    public void shouldTranslatePreparedInsertUpdateAndDeleteSqlToCypher() throws Exception {
        FakeNeo4jQueryExecutorFactory factory = new FakeNeo4jQueryExecutorFactory();
        Neo4jDriver.setExecutorFactory(factory);
        try (Connection connection = new Neo4jDriver().connect("jdbc:neo4j://localhost:7687/neo4j", new Properties())) {
            java.sql.PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO Person(id, name, age) VALUES (?, ?, ?)");
            insert.setString(1, "p1");
            insert.setString(2, "Alice");
            insert.setInt(3, 30);
            insert.executeUpdate();
            assertEquals("CREATE (n:Person {id: $p1, name: $p2, age: $p3})", factory.getLastCypher());
            assertEquals("Alice", factory.getLastParameters().get("p2"));

            java.sql.PreparedStatement update = connection.prepareStatement("UPDATE Person SET age = ? WHERE id = ?");
            update.setInt(1, 31);
            update.setString(2, "p1");
            update.executeUpdate();
            assertEquals("MATCH (n:Person) WHERE n.id = $p2 SET n.age = $p1", factory.getLastCypher());

            java.sql.PreparedStatement delete = connection.prepareStatement("DELETE FROM Person WHERE id = ?");
            delete.setString(1, "p1");
            delete.executeUpdate();
            assertEquals("MATCH (n:Person) WHERE n.id = $p1 DELETE n", factory.getLastCypher());
        }
        finally {
            Neo4jDriver.setExecutorFactory(null);
        }
    }

    @Test
    public void shouldRejectUnsupportedSqlInsteadOfPretendingFullSqlSupport() throws Exception {
        Neo4jDriver.setExecutorFactory(new FakeNeo4jQueryExecutorFactory());
        try (Connection connection = new Neo4jDriver().connect("jdbc:neo4j://localhost:7687/neo4j", new Properties());
             Statement statement = connection.createStatement()) {
            try {
                statement.executeQuery("SELECT p.name FROM Person p JOIN Team t ON p.team_id = t.id");
            }
            catch (SQLException expected) {
                assertTrue(expected.getMessage().contains("Unsupported Neo4j SQL"));
            }
        }
        finally {
            Neo4jDriver.setExecutorFactory(null);
        }
    }

    @Test
    public void shouldExposeBasicMetadataAndTransactionCapabilities() throws Exception {
        FakeNeo4jQueryExecutorFactory factory = new FakeNeo4jQueryExecutorFactory();
        Neo4jDriver.setExecutorFactory(factory);
        try (Connection connection = new Neo4jDriver().connect("jdbc:neo4j://localhost:7687/neo4j", new Properties())) {
            assertEquals("Neo4j", connection.getMetaData().getDatabaseProductName());
            assertEquals("fw-plus-neo4j-jdbc", connection.getMetaData().getDriverName());
            assertTrue(connection.getMetaData().supportsTransactions());
            assertTrue(connection.getMetaData().supportsTransactionIsolationLevel(Connection.TRANSACTION_READ_COMMITTED));

            connection.setAutoCommit(false);
            assertFalse(connection.getAutoCommit());
            assertFalse(factory.isAutoCommit());
            connection.commit();
            assertEquals(1, factory.getCommitCount());
            connection.rollback();
            assertEquals(1, factory.getRollbackCount());
            connection.setAutoCommit(true);
            assertTrue(connection.getAutoCommit());
        }
        finally {
            Neo4jDriver.setExecutorFactory(null);
        }
    }

    @Test
    public void shouldKeepConnectionLocalStateAndTypeMap() throws Exception {
        Neo4jDriver.setExecutorFactory(new FakeNeo4jQueryExecutorFactory());
        try (Connection connection = new Neo4jDriver().connect("jdbc:neo4j://localhost:7687/neo4j", new Properties())) {
            connection.setClientInfo("traceId", "t1");
            connection.setNetworkTimeout(null, 1234);
            connection.setCatalog("catalog1");
            connection.setSchema("schema1");
            Map<String, Class<?>> typeMap = new HashMap<String, Class<?>>();
            typeMap.put("Person", String.class);
            connection.setTypeMap(typeMap);

            assertEquals("SELECT 1", connection.nativeSQL("SELECT 1"));
            assertEquals("t1", connection.getClientInfo("traceId"));
            assertEquals(1234, connection.getNetworkTimeout());
            assertEquals("schema1", connection.getCatalog());
            assertEquals("schema1", connection.getSchema());
            assertEquals(String.class, connection.getTypeMap().get("Person"));
            assertTrue(connection.unwrap(Connection.class) == connection);
            assertTrue(connection.isWrapperFor(Connection.class));
        }
        finally {
            Neo4jDriver.setExecutorFactory(null);
        }
    }

    @Test
    public void shouldKeepStatementLocalStateAndMetadataCapabilities() throws Exception {
        Neo4jDriver.setExecutorFactory(new FakeNeo4jQueryExecutorFactory());
        try (Connection connection = new Neo4jDriver().connect("jdbc:neo4j://localhost:7687/neo4j", new Properties());
             Statement statement = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
                     ResultSet.CONCUR_READ_ONLY, ResultSet.CLOSE_CURSORS_AT_COMMIT)) {
            statement.setMaxRows(50);
            statement.setMaxFieldSize(128);
            statement.setFetchSize(20);
            statement.setFetchDirection(ResultSet.FETCH_REVERSE);
            statement.setQueryTimeout(3);
            statement.setPoolable(true);
            statement.closeOnCompletion();

            assertEquals(50, statement.getMaxRows());
            assertEquals(128, statement.getMaxFieldSize());
            assertEquals(20, statement.getFetchSize());
            assertEquals(ResultSet.FETCH_REVERSE, statement.getFetchDirection());
            assertEquals(3, statement.getQueryTimeout());
            assertTrue(statement.isPoolable());
            assertTrue(statement.isCloseOnCompletion());
            assertEquals(ResultSet.TYPE_SCROLL_INSENSITIVE, statement.getResultSetType());
            assertEquals(ResultSet.CONCUR_READ_ONLY, statement.getResultSetConcurrency());
            assertEquals(ResultSet.CLOSE_CURSORS_AT_COMMIT, statement.getResultSetHoldability());
            assertTrue(connection.getMetaData().supportsBatchUpdates());
            assertFalse(connection.getMetaData().supportsGetGeneratedKeys());
            assertFalse(connection.getMetaData().supportsResultSetConcurrency(
                    ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE));
        }
        finally {
            Neo4jDriver.setExecutorFactory(null);
        }
    }

    @Test
    public void shouldCreateStandardLocalJdbcObjects() throws Exception {
        Neo4jDriver.setExecutorFactory(new FakeNeo4jQueryExecutorFactory());
        try (Connection connection = new Neo4jDriver().connect("jdbc:neo4j://localhost:7687/neo4j", new Properties())) {
            Blob blob = connection.createBlob();
            blob.setBytes(1, new byte[] {1, 2, 3});
            assertEquals(3L, blob.length());
            assertEquals(2, blob.getBytes(2, 1)[0]);

            Clob clob = connection.createClob();
            clob.setString(1, "hello");
            assertEquals("ell", clob.getSubString(2, 3));

            NClob nClob = connection.createNClob();
            nClob.setString(1, "neo4j");
            assertEquals("neo4j", nClob.getSubString(1, 5));

            SQLXML sqlxml = connection.createSQLXML();
            sqlxml.setString("<root><name>Alice</name></root>");
            assertEquals("<root><name>Alice</name></root>", sqlxml.getString());

            Array array = connection.createArrayOf("VARCHAR", new Object[] {"a", "b"});
            assertEquals("VARCHAR", array.getBaseTypeName());
            ResultSet arrayRows = array.getResultSet();
            assertTrue(arrayRows.next());
            assertEquals(1L, arrayRows.getLong(1));
            assertEquals("a", arrayRows.getString(2));
            arrayRows.close();

            Struct struct = connection.createStruct("Person", new Object[] {"Alice", 30});
            assertEquals("Person", struct.getSQLTypeName());
            assertEquals("Alice", struct.getAttributes()[0]);
        }
        finally {
            Neo4jDriver.setExecutorFactory(null);
        }
    }

    @Test
    public void shouldBindRowIdAndRefParameters() throws Exception {
        FakeNeo4jQueryExecutorFactory factory = new FakeNeo4jQueryExecutorFactory();
        Neo4jDriver.setExecutorFactory(factory);
        try (Connection connection = new Neo4jDriver().connect("jdbc:neo4j://localhost:7687/neo4j", new Properties())) {
            PreparedStatement statement = connection.prepareStatement("RETURN ? AS rowId, ? AS refValue");
            RowId rowId = new Neo4jRowId("node-1");
            Ref ref = new Neo4jRef("Person", "alice");
            statement.setRowId(1, rowId);
            statement.setRef(2, ref);

            statement.executeQuery();

            assertEquals("node-1", factory.getLastParameters().get("p1"));
            assertEquals("alice", factory.getLastParameters().get("p2"));
            statement.close();
        }
        finally {
            Neo4jDriver.setExecutorFactory(null);
        }
    }

    @Test
    public void shouldMaterializeJdbcObjectParametersForNeo4jDriver() throws Exception {
        FakeNeo4jQueryExecutorFactory factory = new FakeNeo4jQueryExecutorFactory();
        Neo4jDriver.setExecutorFactory(factory);
        try (Connection connection = new Neo4jDriver().connect("jdbc:neo4j://localhost:7687/neo4j", new Properties())) {
            PreparedStatement statement = connection.prepareStatement(
                    "RETURN ? AS payload, ? AS content, ? AS xml, ? AS tags");
            SQLXML sqlxml = connection.createSQLXML();
            sqlxml.setString("<root/>");
            Array array = connection.createArrayOf("VARCHAR", new Object[] {"a", "b"});
            statement.setBlob(1, new SerialBlob("blob".getBytes(StandardCharsets.UTF_8)));
            statement.setClob(2, new SerialClob("clob".toCharArray()));
            statement.setSQLXML(3, sqlxml);
            statement.setArray(4, array);

            statement.executeQuery();

            assertEquals("blob", new String((byte[]) factory.getLastParameters().get("p1"), StandardCharsets.UTF_8));
            assertEquals("clob", factory.getLastParameters().get("p2"));
            assertEquals("<root/>", factory.getLastParameters().get("p3"));
            org.junit.Assert.assertArrayEquals(new Object[] {"a", "b"},
                    (Object[]) factory.getLastParameters().get("p4"));
            statement.close();
        }
        finally {
            Neo4jDriver.setExecutorFactory(null);
        }
    }

    @Test
    public void shouldExposeMetadataResultShapes() throws Exception {
        Neo4jDriver.setExecutorFactory(new FakeNeo4jQueryExecutorFactory());
        try (Connection connection = new Neo4jDriver().connect("jdbc:neo4j://localhost:7687/neo4j", new Properties())) {
            DatabaseMetaData metaData = connection.getMetaData();

            ResultSet tables = metaData.getTables(null, null, null, null);
            assertEquals("TABLE_NAME", tables.getMetaData().getColumnLabel(3));
            tables.close();

            ResultSet columns = metaData.getColumns(null, null, null, null);
            assertEquals("COLUMN_NAME", columns.getMetaData().getColumnLabel(4));
            columns.close();

            ResultSet primaryKeys = metaData.getPrimaryKeys(null, null, "Person");
            assertEquals("COLUMN_NAME", primaryKeys.getMetaData().getColumnLabel(4));
            primaryKeys.close();

            ResultSet indexes = metaData.getIndexInfo(null, null, "Person", false, false);
            assertEquals("INDEX_NAME", indexes.getMetaData().getColumnLabel(6));
            indexes.close();
        }
        finally {
            Neo4jDriver.setExecutorFactory(null);
        }
    }
}
