package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.dbbrowser.service;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.dbbrowser.model.DbBrowserDataSourceInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.dbbrowser.model.DbBrowserFilter;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.dbbrowser.model.DbBrowserObjectDetail;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.dbbrowser.model.DbBrowserObjectInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.dbbrowser.model.DbBrowserPreviewRequest;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.dbbrowser.model.DbBrowserPreviewResult;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ssssssss.magicapi.datasource.model.MagicDynamicDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultDbBrowserServiceTest {

	private DefaultDbBrowserService service;
	private JdbcDataSource dataSource;

	@BeforeEach
	void setUp() throws Exception {
		dataSource = new JdbcDataSource();
		dataSource.setURL("jdbc:h2:mem:dbbrowser;DB_CLOSE_DELAY=-1");
		dataSource.setUser("sa");
		try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
			statement.execute("DROP ALL OBJECTS");
			statement.execute("CREATE TABLE orders (id BIGINT PRIMARY KEY, customer_name VARCHAR(64) NOT NULL, amount DECIMAL(10,2), note VARCHAR(128))");
			statement.execute("CREATE INDEX idx_orders_customer ON orders(customer_name)");
			statement.execute("CREATE VIEW order_names AS SELECT id, customer_name FROM orders");
			statement.execute("INSERT INTO orders (id, customer_name, amount, note) VALUES (1, 'Alice', 12.50, NULL)");
			statement.execute("INSERT INTO orders (id, customer_name, amount, note) VALUES (2, 'Bob', 20.00, 'priority')");
			statement.execute("INSERT INTO orders (id, customer_name, amount, note) VALUES (3, 'Cara', 30.00, NULL)");
		}
		MagicDynamicDataSource dynamicDataSource = new MagicDynamicDataSource();
		dynamicDataSource.put("orders-id", "orders", "Orders", dataSource, 1000);
		service = new DefaultDbBrowserService(dynamicDataSource, new DbBrowserSqlBuilder());
	}

	@Test
	void shouldListRegisteredDataSourceAndMetadata() {
		List<DbBrowserDataSourceInfo> dataSources = service.listDataSources();

		assertThat(dataSources).extracting(DbBrowserDataSourceInfo::getKey).containsExactly("orders");
		assertThat(service.listNamespaces("orders"))
				.anySatisfy(namespace -> assertThat(namespace.getSchema()).isEqualTo("PUBLIC"));
		assertThat(service.listObjects("orders", null, "PUBLIC", null))
				.extracting(DbBrowserObjectInfo::getName)
				.contains("ORDERS", "ORDER_NAMES");
	}

	@Test
	void shouldReturnColumnsPrimaryKeysAndIndexesForTable() {
		DbBrowserObjectDetail detail = service.getObjectDetail("orders", null, "PUBLIC", "ORDERS", "TABLE");

		assertThat(detail.getColumns()).extracting(column -> column.getName()).contains("ID", "CUSTOMER_NAME", "AMOUNT", "NOTE");
		assertThat(detail.getPrimaryKeys()).contains("ID");
		assertThat(detail.getIndexes()).anySatisfy(index -> {
			assertThat(index.getName()).isEqualTo("IDX_ORDERS_CUSTOMER");
			assertThat(index.getColumnName()).isEqualTo("CUSTOMER_NAME");
		});
	}

	@Test
	void shouldPreviewPagesAndBindFilters() {
		DbBrowserPreviewRequest request = new DbBrowserPreviewRequest();
		request.setDatasourceKey("orders");
		request.setSchema("PUBLIC");
		request.setObjectName("ORDERS");
		request.setObjectType("TABLE");
		request.setPage(1);
		request.setPageSize(2);
		request.setSortColumn("ID");
		request.setSortDirection("ASC");
		DbBrowserFilter filter = new DbBrowserFilter();
		filter.setColumn("CUSTOMER_NAME");
		filter.setOperator("ne");
		filter.setValue("Nobody");
		request.setFilters(java.util.Collections.singletonList(filter));

		DbBrowserPreviewResult result = service.preview(request);

		assertThat(result.getColumns()).contains("ID", "CUSTOMER_NAME");
		assertThat(result.getRows()).hasSize(2);
		assertThat(result.isHasMore()).isTrue();
		assertThat(result.getRows().get(0)).containsEntry("ID", 1L);
	}

	@Test
	void shouldReportMoreRowsWhenPreviewUsesMaximumPageSize() throws Exception {
		try (Connection connection = dataSource.getConnection();
			 PreparedStatement statement = connection.prepareStatement(
					"INSERT INTO orders (id, customer_name, amount, note) VALUES (?, ?, ?, ?)")) {
			for (int id = 4; id <= DbBrowserSqlBuilder.MAX_PAGE_SIZE + 1; id++) {
				statement.setLong(1, id);
				statement.setString(2, "Customer " + id);
				statement.setBigDecimal(3, new java.math.BigDecimal("1.00"));
				statement.setString(4, null);
				statement.addBatch();
			}
			statement.executeBatch();
		}

		DbBrowserPreviewRequest request = previewRequest();
		request.setPageSize(DbBrowserSqlBuilder.MAX_PAGE_SIZE);
		request.setSortColumn("ID");
		request.setSortDirection("ASC");

		DbBrowserPreviewResult result = service.preview(request);

		assertThat(result.getRows()).hasSize(DbBrowserSqlBuilder.MAX_PAGE_SIZE);
		assertThat(result.isHasMore()).isTrue();
	}

	@Test
	void shouldBindQuotedSqlFragmentAsFilterValue() {
		DbBrowserPreviewRequest request = previewRequest();
		DbBrowserFilter filter = new DbBrowserFilter();
		filter.setColumn("CUSTOMER_NAME");
		filter.setOperator("eq");
		filter.setValue("Alice' OR 1=1 --");
		request.setFilters(java.util.Collections.singletonList(filter));

		DbBrowserPreviewResult result = service.preview(request);

		assertThat(result.getRows()).isEmpty();
	}

	@Test
	void shouldListRoutineMetadataWithoutInvocation() {
		assertThat(service.listRoutines("orders", null, "PUBLIC", null)).isNotNull();
	}

	@Test
	void shouldRejectUnknownDatasourceObjectAndColumn() {
		DbBrowserPreviewRequest request = new DbBrowserPreviewRequest();
		request.setDatasourceKey("missing");
		assertThatThrownBy(() -> service.preview(request)).isInstanceOf(IllegalArgumentException.class);

		request.setDatasourceKey("orders");
		request.setSchema("PUBLIC");
		request.setObjectName("unknown");
		request.setObjectType("TABLE");
		assertThatThrownBy(() -> service.preview(request)).isInstanceOf(IllegalArgumentException.class);

		request.setObjectName("ORDERS");
		request.setSortColumn("unknown_column");
		assertThatThrownBy(() -> service.preview(request)).isInstanceOf(IllegalArgumentException.class);
	}

	private DbBrowserPreviewRequest previewRequest() {
		DbBrowserPreviewRequest request = new DbBrowserPreviewRequest();
		request.setDatasourceKey("orders");
		request.setSchema("PUBLIC");
		request.setObjectName("ORDERS");
		request.setObjectType("TABLE");
		request.setPage(1);
		return request;
	}
}
