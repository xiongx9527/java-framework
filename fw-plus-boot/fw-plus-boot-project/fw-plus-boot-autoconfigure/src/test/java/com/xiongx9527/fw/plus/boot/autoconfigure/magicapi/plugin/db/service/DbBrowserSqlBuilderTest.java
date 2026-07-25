package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.db.service;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.db.model.DbBrowserFilter;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.db.model.DbBrowserPreviewRequest;
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
	private final Set<String> allowedObjects = Collections.singleton(
			DbBrowserSqlBuilder.allowedObject("TABLE", null, null, "users"));

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
	void shouldAllowInternalPrefetchWithoutChangingPageOffset() throws Exception {
		DbBrowserPreviewRequest request = baseRequest();
		request.setPage(2);
		request.setPageSize(DbBrowserSqlBuilder.MAX_PAGE_SIZE);

		DbBrowserSqlBuilder.BuildResult result = sqlBuilder.buildPreviewSql(
				connectionWithQuoteAndProductName("\"", "MySQL"), request, allowedColumns, allowedObjects,
				DbBrowserSqlBuilder.MAX_PAGE_SIZE + 1);

		assertEquals("SELECT * FROM \"users\" LIMIT 201 OFFSET 200", result.getSql());
		assertEquals(DbBrowserSqlBuilder.MAX_PAGE_SIZE + 1, result.getPageSize());
	}

	@Test
	void shouldUseLimitOffsetForMySql() throws Exception {
		DbBrowserPreviewRequest request = baseRequest();
		request.setPage(3);
		request.setPageSize(20);

		DbBrowserSqlBuilder.BuildResult result = sqlBuilder.buildPreviewSql(
				connectionWithQuoteAndProductName("\"", "MySQL"), request, allowedColumns, allowedObjects);

		assertEquals("SELECT * FROM \"users\" LIMIT 20 OFFSET 40", result.getSql());
	}

	@Test
	void shouldUseDefaultOrderForSqlServerPaginationWithoutSort() throws Exception {
		DbBrowserPreviewRequest request = baseRequest();
		request.setPage(2);
		request.setPageSize(20);

		DbBrowserSqlBuilder.BuildResult result = sqlBuilder.buildPreviewSql(
				connectionWithQuoteAndProductName("\"", "SQL Server"), request, allowedColumns, allowedObjects);

		assertEquals("SELECT * FROM \"users\" ORDER BY 1 OFFSET 20 ROWS FETCH NEXT 20 ROWS ONLY",
				result.getSql());
	}

	@Test
	void shouldRejectSqlServerOffsetFetchBeforeVersion2012() throws Exception {
		DbBrowserPreviewRequest request = baseRequest();

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> sqlBuilder.buildPreviewSql(connectionWithQuoteProductNameAndMajorVersion("\"", "Microsoft SQL Server", 10),
						request, allowedColumns, allowedObjects));

		assertEquals("Database pagination is not supported for Microsoft SQL Server 10", exception.getMessage());
	}

	@Test
	void shouldRejectOracleOffsetFetchBeforeVersion12() throws Exception {
		DbBrowserPreviewRequest request = baseRequest();

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> sqlBuilder.buildPreviewSql(connectionWithQuoteProductNameAndMajorVersion("\"", "Oracle", 11),
						request, allowedColumns, allowedObjects));

		assertEquals("Database pagination is not supported for Oracle 11", exception.getMessage());
	}

	@Test
	void shouldNotAppendDefaultOrderWhenSqlServerSortIsAllowed() throws Exception {
		DbBrowserPreviewRequest request = baseRequest();
		request.setSortColumn("name");
		request.setSortDirection("DESC");

		DbBrowserSqlBuilder.BuildResult result = sqlBuilder.buildPreviewSql(
				connectionWithQuoteAndProductName("\"", "SQL Server"), request, allowedColumns, allowedObjects);

		assertEquals("SELECT * FROM \"users\" ORDER BY \"name\" DESC OFFSET 0 ROWS FETCH NEXT 50 ROWS ONLY",
				result.getSql());
		assertFalse(result.getSql().contains("ORDER BY 1"));
	}

	@Test
	void shouldAcceptQualifiedObjectFromWhitelist() throws Exception {
		DbBrowserPreviewRequest request = baseRequest();
		request.setCatalog("main");
		request.setSchema("public");

		DbBrowserSqlBuilder.BuildResult result = sqlBuilder.buildPreviewSql(connectionWithQuote("\""), request,
				allowedColumns, Collections.singleton(DbBrowserSqlBuilder.allowedObject("VIEW", "main", "public", "users")));

		assertTrueSqlIsReadOnly(result.getSql());
		assertFalse(result.getSql().contains("main.public.users"));
	}

	@Test
	void shouldUseSchemaTableForPostgreSqlQualifiedObject() throws Exception {
		DbBrowserPreviewRequest request = baseRequest();
		request.setCatalog("orders_db");
		request.setSchema("public");

		DbBrowserSqlBuilder.BuildResult result = sqlBuilder.buildPreviewSql(
				connectionWithQuoteAndProductName("\"", "PostgreSQL"), request, allowedColumns,
				Collections.singleton(DbBrowserSqlBuilder.allowedObject("TABLE", "orders_db", "public", "users")));

		assertEquals("SELECT * FROM \"public\".\"users\" LIMIT 50 OFFSET 0", result.getSql());
	}

	@Test
	void shouldUseCatalogTableForMySqlQualifiedObject() throws Exception {
		DbBrowserPreviewRequest request = baseRequest();
		request.setCatalog("shop");
		request.setSchema("ignored_schema");

		DbBrowserSqlBuilder.BuildResult result = sqlBuilder.buildPreviewSql(
				connectionWithQuoteAndProductName("`", "MySQL"), request, allowedColumns,
				Collections.singleton(DbBrowserSqlBuilder.allowedObject("TABLE", "shop", "ignored_schema", "users")));

		assertEquals("SELECT * FROM `shop`.`users` LIMIT 50 OFFSET 0", result.getSql());
	}

	@Test
	void shouldUseCatalogSchemaTableForSqlServerQualifiedObject() throws Exception {
		DbBrowserPreviewRequest request = baseRequest();
		request.setCatalog("shop");
		request.setSchema("dbo");

		DbBrowserSqlBuilder.BuildResult result = sqlBuilder.buildPreviewSql(
				connectionWithQuoteAndProductName("\"", "Microsoft SQL Server"), request, allowedColumns,
				Collections.singleton(DbBrowserSqlBuilder.allowedObject("TABLE", "shop", "dbo", "users")));

		assertEquals("SELECT * FROM \"shop\".\"dbo\".\"users\" ORDER BY 1 OFFSET 0 ROWS FETCH NEXT 50 ROWS ONLY",
				result.getSql());
	}

	@Test
	void shouldUseSchemaTableForOracleQualifiedObject() throws Exception {
		DbBrowserPreviewRequest request = baseRequest();
		request.setCatalog("ignored_catalog");
		request.setSchema("APP");

		DbBrowserSqlBuilder.BuildResult result = sqlBuilder.buildPreviewSql(
				connectionWithQuoteProductNameAndMajorVersion("\"", "Oracle", 12), request, allowedColumns,
				Collections.singleton(DbBrowserSqlBuilder.allowedObject("TABLE", "ignored_catalog", "APP", "users")));

		assertEquals("SELECT * FROM \"APP\".\"users\" ORDER BY 1 OFFSET 0 ROWS FETCH NEXT 50 ROWS ONLY",
				result.getSql());
	}

	@Test
	void shouldRejectUnqualifiedWhitelistEntryForQualifiedRequest() throws Exception {
		DbBrowserPreviewRequest request = baseRequest();
		request.setCatalog("other");
		request.setSchema("private");

		assertThrows(IllegalArgumentException.class,
				() -> sqlBuilder.buildPreviewSql(connectionWithQuote("\""), request, allowedColumns,
						Collections.singleton("users")));
	}

	@Test
	void shouldRejectDottedSchemaWhenWhitelistUsesDifferentIdentifierSegments() throws Exception {
		DbBrowserPreviewRequest request = baseRequest();
		request.setCatalog("a");
		request.setSchema("b.c");
		request.setObjectName(null);

		assertThrows(IllegalArgumentException.class,
				() -> sqlBuilder.buildPreviewSql(connectionWithQuote("\""), request, allowedColumns,
						Collections.singleton("TABLE:a.b.c")));
	}

	@Test
	void shouldAcceptDottedSchemaOnlyWhenWhitelistUsesSameIdentifierSegment() throws Exception {
		DbBrowserPreviewRequest request = baseRequest();
		request.setCatalog("a");
		request.setSchema("b.c");

		DbBrowserSqlBuilder.BuildResult result = sqlBuilder.buildPreviewSql(connectionWithQuote("\""), request,
				allowedColumns, Collections.singleton(DbBrowserSqlBuilder.allowedObject("TABLE", "a", "b.c", "users")));

		assertEquals("SELECT * FROM \"a\".\"b.c\".\"users\" ORDER BY 1 OFFSET 0 ROWS FETCH NEXT 50 ROWS ONLY",
				result.getSql());
	}

	@Test
	void shouldEncodeNullBlankAndDottedIdentifierSegmentsWithoutCollision() {
		assertFalse(DbBrowserSqlBuilder.allowedObject("TABLE", "a", null, "users")
				.equals(DbBrowserSqlBuilder.allowedObject("TABLE", "a", "", "users")));
		assertFalse(DbBrowserSqlBuilder.allowedObject("TABLE", "a", "b.c", "users")
				.equals(DbBrowserSqlBuilder.allowedObject("TABLE", "a", "b", "c.users")));
	}

	@Test
	void shouldRejectUntypedWhitelistEntry() throws Exception {
		DbBrowserPreviewRequest request = baseRequest();

		assertThrows(IllegalArgumentException.class,
				() -> sqlBuilder.buildPreviewSql(connectionWithQuote("\""), request, allowedColumns,
						Collections.singleton("users")));
	}

	@Test
	void shouldRejectFunctionWhitelistEntryWhenClientClaimsTable() throws Exception {
		DbBrowserPreviewRequest request = baseRequest();
		request.setObjectType("TABLE");

		assertThrows(IllegalArgumentException.class,
				() -> sqlBuilder.buildPreviewSql(connectionWithQuote("\""), request, allowedColumns,
						Collections.singleton("FUNCTION:users")));
	}

	@Test
	void shouldRejectTableRequestWhenWhitelistedObjectIsView() throws Exception {
		DbBrowserPreviewRequest request = baseRequest();
		request.setObjectType("TABLE");

		assertThrows(IllegalArgumentException.class,
				() -> sqlBuilder.buildPreviewSql(connectionWithQuote("\""), request, allowedColumns,
						Collections.singleton(DbBrowserSqlBuilder.allowedObject("VIEW", null, null, "users"))));
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
		return connectionWithQuoteAndProductName(quote, "SQL Server");
	}

	private Connection connectionWithQuoteAndProductName(final String quote, final String productName) {
		return connectionWithQuoteProductNameAndMajorVersion(quote, productName, 15);
	}

	private Connection connectionWithQuoteProductNameAndMajorVersion(final String quote, final String productName,
			final int majorVersion) {
		final DatabaseMetaData metaData = (DatabaseMetaData) Proxy.newProxyInstance(
				getClass().getClassLoader(), new Class<?>[]{DatabaseMetaData.class}, (proxy, method, args) -> {
					if ("getIdentifierQuoteString".equals(method.getName())) {
						return quote;
					}
					if ("getDatabaseProductName".equals(method.getName())) {
						return productName;
					}
					if ("getDatabaseMajorVersion".equals(method.getName())) {
						return majorVersion;
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
