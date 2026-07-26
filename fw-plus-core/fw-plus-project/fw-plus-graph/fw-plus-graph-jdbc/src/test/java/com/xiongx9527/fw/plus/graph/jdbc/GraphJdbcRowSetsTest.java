package com.xiongx9527.fw.plus.graph.jdbc;

import org.junit.Test;

import java.sql.ResultSet;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GraphJdbcRowSetsTest {

	@Test
	public void resultSetShouldUseColumnLabelsTypesAndRows() throws Exception {
		Map<String, Object> row = new LinkedHashMap<String, Object>();
		row.put("name", "Tim");
		row.put("age", Integer.valueOf(42));
		List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
		rows.add(row);
		ResultSet rs = GraphJdbcRowSets.resultSet(GraphJdbcResult.rows(Arrays.asList("name", "age"), rows));
		assertTrue(rs.next());
		assertEquals("Tim", rs.getString("name"));
		assertEquals(42, rs.getInt("age"));
		assertEquals(Types.INTEGER, rs.getMetaData().getColumnType(2));
		assertFalse(rs.next());
	}

	@Test
	public void emptyGeneratedKeysShouldHaveNoColumnsAndNoRows() throws Exception {
		ResultSet rs = GraphJdbcRowSets.emptyGeneratedKeys();
		assertEquals(0, rs.getMetaData().getColumnCount());
		assertFalse(rs.next());
	}

	@Test
	public void emptyGeneratedKeysShouldRejectOperationsAfterClose() throws Exception {
		ResultSet rs = GraphJdbcRowSets.emptyGeneratedKeys();
		assertFalse(rs.isClosed());
		rs.close();
		assertTrue(rs.isClosed());
		rs.close();
		try {
			rs.next();
			throw new AssertionError("next should reject a closed result set");
		}
		catch (java.sql.SQLException expected) {
			// expected
		}
		try {
			rs.getMetaData();
			throw new AssertionError("getMetaData should reject a closed result set");
		}
		catch (java.sql.SQLException expected) {
			// expected
		}
	}

	@Test
	public void resultShouldDefensivelyCopyRowsAndExposeState() {
		List<String> columns = new ArrayList<String>(Arrays.asList("name"));
		Map<String, Object> row = new LinkedHashMap<String, Object>();
		row.put("name", "Tim");
		List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
		rows.add(row);
		GraphJdbcResult result = GraphJdbcResult.rows(columns, rows);
		columns.clear();
		row.put("name", "Changed");
		assertTrue(result.hasResultSet());
		assertEquals(Arrays.asList("name"), result.getColumns());
		assertEquals("Tim", result.getRows().get(0).get("name"));
		result.getColumns().clear();
		result.getRows().get(0).put("name", "Changed again");
		assertEquals(Arrays.asList("name"), result.getColumns());
		assertEquals("Tim", result.getRows().get(0).get("name"));
		assertFalse(GraphJdbcResult.updateCount(1).hasResultSet());
		assertEquals(1, GraphJdbcResult.updateCount(1).getUpdateCount());
	}

	@Test
	public void shapedResultSetShouldUseColumnNameLabelAndType() throws Exception {
		GraphJdbcColumn column = GraphJdbcColumn.of("nodeId", "id", Types.BIGINT);
		Map<String, Object> row = new LinkedHashMap<String, Object>();
		row.put("nodeId", Long.valueOf(7L));
		ResultSet rs = GraphJdbcRowSets.shapedResultSet(Arrays.asList(column), Arrays.asList(row));
		assertEquals("nodeId", column.getName());
		assertEquals("id", column.getLabel());
		assertEquals(Types.BIGINT, column.getJdbcType());
		assertTrue(rs.next());
		assertEquals("id", rs.getMetaData().getColumnLabel(1));
		assertEquals(Types.BIGINT, rs.getMetaData().getColumnType(1));
		assertEquals(7L, rs.getLong("id"));
	}
}
