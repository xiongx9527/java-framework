package com.xiongx9527.fw.plus.graph.nebula.jdbc.client;

import com.xiongx9527.fw.plus.graph.nebula.jdbc.config.NebulaConnectionConfig;
import org.junit.Test;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import static com.xiongx9527.fw.plus.graph.nebula.jdbc.test.NativeResultSets.failure;
import static com.xiongx9527.fw.plus.graph.nebula.jdbc.test.NativeResultSets.success;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class NebulaClientFacadeTest {

    @Test
    public void openShouldOpenNativeClientAndUseGraphspaceFromConfig() throws Exception {
        RecordingNativeClient nativeClient = new RecordingNativeClient();
        NebulaClientFacade facade = new NebulaClientFacade(nativeClient);

        facade.open(config());

        assertEquals(Collections.singletonList("USE test_space"), nativeClient.executed);
        assertSame(nativeClient.nativeClient, facade.getNativeClient());
    }

    @Test
    public void openShouldThrowSQLExceptionWhenUseGraphspaceFails() throws Exception {
        RecordingNativeClient nativeClient = new RecordingNativeClient();
        nativeClient.useResult = failure("CATALOG_GRAPH_NOT_FOUND", "space not found");
        NebulaClientFacade facade = new NebulaClientFacade(nativeClient);

        try {
            facade.open(config());
            fail("Expected SQLException");
        } catch (SQLException exception) {
            assertTrue(exception.getMessage().contains("test_space"));
            assertTrue(exception.getMessage().contains("space not found"));
        }

        assertTrue(nativeClient.closed);
    }

    @Test
    public void executeNgqlShouldWrapNativeClientExceptionAsSQLException() throws Exception {
        RecordingNativeClient nativeClient = new RecordingNativeClient();
        nativeClient.failure = new SQLException("network down");
        NebulaClientFacade facade = new NebulaClientFacade(nativeClient);
        facade.open(config());

        try {
            facade.executeNgql("MATCH (v) RETURN v");
            fail("Expected SQLException");
        } catch (SQLException exception) {
            assertTrue(exception.getMessage().contains("network down"));
        }
    }

    @Test
    public void closeShouldCloseNativeClientOnlyOnce() throws Exception {
        RecordingNativeClient nativeClient = new RecordingNativeClient();
        NebulaClientFacade facade = new NebulaClientFacade(nativeClient);
        facade.open(config());

        facade.close();
        facade.close();

        assertEquals(1, nativeClient.closeCount);
    }

    @Test
    public void executeNgqlShouldReturnNativeResultSetFromClient() throws Exception {
        RecordingNativeClient nativeClient = new RecordingNativeClient();
        NebulaNativeResultSet resultSet = success();
        nativeClient.queryResult = resultSet;
        NebulaClientFacade facade = new NebulaClientFacade(nativeClient);
        facade.open(config());

        assertSame(resultSet, facade.executeNgql("MATCH (v) RETURN v"));
    }

    @Test
    public void nativeAccessorsShouldExposeOpenedNativeObjects() throws Exception {
        RecordingNativeClient nativeClient = new RecordingNativeClient();
        NebulaClientFacade facade = new NebulaClientFacade(nativeClient);
        facade.open(config());

        assertSame(nativeClient.nativePool, facade.getNativePool());
        assertSame(nativeClient.nativeClient, facade.getNativeClient());
        assertSame(nativeClient.nativeSession, facade.getNativeSession());
    }

    @Test
    public void defaultConstructorShouldCreateClientThroughFactoryWhenOpening() throws Exception {
        RecordingNativeClient nativeClient = new RecordingNativeClient();
        RecordingFactory factory = new RecordingFactory(nativeClient);
        NebulaConnectionConfig config = config();
        NebulaClientFacade facade = new NebulaClientFacade(factory);

        facade.open(config);

        assertTrue(factory.created);
        assertEquals(Collections.singletonList("USE test_space"), nativeClient.executed);
    }

    @Test
    public void openShouldCloseCreatedClientWhenNativeOpenThrows() throws Exception {
        RecordingNativeClient nativeClient = new RecordingNativeClient();
        nativeClient.openFailure = new SQLException("pool exhausted");
        NebulaClientFacade facade = new NebulaClientFacade(nativeClient);

        try {
            facade.open(config());
            fail("Expected SQLException when native open throws");
        } catch (SQLException exception) {
            assertTrue(exception.getMessage().contains("pool exhausted"));
        }

        assertTrue(nativeClient.closed);
    }

    @Test
    public void openFailureShouldEnterClosedStateAndNotReleaseResourcesAgainWhenUseGraphspaceFails() throws Exception {
        RecordingNativeClient nativeClient = new RecordingNativeClient();
        nativeClient.useResult = failure("CATALOG_GRAPH_NOT_FOUND", "space not found");
        NebulaClientFacade facade = new NebulaClientFacade(nativeClient);

        try {
            facade.open(config());
            fail("Expected SQLException");
        } catch (SQLException exception) {
            assertTrue(exception.getMessage().contains("test_space"));
        }

        assertTrue(facade.isClosed());
        facade.close();

        assertEquals(1, nativeClient.closeCount);
    }

    @Test
    public void executeNgqlShouldThrowSQLExceptionWhenNotOpened() throws Exception {
        NebulaClientFacade facade = new NebulaClientFacade(new RecordingNativeClient());

        try {
            facade.executeNgql("MATCH (v) RETURN v");
            fail("Expected SQLException");
        } catch (SQLException exception) {
            assertTrue(exception.getMessage().contains("closed"));
        }
    }

    @Test
    public void executeNgqlShouldThrowSQLExceptionWhenClosed() throws Exception {
        RecordingNativeClient nativeClient = new RecordingNativeClient();
        NebulaClientFacade facade = new NebulaClientFacade(nativeClient);
        facade.open(config());
        facade.close();

        try {
            facade.executeNgql("MATCH (v) RETURN v");
            fail("Expected SQLException");
        } catch (SQLException exception) {
            assertTrue(exception.getMessage().contains("closed"));
        }
    }

    @Test
    public void factoryShouldRejectClientAndDriverOnSameClasspath() throws Exception {
        NebulaPoolFactory factory = new NebulaPoolFactory() {
            @Override
            boolean hasClass(String className) {
                return true;
            }
        };

        try {
            factory.create();
            fail("Expected SQLException");
        } catch (SQLException exception) {
            assertTrue(exception.getMessage().contains("must not appear"));
        }
    }

    private static NebulaConnectionConfig config() {
        Properties mergedProperties = new Properties();
        mergedProperties.setProperty("graphspace", "test_space");
        mergedProperties.setProperty("user", "root");
        mergedProperties.setProperty("password", "nebula");
        return new NebulaConnectionConfig("test_space", "root", "nebula",
                Collections.singletonList(new NebulaHostAddress("127.0.0.1", 9669)),
                2, 11, 1000, 2000, 3000, 4000, mergedProperties);
    }

    private static class RecordingFactory extends NebulaPoolFactory {
        private final NebulaNativeClient nativeClient;
        private boolean created;

        private RecordingFactory(NebulaNativeClient nativeClient) {
            this.nativeClient = nativeClient;
        }

        @Override
        public NebulaNativeClient create() {
            created = true;
            return nativeClient;
        }
    }

    private static class RecordingNativeClient implements NebulaNativeClient {
        private final Object nativeClient = new Object();
        private final Object nativeSession = new Object();
        private final Object nativePool = new Object();
        private final List<String> executed = new ArrayList<String>();
        private NebulaNativeResultSet useResult;
        private NebulaNativeResultSet queryResult;
        private SQLException openFailure;
        private SQLException failure;
        private boolean closed = true;
        private int closeCount;

        private RecordingNativeClient() throws SQLException {
            useResult = success();
            queryResult = success();
        }

        @Override
        public void open(NebulaConnectionConfigHolder config) throws SQLException {
            if (openFailure != null) {
                throw openFailure;
            }
            closed = false;
        }

        @Override
        public NebulaNativeResultSet execute(String ngql) throws SQLException {
            if (failure != null && !"USE test_space".equals(ngql)) {
                throw failure;
            }
            executed.add(ngql);
            return "USE test_space".equals(ngql) ? useResult : queryResult;
        }

        @Override
        public Object getNativeClient() {
            return nativeClient;
        }

        @Override
        public Object getNativeSession() {
            return nativeSession;
        }

        @Override
        public Object getNativePool() {
            return nativePool;
        }

        @Override
        public boolean isClosed() {
            return closed;
        }

        @Override
        public void close() {
            closeCount++;
            closed = true;
        }
    }
}
