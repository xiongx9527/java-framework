package com.xiongx9527.fw.plus.mybatis.datachange.config;

import com.xiongx9527.fw.plus.mybatis.datachange.api.TableIdentifier;
import com.xiongx9527.fw.plus.mybatis.datachange.dialect.DataChangeDialect;
import com.xiongx9527.fw.plus.mybatis.datachange.dialect.DatabaseMetadata;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class TableMonitorRegistryTest {

	@Test
	void findsOnlyConfiguredSchemaWhenTablesShareName() {
		TableMonitor publicOrders = table("public", "orders");
		TableMonitor auditOrders = table("audit", "orders");
		TableMonitorRegistry registry = new TableMonitorRegistry(Arrays.asList(publicOrders, auditOrders));

		assertThat(registry.find(new TableIdentifier(null, "PUBLIC", "ORDERS"), identityDialect()))
				.containsSame(publicOrders);
		assertThat(registry.find(new TableIdentifier(null, "audit", "orders"), identityDialect()))
				.containsSame(auditOrders);
		assertThat(registry.find(new TableIdentifier(null, "missing", "orders"), identityDialect())).isEmpty();
	}

	@Test
	void normalizesTableBeforeExactMatching() {
		TableMonitorRegistry registry = new TableMonitorRegistry(Arrays.asList(table("PUBLIC", "ORDERS")));
		DataChangeDialect upperCaseDialect = new DataChangeDialect() {
			@Override
			public boolean supports(DatabaseMetadata metadata) { return true; }

			@Override
			public TableIdentifier normalize(TableIdentifier table) {
				return new TableIdentifier(table.getCatalog(), table.getSchema(), table.getTable());
			}

			@Override
			public int getOrder() { return 0; }
		};

		assertThat(registry.find(new TableIdentifier(null, "public", "orders"), upperCaseDialect)).isPresent();
	}

	@Test
	void mightMatchUsesTableNameAsConservativeWhitelist() {
		TableMonitorRegistry registry = new TableMonitorRegistry(Arrays.asList(table("public", "orders")));

		assertThat(registry.mightMatch(new TableIdentifier(null, "audit", "orders"))).isTrue();
		assertThat(registry.mightMatch(new TableIdentifier(null, "public", "customers"))).isFalse();
	}

	private DataChangeDialect identityDialect() {
		return new DataChangeDialect() {
			@Override
			public boolean supports(DatabaseMetadata metadata) { return true; }

			@Override
			public TableIdentifier normalize(TableIdentifier table) { return table; }

			@Override
			public int getOrder() { return 0; }
		};
	}

	private TableMonitor table(String schema, String name) {
		TableMonitor table = new TableMonitor();
		table.setSchema(schema);
		table.setName(name);
		return table;
	}
}
