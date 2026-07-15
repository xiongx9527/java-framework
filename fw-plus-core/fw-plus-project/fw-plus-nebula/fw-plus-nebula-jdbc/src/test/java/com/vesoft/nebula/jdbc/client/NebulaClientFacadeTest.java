package com.vesoft.nebula.jdbc.client;

import com.vesoft.nebula.client.graph.NebulaPoolConfig;
import com.vesoft.nebula.client.graph.data.HostAddress;
import com.vesoft.nebula.client.graph.data.ResultSet;
import com.vesoft.nebula.client.graph.exception.IOErrorException;
import com.vesoft.nebula.client.graph.net.NebulaPool;
import com.vesoft.nebula.client.graph.net.Session;
import com.vesoft.nebula.jdbc.config.NebulaConnectionConfig;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.sql.SQLException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class NebulaClientFacadeTest {

    @Test
    @SuppressWarnings("unchecked")
    public void openShouldInitializePoolGetSessionAndUseGraphspaceFromConfig() throws Exception {
        NebulaPool pool = mock(NebulaPool.class);
        Session session = mock(Session.class);
        ResultSet useResult = successfulResult();
        NebulaClientFacade facade = new NebulaClientFacade(pool);

        when(pool.init(any(List.class), any(NebulaPoolConfig.class))).thenReturn(true);
        when(pool.getSession("root", "nebula", true)).thenReturn(session);
        when(session.execute("use test_space")).thenReturn(useResult);

        facade.open(config());

        ArgumentCaptor<List> addressesCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<NebulaPoolConfig> poolConfigCaptor = ArgumentCaptor.forClass(NebulaPoolConfig.class);
        verify(pool).init(addressesCaptor.capture(), poolConfigCaptor.capture());
        List<HostAddress> addresses = addressesCaptor.getValue();
        assertEquals(1, addresses.size());
        assertEquals("127.0.0.1", addresses.get(0).getHost());
        assertEquals(9669, addresses.get(0).getPort());
        assertEquals(2, poolConfigCaptor.getValue().getMinConnSize());
        assertEquals(11, poolConfigCaptor.getValue().getMaxConnSize());
        assertEquals(1000, poolConfigCaptor.getValue().getTimeout());
        assertEquals(2000, poolConfigCaptor.getValue().getIdleTime());
        assertEquals(3000, poolConfigCaptor.getValue().getIntervalIdle());
        assertEquals(4000, poolConfigCaptor.getValue().getWaitTime());
        verify(pool).getSession("root", "nebula", true);
        verify(session).execute("use test_space");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void openShouldThrowSQLExceptionWhenUseGraphspaceFails() throws Exception {
        NebulaPool pool = mock(NebulaPool.class);
        Session session = mock(Session.class);
        ResultSet failedUse = mock(ResultSet.class);
        NebulaClientFacade facade = new NebulaClientFacade(pool);

        when(pool.init(any(List.class), any(NebulaPoolConfig.class))).thenReturn(true);
        when(pool.getSession("root", "nebula", true)).thenReturn(session);
        when(session.execute("use test_space")).thenReturn(failedUse);
        when(failedUse.isSucceeded()).thenReturn(false);
        when(failedUse.getErrorCode()).thenReturn(-1005);
        when(failedUse.getErrorMessage()).thenReturn("space not found");

        try {
            facade.open(config());
            fail("Expected SQLException");
        } catch (SQLException exception) {
            assertTrue(exception.getMessage().contains("test_space"));
            assertTrue(exception.getMessage().contains("space not found"));
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void executeNgqlShouldWrapNebulaClientExceptionAsSQLException() throws Exception {
        NebulaPool pool = mock(NebulaPool.class);
        Session session = mock(Session.class);
        ResultSet useResult = successfulResult();
        NebulaClientFacade facade = new NebulaClientFacade(pool);

        when(pool.init(any(List.class), any(NebulaPoolConfig.class))).thenReturn(true);
        when(pool.getSession("root", "nebula", true)).thenReturn(session);
        when(session.execute("use test_space")).thenReturn(useResult);
        when(session.execute("MATCH (v) RETURN v")).thenThrow(new IOErrorException(-1, "network down"));
        facade.open(config());

        try {
            facade.executeNgql("MATCH (v) RETURN v");
            fail("Expected SQLException");
        } catch (SQLException exception) {
            assertTrue(exception.getMessage().contains("network down"));
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void closeShouldReleaseSessionAndClosePoolOnlyOnce() throws Exception {
        NebulaPool pool = mock(NebulaPool.class);
        Session session = mock(Session.class);
        ResultSet useResult = successfulResult();
        NebulaClientFacade facade = new NebulaClientFacade(pool);

        when(pool.init(any(List.class), any(NebulaPoolConfig.class))).thenReturn(true);
        when(pool.getSession("root", "nebula", true)).thenReturn(session);
        when(session.execute("use test_space")).thenReturn(useResult);
        facade.open(config());

        facade.close();
        facade.close();

        verify(session, times(1)).release();
        verify(pool, times(1)).close();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void executeNgqlShouldReturnNebulaResultSetFromSession() throws Exception {
        NebulaPool pool = mock(NebulaPool.class);
        Session session = mock(Session.class);
        ResultSet resultSet = successfulResult();
        ResultSet useResult = successfulResult();
        NebulaClientFacade facade = new NebulaClientFacade(pool);

        when(pool.init(any(List.class), any(NebulaPoolConfig.class))).thenReturn(true);
        when(pool.getSession("root", "nebula", true)).thenReturn(session);
        when(session.execute("use test_space")).thenReturn(useResult);
        when(session.execute(eq("MATCH (v) RETURN v"))).thenReturn(resultSet);
        facade.open(config());

        assertSame(resultSet, facade.executeNgql("MATCH (v) RETURN v"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void openShouldThrowSQLExceptionAndClosePoolWhenPoolInitReturnsFalse() throws Exception {
        NebulaPool pool = mock(NebulaPool.class);
        NebulaClientFacade facade = new NebulaClientFacade(pool);

        when(pool.init(any(List.class), any(NebulaPoolConfig.class))).thenReturn(false);

        try {
            facade.open(config());
            fail("Expected SQLException when NebulaPool init returns false");
        } catch (NullPointerException exception) {
            fail("NebulaPool init=false should fail before getSession");
        } catch (SQLException exception) {
            assertTrue(exception.getMessage().contains("graphAddress"));
            assertTrue(exception.getMessage().contains("127.0.0.1:9669"));
        }

        verify(pool, never()).getSession(any(String.class), any(String.class), eq(true));
        verify(pool).close();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void openShouldReleaseSessionAndClosePoolWhenUseGraphspaceFails() throws Exception {
        NebulaPool pool = mock(NebulaPool.class);
        Session session = mock(Session.class);
        ResultSet failedUse = mock(ResultSet.class);
        NebulaClientFacade facade = new NebulaClientFacade(pool);

        when(pool.init(any(List.class), any(NebulaPoolConfig.class))).thenReturn(true);
        when(pool.getSession("root", "nebula", true)).thenReturn(session);
        when(session.execute("use test_space")).thenReturn(failedUse);
        when(failedUse.isSucceeded()).thenReturn(false);
        when(failedUse.getErrorCode()).thenReturn(-1005);
        when(failedUse.getErrorMessage()).thenReturn("space not found");

        try {
            facade.open(config());
            fail("Expected SQLException");
        } catch (SQLException exception) {
            assertTrue(exception.getMessage().contains("test_space"));
        }

        verify(session).release();
        verify(pool).close();
    }

    @Test
    public void executeNgqlShouldThrowSQLExceptionWhenNotOpened() throws Exception {
        NebulaClientFacade facade = new NebulaClientFacade(mock(NebulaPool.class));

        try {
            facade.executeNgql("MATCH (v) RETURN v");
            fail("Expected SQLException");
        } catch (SQLException exception) {
            assertTrue(exception.getMessage().contains("not open"));
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void executeNgqlShouldThrowSQLExceptionWhenClosed() throws Exception {
        NebulaPool pool = mock(NebulaPool.class);
        Session session = mock(Session.class);
        ResultSet useResult = successfulResult();
        NebulaClientFacade facade = new NebulaClientFacade(pool);

        when(pool.init(any(List.class), any(NebulaPoolConfig.class))).thenReturn(true);
        when(pool.getSession("root", "nebula", true)).thenReturn(session);
        when(session.execute("use test_space")).thenReturn(useResult);
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
    public void openShouldWrapNonNumericGraphAddressPortAsSQLException() throws Exception {
        NebulaClientFacade facade = new NebulaClientFacade(mock(NebulaPool.class));
        NebulaConnectionConfig invalidConfig = new NebulaConnectionConfig("//127.0.0.1:not-a-port", "test_space", "root", "nebula", 2, 11, 1000, 2000, 3000, 4000);

        try {
            facade.open(invalidConfig);
            fail("Expected SQLException");
        } catch (SQLException exception) {
            assertTrue(exception.getMessage().contains("graphAddress"));
            assertTrue(exception.getMessage().contains("127.0.0.1:not-a-port"));
        }
    }

    private static NebulaConnectionConfig config() {
        return new NebulaConnectionConfig("//127.0.0.1:9669", "test_space", "root", "nebula", 2, 11, 1000, 2000, 3000, 4000);
    }

    private static ResultSet successfulResult() {
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.isSucceeded()).thenReturn(true);
        return resultSet;
    }
}
