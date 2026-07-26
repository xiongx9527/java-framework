package com.xiongx9527.fw.plus.graph.jdbc;

import org.junit.Test;

import java.sql.SQLFeatureNotSupportedException;
import java.sql.Types;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class GraphJdbcDefaultsTest {

	@Test
	public void defaultValueShouldMatchJdbcProxyPrimitiveReturnTypes() {
		assertNull(GraphJdbcDefaults.defaultValue(Void.TYPE));
		assertEquals(Boolean.FALSE, GraphJdbcDefaults.defaultValue(Boolean.TYPE));
		assertEquals(Integer.valueOf(0), GraphJdbcDefaults.defaultValue(Integer.TYPE));
		assertEquals(Long.valueOf(0L), GraphJdbcDefaults.defaultValue(Long.TYPE));
		assertEquals(Float.valueOf(0F), GraphJdbcDefaults.defaultValue(Float.TYPE));
		assertEquals(Double.valueOf(0D), GraphJdbcDefaults.defaultValue(Double.TYPE));
		assertEquals(Short.valueOf((short) 0), GraphJdbcDefaults.defaultValue(Short.TYPE));
		assertEquals(Byte.valueOf((byte) 0), GraphJdbcDefaults.defaultValue(Byte.TYPE));
		assertNull(GraphJdbcDefaults.defaultValue(String.class));
	}

	@Test
	public void wrapperSupportShouldExposeOnlyTargetAssignableTypes() throws Exception {
		Object target = "value";
		assertTrue(GraphJdbcWrapperSupport.isWrapperFor(target, String.class));
		assertEquals("value", GraphJdbcWrapperSupport.unwrap(target, String.class));
		assertFalse(GraphJdbcWrapperSupport.isWrapperFor(target, java.sql.Connection.class));
	}

	@Test
	public void defaultsShouldProvideJdbcTypeFallbacksAndUnsupportedException() {
		assertTrue(GraphJdbcDefaults.unsupportedOperation() instanceof SQLFeatureNotSupportedException);
		assertEquals(Types.INTEGER, GraphJdbcDefaults.jdbcType(Integer.valueOf(1)));
		assertEquals(Types.BIGINT, GraphJdbcDefaults.jdbcType(Long.valueOf(1L)));
		assertEquals(Types.DOUBLE, GraphJdbcDefaults.jdbcType(Double.valueOf(1D)));
		assertEquals(Types.BOOLEAN, GraphJdbcDefaults.jdbcType(Boolean.TRUE));
		assertEquals(Types.VARCHAR, GraphJdbcDefaults.jdbcType("value"));
	}
}
