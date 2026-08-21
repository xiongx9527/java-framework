package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.db.service;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.db.model.DbBrowserColumnInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.db.model.DbBrowserDataSourceInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.db.model.DbBrowserIndexInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.db.model.DbBrowserNamespace;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.db.model.DbBrowserObjectDetail;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.db.model.DbBrowserObjectInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.db.model.DbBrowserPreviewRequest;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.db.model.DbBrowserPreviewResult;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.db.model.DbBrowserRoutineInfo;
import org.ssssssss.magicapi.datasource.model.MagicDynamicDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class DefaultDbBrowserService implements DbBrowserService {
	private static final String TABLE = "TABLE";
	private static final String VIEW = "VIEW";
	private static final String PROCEDURE = "PROCEDURE";
	private static final String FUNCTION = "FUNCTION";

	private final MagicDynamicDataSource dynamicDataSource;
	private final DbBrowserSqlBuilder sqlBuilder;

	public DefaultDbBrowserService(MagicDynamicDataSource dynamicDataSource, DbBrowserSqlBuilder sqlBuilder) {
		if (dynamicDataSource == null || sqlBuilder == null) {
			throw new IllegalArgumentException("Dynamic datasource and SQL builder are required");
		}
		this.dynamicDataSource = dynamicDataSource;
		this.sqlBuilder = sqlBuilder;
	}

	@Override
	public List<DbBrowserDataSourceInfo> listDataSources() {
		Collection<MagicDynamicDataSource.DataSourceNode> nodes = dynamicDataSource.datasourceNodes();
		List<DbBrowserDataSourceInfo> result = new ArrayList<DbBrowserDataSourceInfo>();
		for (MagicDynamicDataSource.DataSourceNode node : nodes) {
			DbBrowserDataSourceInfo info = new DbBrowserDataSourceInfo();
			info.setId(node.getId());
			info.setKey(node.getKey());
			info.setName(node.getName());
			result.add(info);
		}
		return result;
	}

	@Override
	public List<DbBrowserNamespace> listNamespaces(String key) {
		try (Connection connection = connection(key)) {
			DatabaseMetaData metaData = connection.getMetaData();
			List<DbBrowserNamespace> result = new ArrayList<DbBrowserNamespace>();
			Set<String> values = new LinkedHashSet<String>();
			try (ResultSet schemas = metaData.getSchemas()) {
				while (schemas.next()) {
					addNamespace(result, values, string(schemas, "TABLE_CATALOG"), string(schemas, "TABLE_SCHEM"));
				}
			}
			try (ResultSet catalogs = metaData.getCatalogs()) {
				while (catalogs.next()) {
					addNamespace(result, values, string(catalogs, "TABLE_CAT"), null);
				}
			}
			return result;
		} catch (SQLException e) {
			throw metadataFailure(e);
		}
	}

	@Override
	public List<DbBrowserObjectInfo> listObjects(String key, String catalog, String schema, String type) {
		try (Connection connection = connection(key)) {
			return readObjects(connection, catalog, schema, types(type));
		} catch (SQLException e) {
			throw metadataFailure(e);
		}
	}

	@Override
	public List<DbBrowserRoutineInfo> listRoutines(String key, String catalog, String schema, String type) {
		String normalizedType = normalizeRoutineType(type);
		try (Connection connection = connection(key)) {
			DatabaseMetaData metaData = connection.getMetaData();
			List<DbBrowserRoutineInfo> result = new ArrayList<DbBrowserRoutineInfo>();
			if (normalizedType == null || PROCEDURE.equals(normalizedType)) {
				try (ResultSet procedures = metaData.getProcedures(catalog, schema, null)) {
					while (procedures.next()) {
						result.add(routine(procedures, PROCEDURE, "PROCEDURE_CAT", "PROCEDURE_SCHEM", "PROCEDURE_NAME", "REMARKS"));
					}
				}
			}
			if (normalizedType == null || FUNCTION.equals(normalizedType)) {
				try (ResultSet functions = metaData.getFunctions(catalog, schema, null)) {
					while (functions.next()) {
						result.add(routine(functions, FUNCTION, "FUNCTION_CAT", "FUNCTION_SCHEM", "FUNCTION_NAME", "REMARKS"));
					}
				}
			}
			return result;
		} catch (SQLException e) {
			throw metadataFailure(e);
		}
	}

	@Override
	public DbBrowserObjectDetail getObjectDetail(String key, String catalog, String schema, String objectName, String objectType) {
		try (Connection connection = connection(key)) {
			DbBrowserObjectInfo object = findObject(connection, catalog, schema, objectName, objectType);
			DatabaseMetaData metaData = connection.getMetaData();
			DbBrowserObjectDetail detail = new DbBrowserObjectDetail();
			detail.setObject(object);
			detail.setColumns(readColumns(metaData, object));
			detail.setPrimaryKeys(readPrimaryKeys(metaData, object));
			detail.setIndexes(readIndexes(metaData, object));
			return detail;
		} catch (SQLException e) {
			throw metadataFailure(e);
		}
	}

	@Override
	public DbBrowserPreviewResult preview(DbBrowserPreviewRequest request) {
		if (request == null) {
			throw new IllegalArgumentException("Preview request is required");
		}
		try (Connection connection = connection(request.getDatasourceKey())) {
			DbBrowserObjectInfo object = findObject(connection, request.getCatalog(), request.getSchema(),
					request.getObjectName(), request.getObjectType());
			List<DbBrowserColumnInfo> columns = readColumns(connection.getMetaData(), object);
			Set<String> allowedColumns = new LinkedHashSet<String>();
			for (DbBrowserColumnInfo column : columns) {
				allowedColumns.add(column.getName());
			}
			Set<String> allowedObjects = Collections.singleton(DbBrowserSqlBuilder.allowedObject(object.getType(),
					object.getCatalog(), object.getSchema(), object.getName()));
			int pageSize = normalizePageSize(request.getPageSize());
			DbBrowserPreviewRequest queryRequest = copyRequest(request, pageSize);
			queryRequest.setCatalog(object.getCatalog());
			queryRequest.setSchema(object.getSchema());
			queryRequest.setObjectName(object.getName());
			queryRequest.setObjectType(object.getType());
			DbBrowserSqlBuilder.BuildResult buildResult = sqlBuilder.buildPreviewSql(connection, queryRequest,
					allowedColumns, allowedObjects, pageSize + 1);
			return executePreview(connection, buildResult, pageSize, request);
		} catch (SQLException e) {
			throw metadataFailure(e);
		}
	}

	private DbBrowserPreviewResult executePreview(Connection connection, DbBrowserSqlBuilder.BuildResult buildResult,
			int pageSize, DbBrowserPreviewRequest request) throws SQLException {
		List<String> columns = new ArrayList<String>();
		List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
		try (PreparedStatement statement = connection.prepareStatement(buildResult.getSql())) {
			for (int i = 0; i < buildResult.getParameters().size(); i++) {
				statement.setObject(i + 1, buildResult.getParameters().get(i));
			}
			try (ResultSet resultSet = statement.executeQuery()) {
				ResultSetMetaData metaData = resultSet.getMetaData();
				for (int i = 1; i <= metaData.getColumnCount(); i++) {
					columns.add(metaData.getColumnLabel(i));
				}
				while (resultSet.next()) {
					Map<String, Object> row = new LinkedHashMap<String, Object>();
					for (int i = 1; i <= columns.size(); i++) {
						row.put(columns.get(i - 1), resultSet.getObject(i));
					}
					rows.add(row);
				}
			}
		}
		boolean hasMore = rows.size() > pageSize;
		if (hasMore) {
			rows.remove(rows.size() - 1);
		}
		DbBrowserPreviewResult result = new DbBrowserPreviewResult();
		result.setColumns(columns);
		result.setRows(rows);
		result.setPage(buildResult.getPage());
		result.setPageSize(pageSize);
		result.setHasMore(hasMore);
		result.setAppliedSort(request.getSortColumn());
		result.setAppliedFilters(request.getFilters());
		return result;
	}

	private List<DbBrowserObjectInfo> readObjects(Connection connection, String catalog, String schema, String[] types) throws SQLException {
		List<DbBrowserObjectInfo> result = new ArrayList<DbBrowserObjectInfo>();
		try (ResultSet objects = connection.getMetaData().getTables(catalog, schema, null, types)) {
			while (objects.next()) {
				DbBrowserObjectInfo object = new DbBrowserObjectInfo();
				object.setCatalog(string(objects, "TABLE_CAT"));
				object.setSchema(string(objects, "TABLE_SCHEM"));
				object.setName(string(objects, "TABLE_NAME"));
				object.setType(browserObjectType(string(objects, "TABLE_TYPE")));
				object.setRemarks(string(objects, "REMARKS"));
				result.add(object);
			}
		}
		return result;
	}

	private DbBrowserObjectInfo findObject(Connection connection, String catalog, String schema, String name, String requestedType) throws SQLException {
		if (isBlank(name)) {
			throw new IllegalArgumentException("Object name is required");
		}
		String type = normalizeObjectType(requestedType);
		for (DbBrowserObjectInfo object : readObjects(connection, catalog, schema, types(type))) {
			if (name.equals(object.getName()) && matchesRequested(catalog, object.getCatalog())
					&& matchesRequested(schema, object.getSchema())) {
				return object;
			}
		}
		throw new IllegalArgumentException("Unknown database object");
	}

	private List<DbBrowserColumnInfo> readColumns(DatabaseMetaData metaData, DbBrowserObjectInfo object) throws SQLException {
		List<DbBrowserColumnInfo> result = new ArrayList<DbBrowserColumnInfo>();
		try (ResultSet columns = metaData.getColumns(object.getCatalog(), object.getSchema(), object.getName(), null)) {
			while (columns.next()) {
				DbBrowserColumnInfo column = new DbBrowserColumnInfo();
				column.setName(string(columns, "COLUMN_NAME"));
				column.setJdbcType(integer(columns, "DATA_TYPE"));
				column.setTypeName(string(columns, "TYPE_NAME"));
				column.setSize(integer(columns, "COLUMN_SIZE"));
				column.setDecimalDigits(integer(columns, "DECIMAL_DIGITS"));
				column.setNullable(integer(columns, "NULLABLE"));
				column.setDefaultValue(string(columns, "COLUMN_DEF"));
				column.setRemarks(string(columns, "REMARKS"));
				column.setOrdinalPosition(integer(columns, "ORDINAL_POSITION"));
				result.add(column);
			}
		}
		return result;
	}

	private List<String> readPrimaryKeys(DatabaseMetaData metaData, DbBrowserObjectInfo object) throws SQLException {
		List<String> result = new ArrayList<String>();
		try (ResultSet keys = metaData.getPrimaryKeys(object.getCatalog(), object.getSchema(), object.getName())) {
			while (keys.next()) {
				result.add(string(keys, "COLUMN_NAME"));
			}
		}
		return result;
	}

	private List<DbBrowserIndexInfo> readIndexes(DatabaseMetaData metaData, DbBrowserObjectInfo object) throws SQLException {
		List<DbBrowserIndexInfo> result = new ArrayList<DbBrowserIndexInfo>();
		try (ResultSet indexes = metaData.getIndexInfo(object.getCatalog(), object.getSchema(), object.getName(), false, false)) {
			while (indexes.next()) {
				String name = string(indexes, "INDEX_NAME");
				if (name == null) {
					continue;
				}
				DbBrowserIndexInfo index = new DbBrowserIndexInfo();
				index.setName(name);
				index.setNonUnique(bool(indexes, "NON_UNIQUE"));
				index.setColumnName(string(indexes, "COLUMN_NAME"));
				index.setOrdinalPosition(integer(indexes, "ORDINAL_POSITION"));
				index.setAscOrDesc(string(indexes, "ASC_OR_DESC"));
				index.setType(shortValue(indexes, "TYPE"));
				result.add(index);
			}
		}
		return result;
	}

	private MagicDynamicDataSource.DataSourceNode dataSourceNode(String key) {
		for (MagicDynamicDataSource.DataSourceNode node : dynamicDataSource.datasourceNodes()) {
			if (equalsNullable(key, node.getKey())) {
				return node;
			}
		}
		throw new IllegalArgumentException("Unknown datasource");
	}

	private Connection connection(String key) throws SQLException {
		DataSource dataSource = dataSourceNode(key).getDataSource();
		if (dataSource == null) {
			throw new IllegalArgumentException("Unknown datasource");
		}
		return dataSource.getConnection();
	}

	private String[] types(String type) {
		String normalizedType = normalizeObjectType(type);
		return normalizedType == null ? new String[] { TABLE, VIEW } : new String[] { normalizedType };
	}

	private String normalizeObjectType(String type) {
		if (isBlank(type)) {
			return null;
		}
		String normalized = type.toUpperCase(Locale.ROOT);
		if (!TABLE.equals(normalized) && !VIEW.equals(normalized)) {
			throw new IllegalArgumentException("Only table and view objects are supported");
		}
		return normalized;
	}

	private String browserObjectType(String type) {
		return type != null && type.toUpperCase(Locale.ROOT).contains(VIEW) ? VIEW : TABLE;
	}

	private String normalizeRoutineType(String type) {
		if (isBlank(type)) {
			return null;
		}
		String normalized = type.toUpperCase(Locale.ROOT);
		if (!PROCEDURE.equals(normalized) && !FUNCTION.equals(normalized)) {
			throw new IllegalArgumentException("Only procedure and function routines are supported");
		}
		return normalized;
	}

	private DbBrowserRoutineInfo routine(ResultSet resultSet, String type, String catalogColumn, String schemaColumn,
			String nameColumn, String remarksColumn) throws SQLException {
		DbBrowserRoutineInfo routine = new DbBrowserRoutineInfo();
		routine.setCatalog(string(resultSet, catalogColumn));
		routine.setSchema(string(resultSet, schemaColumn));
		routine.setName(string(resultSet, nameColumn));
		routine.setType(type);
		routine.setRemarks(string(resultSet, remarksColumn));
		return routine;
	}

	private DbBrowserPreviewRequest copyRequest(DbBrowserPreviewRequest request, int pageSize) {
		DbBrowserPreviewRequest copy = new DbBrowserPreviewRequest();
		copy.setDatasourceKey(request.getDatasourceKey());
		copy.setCatalog(request.getCatalog());
		copy.setSchema(request.getSchema());
		copy.setObjectName(request.getObjectName());
		copy.setObjectType(request.getObjectType());
		copy.setPage(request.getPage());
		copy.setPageSize(pageSize);
		copy.setSortColumn(request.getSortColumn());
		copy.setSortDirection(request.getSortDirection());
		copy.setFilters(request.getFilters());
		return copy;
	}

	private int normalizePageSize(Integer requestedPageSize) {
		if (requestedPageSize == null || requestedPageSize < 1) {
			return DbBrowserSqlBuilder.DEFAULT_PAGE_SIZE;
		}
		return Math.min(requestedPageSize, DbBrowserSqlBuilder.MAX_PAGE_SIZE);
	}

	private void addNamespace(List<DbBrowserNamespace> result, Set<String> values, String catalog, String schema) {
		String value = valueKey(catalog) + valueKey(schema);
		if (values.add(value)) {
			DbBrowserNamespace namespace = new DbBrowserNamespace();
			namespace.setCatalog(catalog);
			namespace.setSchema(schema);
			result.add(namespace);
		}
	}

	private String valueKey(String value) {
		return value == null ? "-1:" : value.length() + ":" + value;
	}

	private String string(ResultSet resultSet, String column) throws SQLException { return resultSet.getString(column); }
	private Integer integer(ResultSet resultSet, String column) throws SQLException { int value = resultSet.getInt(column); return resultSet.wasNull() ? null : value; }
	private Short shortValue(ResultSet resultSet, String column) throws SQLException { short value = resultSet.getShort(column); return resultSet.wasNull() ? null : value; }
	private Boolean bool(ResultSet resultSet, String column) throws SQLException { boolean value = resultSet.getBoolean(column); return resultSet.wasNull() ? null : value; }
	private boolean equalsNullable(String first, String second) { return first == null ? second == null : first.equals(second); }
	private boolean matchesRequested(String requested, String actual) { return requested == null || requested.equals(actual); }
	private boolean isBlank(String value) { return value == null || value.trim().isEmpty(); }
	private IllegalStateException metadataFailure(SQLException e) { return new IllegalStateException("Unable to read database metadata", e); }
}
