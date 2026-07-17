package com.vesoft.nebula.jdbc.sql;

import org.junit.Test;

import java.sql.SQLException;
import java.sql.Types;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class NebulaParameterBindingsTest {

	@Test
	public void bindingsShouldUseOneBasedIndexesAndRetainJdbcTypes() throws Exception {
		NebulaParameterBindings bindings = new NebulaParameterBindings(2);

		bindings.bind(1, "Tim", Types.VARCHAR);
		bindings.bind(2, 18, Types.INTEGER, 10);
		bindings.validateComplete();

		assertEquals("Tim", bindings.get(1).getValue());
		assertEquals(Types.INTEGER, bindings.get(2).getJdbcType());
		assertEquals(Integer.valueOf(10), bindings.get(2).getScaleOrLength());
	}

	@Test
	public void bindingsShouldRejectOutOfRangeIndexesAndMissingValues() throws Exception {
		final NebulaParameterBindings bindings = new NebulaParameterBindings(2);

		assertThrows(new SqlRunnable() {
			@Override
			public void run() throws SQLException {
				bindings.bind(0, "Tim", Types.VARCHAR);
			}
		});
		assertThrows(new SqlRunnable() {
			@Override
			public void run() throws SQLException {
				bindings.bind(3, "Tim", Types.VARCHAR);
			}
		});
		assertThrows(new SqlRunnable() {
			@Override
			public void run() throws SQLException {
				bindings.validateComplete();
			}
		});
	}

	@Test
	public void asMapAndCopyShouldProtectExecutorSnapshots() throws Exception {
		NebulaParameterBindings bindings = new NebulaParameterBindings(1);
		bindings.bind(1, "Tim", Types.VARCHAR);

		Map<Integer, NebulaParameterBinding> values = bindings.asMap();
		assertEquals("Tim", values.get(1).getValue());
		assertFalse(values.isEmpty());
		assertThrowsUnsupportedMapMutation(values);

		NebulaParameterBindings copy = bindings.copy();
		bindings.bind(1, "Bob", Types.VARCHAR);

		assertEquals("Tim", copy.get(1).getValue());
		assertEquals("Bob", bindings.get(1).getValue());
	}

	private static void assertThrows(SqlRunnable runnable) throws Exception {
		try {
			runnable.run();
			fail("Expected SQLException");
		} catch (SQLException expected) {
			assertTrue(expected.getMessage().length() > 0);
		}
	}

	private static void assertThrowsUnsupportedMapMutation(Map<Integer, NebulaParameterBinding> values) {
		try {
			values.clear();
			fail("Expected UnsupportedOperationException");
		} catch (UnsupportedOperationException expected) {
			assertTrue(values.containsKey(1));
		}
	}

	private interface SqlRunnable {
		void run() throws SQLException;
	}
}
