package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.dbbrowser.service;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.dbbrowser.model.DbBrowserFilter;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.dbbrowser.model.DbBrowserPreviewRequest;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DbBrowserSqlBuilderTest {

	private final DbBrowserSqlBuilder sqlBuilder = new DbBrowserSqlBuilder();
	private final Set<String> allowedColumns = new HashSet<String>(Arrays.asList("id", "name"));
	private final Set<String> allowedObjects = Collections.singleton("users");

	@Test
	void shouldRejectUnknownSortColumn() throws Exception {
		DbBrowserPreviewRequest request = baseRequest();
		request.setSortColumn("unknown_column");

		assertThrows(IllegalArgumentException.class,
				() -> sqlBuilder.buildPreviewSql(connectionWithQuote("\""), request, allowedColumns, allowedObjects));
	}

	@Test
	void shouldRejectUnknownFilterColumn() throws Exception {
		DbBrowserPreviewRequest request = baseRequest();
		DbBrowserFilter filter = new DbBrowserFilter();
		filter.setColumn("unknown_column");
		filter.setOperator("eq");
		filter.setValue("value");
		request.setFilters(Collections.singletonList(filter));

		assertThrows(IllegalArgumentException.class,
				() -> sqlBuilder.buildPreviewSql(connectionWithQuote("\""), request, allowedColumns, allowedObjects));
	}

	@Test
	void shouldCapPageSizeAtMaximum() throws Exception {
		DbBrowserPreviewRequest request = baseRequest();
		request.setPageSize(DbBrowserSqlBuilder.MAX_PAGE_SIZE + 1);

		DbBrowserSqlBuilder.BuildResult result = sqlBuilder.buildPreviewSql(
				connectionWithQuote("\""), request, allowedColumns, allowedObjects);

		assertEquals(DbBrowserSqlBuilder.MAX_PAGE_SIZE, result.getPageSize());
		assertTrueSqlIsReadOnly(result.getSql());
	}

	@Test
	void shouldAcceptQualifiedObjectFromWhitelist() throws Exception {
		DbBrowserPreviewRequest request = baseRequest();
		request.setCatalog("main");
		request.setSchema("public");

		DbBrowserSqlBuilder.BuildResult result = sqlBuilder.buildPreviewSql(connectionWithQuote("\""), request,
				allowedColumns, Collections.singleton("main.public.users"));

		assertTrueSqlIsReadOnly(result.getSql());
		assertFalse(result.getSql().contains("main.public.users"));
	}

	@Test
	void shouldRejectFunctionObjectType() throws Exception {
		DbBrowserPreviewRequest request = baseRequest();
		request.setObjectType("FUNCTION");

		assertThrows(IllegalArgumentException.class,
				() -> sqlBuilder.buildPreviewSql(connectionWithQuote("\""), request, allowedColumns, allowedObjects));
	}

	@Test
	void shouldBindFilterValueAsParameterInsteadOfEmbeddingItInSql() throws Exception {
		String filterValue = "value' OR 1=1 --";
		DbBrowserPreviewRequest request = baseRequest();
		DbBrowserFilter filter = new DbBrowserFilter();
		filter.setColumn("name");
		filter.setOperator("like");
		filter.setValue(filterValue);
		request.setFilters(Collections.singletonList(filter));

		DbBrowserSqlBuilder.BuildResult result = sqlBuilder.buildPreviewSql(
				connectionWithQuote("\""), request, allowedColumns, allowedObjects);

		assertFalse(result.getSql().contains(filterValue));
		assertEquals(Collections.<Object>singletonList(filterValue), result.getParameters());
		assertTrueSqlIsReadOnly(result.getSql());
	}

	private DbBrowserPreviewRequest baseRequest() {
		DbBrowserPreviewRequest request = new DbBrowserPreviewRequest();
		request.setObjectName("users");
		request.setPage(1);
		return request;
	}

	private Connection connectionWithQuote(final String quote) {
		final DatabaseMetaData metaData = (DatabaseMetaData) Proxy.newProxyInstance(
				getClass().getClassLoader(), new Class<?>[]{DatabaseMetaData.class}, (proxy, method, args) -> {
					if ("getIdentifierQuoteString".equals(method.getName())) {
						return quote;
					}
					throw new UnsupportedOperationException(method.getName());
				});
		return (Connection) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{Connection.class},
				(proxy, method, args) -> {
					if ("getMetaData".equals(method.getName())) {
						return metaData;
					}
					throw new UnsupportedOperationException(method.getName());
				});
	}

	private void assertTrueSqlIsReadOnly(String sql) {
		assertFalse(sql.contains("INSERT"));
		assertFalse(sql.contains("UPDATE"));
		assertFalse(sql.contains("DELETE"));
		assertFalse(sql.contains("DROP"));
		assertFalse(sql.contains("CREATE"));
	}
}
