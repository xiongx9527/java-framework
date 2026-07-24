package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.dbbrowser.service;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.dbbrowser.model.DbBrowserFilter;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.dbbrowser.model.DbBrowserPreviewRequest;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class DbBrowserSqlBuilder {
	public static final int DEFAULT_PAGE_SIZE = 50;
	public static final int MAX_PAGE_SIZE = 200;
	private static final String ALLOWED_OBJECT_PREFIX = "DBB1|";

	public static String allowedObject(String type, String catalog, String schema, String objectName) {
		if (isBlank(type)) {
			throw new IllegalArgumentException("Object type is required");
		}
		if (isBlank(objectName)) {
			throw new IllegalArgumentException("Object name is required");
		}
		return ALLOWED_OBJECT_PREFIX + encodeSegment(type.toUpperCase(Locale.ROOT)) + encodeSegment(catalog)
				+ encodeSegment(schema) + encodeSegment(objectName);
	}

	public BuildResult buildPreviewSql(Connection connection, DbBrowserPreviewRequest request,
			Set<String> allowedColumns, Set<String> allowedObjects) throws SQLException {
		if (connection == null || request == null || allowedColumns == null || allowedObjects == null) {
			throw new IllegalArgumentException("Preview request and metadata whitelist are required");
		}

		String quote = getIdentifierQuote(connection);
		String databaseProductName = getDatabaseProductName(connection);
		String objectIdentifier = resolveObjectIdentifier(request, allowedObjects, quote);
		int page = request.getPage() == null || request.getPage() < 1 ? 1 : request.getPage();
		int pageSize = normalizePageSize(request.getPageSize());
		List<Object> parameters = new ArrayList<Object>();
		StringBuilder sql = new StringBuilder("SELECT * FROM ").append(objectIdentifier);

		appendFilters(sql, parameters, quote, request.getFilters(), allowedColumns);
		boolean hasSort = appendSort(sql, quote, request.getSortColumn(), request.getSortDirection(), allowedColumns);
		appendPagination(sql, databaseProductName, (long) (page - 1) * pageSize, pageSize, hasSort);
		return new BuildResult(sql.toString(), parameters, page, pageSize);
	}

	private void appendPagination(StringBuilder sql, String databaseProductName, long offset, int pageSize,
			boolean hasSort) {
		if (supportsLimitOffset(databaseProductName)) {
			sql.append(" LIMIT ").append(pageSize).append(" OFFSET ").append(offset);
			return;
		}
		if (!hasSort) {
			sql.append(" ORDER BY 1");
		}
		sql.append(" OFFSET ").append(offset).append(" ROWS FETCH NEXT ").append(pageSize).append(" ROWS ONLY");
	}

	private boolean supportsLimitOffset(String databaseProductName) {
		if (databaseProductName == null) {
			return false;
		}
		String normalizedProductName = databaseProductName.toLowerCase(Locale.ROOT);
		return normalizedProductName.contains("mysql") || normalizedProductName.contains("mariadb")
				|| normalizedProductName.contains("postgresql") || normalizedProductName.equals("h2")
				|| normalizedProductName.contains("sqlite");
	}

	private String resolveObjectIdentifier(DbBrowserPreviewRequest request, Set<String> allowedObjects, String quote) {
		String objectName = request.getObjectName();
		String trustedObjectType = resolveTrustedObjectType(request.getCatalog(), request.getSchema(), objectName,
				allowedObjects);
		if (trustedObjectType == null) {
			throw new IllegalArgumentException("Unknown database object");
		}
		validateRequestedObjectType(request.getObjectType(), trustedObjectType);
		StringBuilder identifier = new StringBuilder();
		appendQuotedIdentifierPart(identifier, request.getCatalog(), quote);
		appendQuotedIdentifierPart(identifier, request.getSchema(), quote);
		appendQuotedIdentifierPart(identifier, objectName, quote);
		return identifier.toString();
	}

	private String resolveTrustedObjectType(String catalog, String schema, String objectName, Set<String> allowedObjects) {
		if (isBlank(objectName)) {
			return null;
		}
		String tableKey = allowedObject("TABLE", catalog, schema, objectName);
		if (allowedObjects.contains(tableKey)) {
			return "TABLE";
		}
		String viewKey = allowedObject("VIEW", catalog, schema, objectName);
		if (allowedObjects.contains(viewKey)) {
			return "VIEW";
		}
		return null;
	}

	private void validateRequestedObjectType(String objectType, String trustedObjectType) {
		if (objectType == null || objectType.trim().isEmpty()) {
			return;
		}
		String normalizedType = objectType.toUpperCase(Locale.ROOT);
		if (!"TABLE".equals(normalizedType) && !"VIEW".equals(normalizedType)
				|| !trustedObjectType.equals(normalizedType)) {
			throw new IllegalArgumentException("Only table and view previews are supported");
		}
	}

	private void appendFilters(StringBuilder sql, List<Object> parameters, String quote, List<DbBrowserFilter> filters,
			Set<String> allowedColumns) {
		if (filters == null || filters.isEmpty()) {
			return;
		}
		sql.append(" WHERE ");
		for (int i = 0; i < filters.size(); i++) {
			if (i > 0) {
				sql.append(" AND ");
			}
			DbBrowserFilter filter = filters.get(i);
			if (filter == null) {
				throw new IllegalArgumentException("Filter must not be null");
			}
			String column = requireAllowed(filter.getColumn(), allowedColumns, "filter column");
			String operator = normalizeOperator(filter.getOperator());
			sql.append(quoteIdentifier(column, quote));
			if ("isnull".equals(operator)) {
				sql.append(" IS NULL");
			} else if ("isnotnull".equals(operator)) {
				sql.append(" IS NOT NULL");
			} else {
				sql.append(' ').append(toSqlOperator(operator)).append(" ?");
				parameters.add(filter.getValue());
			}
		}
	}

	private boolean appendSort(StringBuilder sql, String quote, String sortColumn, String sortDirection,
			Set<String> allowedColumns) {
		if (sortColumn == null || sortColumn.trim().isEmpty()) {
			return false;
		}
		String column = requireAllowed(sortColumn, allowedColumns, "sort column");
		String direction = sortDirection == null || sortDirection.trim().isEmpty() ? "ASC"
				: sortDirection.toUpperCase(Locale.ROOT);
		if (!"ASC".equals(direction) && !"DESC".equals(direction)) {
			throw new IllegalArgumentException("Unsupported sort direction");
		}
		sql.append(" ORDER BY ").append(quoteIdentifier(column, quote)).append(' ').append(direction);
		return true;
	}

	private String getIdentifierQuote(Connection connection) throws SQLException {
		DatabaseMetaData metaData = connection.getMetaData();
		String quote = metaData == null ? null : metaData.getIdentifierQuoteString();
		if (quote == null || quote.trim().isEmpty()) {
			throw new IllegalArgumentException("Database does not support quoted identifiers");
		}
		return quote;
	}

	private String getDatabaseProductName(Connection connection) throws SQLException {
		DatabaseMetaData metaData = connection.getMetaData();
		return metaData == null ? null : metaData.getDatabaseProductName();
	}

	private int normalizePageSize(Integer requestedPageSize) {
		if (requestedPageSize == null || requestedPageSize < 1) {
			return DEFAULT_PAGE_SIZE;
		}
		return Math.min(requestedPageSize, MAX_PAGE_SIZE);
	}

	private String requireAllowed(String value, Set<String> allowedValues, String name) {
		if (value == null || !allowedValues.contains(value)) {
			throw new IllegalArgumentException("Unknown " + name);
		}
		return value;
	}

	private static String encodeSegment(String value) {
		return value == null ? "-1:" : value.length() + ":" + value;
	}

	private static boolean isBlank(String value) {
		return value == null || value.trim().isEmpty();
	}

	private void appendQuotedIdentifierPart(StringBuilder identifier, String part, String quote) {
		if (part == null || part.trim().isEmpty()) {
			return;
		}
		if (identifier.length() > 0) {
			identifier.append('.');
		}
		identifier.append(quoteIdentifier(part, quote));
	}

	private String quoteIdentifier(String identifier, String quote) {
		return quote + identifier.replace(quote, quote + quote) + quote;
	}

	private String normalizeOperator(String operator) {
		if (operator == null) {
			throw new IllegalArgumentException("Filter operator is required");
		}
		String normalized = operator.toLowerCase(Locale.ROOT);
		if (!"eq".equals(normalized) && !"ne".equals(normalized) && !"like".equals(normalized)
				&& !"gt".equals(normalized) && !"gte".equals(normalized) && !"lt".equals(normalized)
				&& !"lte".equals(normalized) && !"isnull".equals(normalized) && !"isnotnull".equals(normalized)) {
			throw new IllegalArgumentException("Unsupported filter operator");
		}
		return normalized;
	}

	private String toSqlOperator(String operator) {
		if ("eq".equals(operator)) {
			return "=";
		}
		if ("ne".equals(operator)) {
			return "<>";
		}
		if ("like".equals(operator)) {
			return "LIKE";
		}
		if ("gt".equals(operator)) {
			return ">";
		}
		if ("gte".equals(operator)) {
			return ">=";
		}
		if ("lt".equals(operator)) {
			return "<";
		}
		return "<=";
	}

	public static class BuildResult {
		private final String sql;
		private final List<Object> parameters;
		private final int page;
		private final int pageSize;

		public BuildResult(String sql, List<Object> parameters, int page, int pageSize) {
			this.sql = sql;
			this.parameters = Collections.unmodifiableList(new ArrayList<Object>(parameters));
			this.page = page;
			this.pageSize = pageSize;
		}

		public String getSql() {
			return sql;
		}

		public List<Object> getParameters() {
			return parameters;
		}

		public int getPage() {
			return page;
		}

		public int getPageSize() {
			return pageSize;
		}
	}
}
