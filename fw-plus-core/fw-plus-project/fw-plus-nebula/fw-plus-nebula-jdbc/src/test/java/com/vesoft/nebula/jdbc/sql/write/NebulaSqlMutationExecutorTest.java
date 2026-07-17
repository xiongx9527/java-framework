package com.vesoft.nebula.jdbc.sql.write;

import com.vesoft.nebula.client.graph.data.ResultSet;
import com.vesoft.nebula.jdbc.NebulaConnection;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class NebulaSqlMutationExecutorTest {

	@Test
	public void executeMutationShouldRunPlanNgqlAndReturnDescriptor() throws Exception {
		NebulaMutationDescriptor descriptor = new NebulaMutationDescriptor(
				NebulaMutationKind.INSERT_VERTEX,
				"person",
				Collections.singletonMap("id", (Object) "p1"),
				Collections.singletonMap("name", (Object) "Tom"),
				"INSERT INTO person(id, name) VALUES ('p1', 'Tom')",
				"INSERT VERTEX person(name) VALUES \"p1\":(\"Tom\")"
		);
		NebulaMutationPlan plan = new NebulaMutationPlan(
				NebulaMutationKind.INSERT_VERTEX,
				"INSERT VERTEX person(name) VALUES \"p1\":(\"Tom\")",
				"person",
				descriptor.getKeys(),
				descriptor.getProperties(),
				"single-plan-success",
				descriptor,
				Collections.<String>emptyList()
		);
		NebulaConnection connection = mock(NebulaConnection.class);
		ResultSet nativeResultSet = mock(ResultSet.class);
		when(nativeResultSet.isSucceeded()).thenReturn(true);
		when(connection.execute(plan.getNgql())).thenReturn(nativeResultSet);

		NebulaMutationResult result = new DirectNebulaSqlMutationExecutor(connection).executeMutation(plan);

		verify(connection).execute(plan.getNgql());
		assertTrue(result.isSuccess());
		assertEquals(1, result.getAffectedRows());
		assertSame(descriptor, result.getMutationDescriptor());
	}
}
