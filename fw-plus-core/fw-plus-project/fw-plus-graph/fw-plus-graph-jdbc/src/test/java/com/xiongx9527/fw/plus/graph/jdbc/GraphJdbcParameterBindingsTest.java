package com.xiongx9527.fw.plus.graph.jdbc;

import org.junit.Test;

import java.sql.ParameterMetaData;
import java.sql.Types;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class GraphJdbcParameterBindingsTest {

	@Test
	public void bindingsShouldStoreByOneBasedParameterIndexAndClear() {
		GraphJdbcParameterBindings bindings = new GraphJdbcParameterBindings();
		bindings.bind(1, "Tom");
		bindings.bind(2, null);
		assertEquals("Tom", bindings.get(1));
		assertNull(bindings.get(2));
		assertEquals(Integer.valueOf(2), Integer.valueOf(bindings.size()));
		bindings.clear();
		assertEquals(Integer.valueOf(0), Integer.valueOf(bindings.size()));
	}

	@Test
	public void parameterMetadataShouldExposeCountAndNullableUnknown() throws Exception {
		ParameterMetaData metaData = new GraphJdbcParameterMetaData(2);
		assertEquals(2, metaData.getParameterCount());
		assertEquals(ParameterMetaData.parameterNullableUnknown, metaData.isNullable(1));
		assertEquals(Types.JAVA_OBJECT, metaData.getParameterType(1));
	}

	@Test
	public void countQuestionMarksShouldIgnoreQuotedText() {
		assertEquals(2, GraphJdbcParameterBindings.countQuestionMarks(
				"MATCH (n) WHERE n.name = ? AND n.note = '?' RETURN ?"));
	}
}
