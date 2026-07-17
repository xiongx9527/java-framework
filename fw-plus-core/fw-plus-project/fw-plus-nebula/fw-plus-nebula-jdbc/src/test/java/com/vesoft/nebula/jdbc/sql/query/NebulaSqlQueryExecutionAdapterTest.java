package com.vesoft.nebula.jdbc.sql.query;

import com.vesoft.nebula.client.graph.data.ResultSet;
import com.vesoft.nebula.jdbc.NebulaConnection;
import com.vesoft.nebula.jdbc.NebulaResultSet;
import org.junit.Test;

import java.sql.SQLException;
import java.util.Collections;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class NebulaSqlQueryExecutionAdapterTest {

	@Test
	public void executeQueryShouldUseExistingNebulaExecutionPath() throws Exception {
		NebulaConnection connection = mock(NebulaConnection.class);
		ResultSet nativeResultSet = mock(ResultSet.class);
		when(nativeResultSet.isSucceeded()).thenReturn(true);
		when(nativeResultSet.isEmpty()).thenReturn(true);
		when(nativeResultSet.getColumnNames()).thenReturn(Collections.<String>emptyList());
		when(connection.execute("MATCH (person:person) RETURN person.name AS name")).thenReturn(nativeResultSet);
		NgqlQueryPlan plan = new NgqlQueryPlan(
				"MATCH (person:person) RETURN person.name AS name",
				QueryKind.VERTEX_QUERY,
				Collections.<QueryColumn>emptyList(),
				QueryFidelity.EQUIVALENT,
				Collections.singletonList("test"));

		java.sql.ResultSet resultSet = new NebulaSqlQueryExecutionAdapter().executeQuery(connection, plan);

		verify(connection).execute("MATCH (person:person) RETURN person.name AS name");
		assertTrue(resultSet instanceof NebulaResultSet);
		assertSame(nativeResultSet, ((NebulaResultSet) resultSet).getNativeNebulaResultSet());
	}

	@Test
	public void executeQueryShouldWrapFailedNativeResult() throws Exception {
		NebulaConnection connection = mock(NebulaConnection.class);
		ResultSet nativeResultSet = mock(ResultSet.class);
		when(nativeResultSet.isSucceeded()).thenReturn(false);
		when(nativeResultSet.getErrorCode()).thenReturn(-1004);
		when(nativeResultSet.getErrorMessage()).thenReturn("syntax error");
		when(connection.execute("BROKEN")).thenReturn(nativeResultSet);
		NgqlQueryPlan plan = new NgqlQueryPlan(
				"BROKEN",
				QueryKind.VERTEX_QUERY,
				Collections.<QueryColumn>emptyList(),
				QueryFidelity.EQUIVALENT,
				Collections.singletonList("test"));

		try {
			new NebulaSqlQueryExecutionAdapter().executeQuery(connection, plan);
			fail("Expected SQLException");
		} catch (SQLException exception) {
			assertTrue(exception.getMessage().contains("nGql \"BROKEN\" executed failed"));
			assertTrue(exception.getMessage().contains("-1004"));
			assertTrue(exception.getMessage().contains("syntax error"));
		}
	}
}
