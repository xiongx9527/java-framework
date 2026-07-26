package com.xiongx9527.fw.plus.graph.jdbc;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.sql.rowset.CachedRowSet;
import javax.sql.rowset.RowSetMetaDataImpl;
import javax.sql.rowset.RowSetProvider;

public final class GraphJdbcRowSets {

	private GraphJdbcRowSets() {
	}

	public static ResultSet resultSet(GraphJdbcResult result) throws SQLException {
		List<String> columns = result.getColumns();
		List<Map<String, Object>> rows = result.getRows();
		if (columns.isEmpty() && !rows.isEmpty()) {
			columns.addAll(rows.get(0).keySet());
		}
		List<GraphJdbcColumn> shapedColumns = new ArrayList<GraphJdbcColumn>();
		for (String column : columns) {
			shapedColumns.add(GraphJdbcColumn.of(column, GraphJdbcDefaults.jdbcType(firstValue(rows, column))));
		}
		return shapedResultSet(shapedColumns, rows);
	}

	public static ResultSet shapedResultSet(List<GraphJdbcColumn> columns, List<Map<String, Object>> rows)
			throws SQLException {
		List<GraphJdbcColumn> shapedColumns = columns == null ? Collections.<GraphJdbcColumn>emptyList() : columns;
		List<Map<String, Object>> shapedRows = rows == null ? Collections.<Map<String, Object>>emptyList() : rows;
		CachedRowSet rowSet = RowSetProvider.newFactory().createCachedRowSet();
		RowSetMetaDataImpl metaData = new RowSetMetaDataImpl();
		Map<String, String> columnNamesByLabel = new java.util.LinkedHashMap<String, String>();
		metaData.setColumnCount(shapedColumns.size());
		for (int i = 0; i < shapedColumns.size(); i++) {
			GraphJdbcColumn column = shapedColumns.get(i);
			metaData.setColumnName(i + 1, column.getName());
			metaData.setColumnLabel(i + 1, column.getLabel());
			metaData.setColumnType(i + 1, column.getJdbcType());
			columnNamesByLabel.put(column.getLabel(), column.getName());
		}
		rowSet.setMetaData(metaData);
		for (int rowIndex = shapedRows.size() - 1; rowIndex >= 0; rowIndex--) {
			Map<String, Object> row = shapedRows.get(rowIndex);
			rowSet.moveToInsertRow();
			for (int columnIndex = 0; columnIndex < shapedColumns.size(); columnIndex++) {
				rowSet.updateObject(columnIndex + 1, row.get(shapedColumns.get(columnIndex).getName()));
			}
			rowSet.insertRow();
			rowSet.moveToCurrentRow();
		}
		rowSet.beforeFirst();
		return resultSetWithLabelSupport(rowSet, columnNamesByLabel);
	}

	public static ResultSet emptyGeneratedKeys() throws SQLException {
		final GraphJdbcLifecycle lifecycle = new GraphJdbcLifecycle();
		return (ResultSet) Proxy.newProxyInstance(GraphJdbcRowSets.class.getClassLoader(), new Class<?>[] {ResultSet.class},
				new InvocationHandler() {
					@Override
					public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
						if ("close".equals(method.getName())) {
							lifecycle.close(null);
							return null;
						}
						if ("isClosed".equals(method.getName())) {
							return Boolean.valueOf(lifecycle.isClosed());
						}
						lifecycle.checkOpen("ResultSet");
						if ("next".equals(method.getName())) {
							return Boolean.FALSE;
						}
						if ("getMetaData".equals(method.getName())) {
							return emptyMetaData();
						}
						if ("isWrapperFor".equals(method.getName())) {
							return Boolean.valueOf(GraphJdbcWrapperSupport.isWrapperFor(proxy, (Class<?>) args[0]));
						}
						if ("unwrap".equals(method.getName())) {
							return GraphJdbcWrapperSupport.unwrap(proxy, (Class<?>) args[0]);
						}
						return GraphJdbcDefaults.defaultValue(method.getReturnType());
					}
				});
	}

	private static ResultSet resultSetWithLabelSupport(final CachedRowSet rowSet,
			final Map<String, String> columnNamesByLabel) {
		return (ResultSet) Proxy.newProxyInstance(GraphJdbcRowSets.class.getClassLoader(), new Class<?>[] {ResultSet.class},
				new InvocationHandler() {
					@Override
					public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
						Object[] resolvedArgs = resolveColumnLabel(args, columnNamesByLabel);
						try {
							return method.invoke(rowSet, resolvedArgs);
						}
						catch (InvocationTargetException e) {
							throw e.getCause();
						}
					}
				});
	}

	private static Object[] resolveColumnLabel(Object[] args, Map<String, String> columnNamesByLabel) {
		if (args == null || args.length == 0 || !(args[0] instanceof String)) {
			return args;
		}
		String columnName = columnNamesByLabel.get(args[0]);
		if (columnName == null) {
			return args;
		}
		Object[] resolvedArgs = args.clone();
		resolvedArgs[0] = columnName;
		return resolvedArgs;
	}

	private static ResultSetMetaData emptyMetaData() {
		return (ResultSetMetaData) Proxy.newProxyInstance(GraphJdbcRowSets.class.getClassLoader(),
				new Class<?>[] {ResultSetMetaData.class}, new InvocationHandler() {
					@Override
					public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
						if ("getColumnCount".equals(method.getName())) {
							return Integer.valueOf(0);
						}
						if ("isWrapperFor".equals(method.getName())) {
							return Boolean.valueOf(GraphJdbcWrapperSupport.isWrapperFor(proxy, (Class<?>) args[0]));
						}
						if ("unwrap".equals(method.getName())) {
							return GraphJdbcWrapperSupport.unwrap(proxy, (Class<?>) args[0]);
						}
						return GraphJdbcDefaults.defaultValue(method.getReturnType());
					}
				});
	}

	private static Object firstValue(List<Map<String, Object>> rows, String column) {
		for (Map<String, Object> row : rows) {
			Object value = row.get(column);
			if (value != null) {
				return value;
			}
		}
		return null;
	}
}
