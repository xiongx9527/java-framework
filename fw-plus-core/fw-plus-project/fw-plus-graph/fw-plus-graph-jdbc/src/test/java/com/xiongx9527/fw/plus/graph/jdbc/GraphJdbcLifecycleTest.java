package com.xiongx9527.fw.plus.graph.jdbc;

import org.junit.Test;

import java.sql.SQLException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class GraphJdbcLifecycleTest {

	@Test
	public void closeShouldRunActionOnlyOnce() throws Exception {
		final int[] count = new int[] {0};
		GraphJdbcLifecycle lifecycle = new GraphJdbcLifecycle();
		lifecycle.close(new GraphJdbcLifecycle.ThrowingCloseAction() {
			@Override
			public void close() throws SQLException {
				count[0]++;
			}
		});
		lifecycle.close(new GraphJdbcLifecycle.ThrowingCloseAction() {
			@Override
			public void close() throws SQLException {
				count[0]++;
			}
		});
		assertEquals(1, count[0]);
		assertTrue(lifecycle.isClosed());
	}

	@Test(expected = SQLException.class)
	public void checkOpenShouldRejectClosedResource() throws Exception {
		GraphJdbcLifecycle lifecycle = new GraphJdbcLifecycle();
		lifecycle.close(null);
		lifecycle.checkOpen("Statement");
	}
}
