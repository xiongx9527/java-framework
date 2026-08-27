package com.xiongx9527.fw.plus.mybatis.datachange.config;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class DataChangeConfigurationTest {

	@Test
	void defaultsToOneThousandSnapshotRowsAndAllColumns() {
		DataChangeConfiguration configuration = new DataChangeConfiguration();
		TableMonitor table = new TableMonitor();
		table.setName("sys_user");
		configuration.setTables(Collections.singletonList(table));

		assertThat(configuration.isEnabled()).isFalse();
		assertThat(configuration.getMaxSnapshotRows()).isEqualTo(1000);
		assertThat(table.getIncludeColumns()).isEmpty();
		assertThat(table.getExcludeColumns()).isEmpty();
	}

	@Test
	void rejectsDuplicateTablesRegardlessOfIdentifierCase() {
		TableMonitor first = table("catalog", "public", "sys_user");
		TableMonitor duplicate = table("CATALOG", "PUBLIC", "SYS_USER");
		DataChangeConfiguration configuration = new DataChangeConfiguration();
		configuration.setTables(Arrays.asList(first, duplicate));

		assertThatIllegalArgumentException().isThrownBy(configuration::validate)
				.withMessage("duplicate monitored table: catalog.public.sys_user");
	}

	@Test
	void rejectsZeroSnapshotRows() {
		DataChangeConfiguration configuration = new DataChangeConfiguration();
		configuration.setMaxSnapshotRows(0);

		assertThatIllegalArgumentException().isThrownBy(configuration::validate)
				.withMessage("maxSnapshotRows must be greater than zero");
	}

	@Test
	void rejectsOverlappingIncludedAndExcludedColumns() {
		TableMonitor table = table(null, null, "sys_user");
		table.setIncludeColumns(Arrays.asList("id", "name"));
		table.setExcludeColumns(Arrays.asList("NAME", "created_at"));
		DataChangeConfiguration configuration = new DataChangeConfiguration();
		configuration.setTables(Collections.singletonList(table));

		assertThatIllegalArgumentException().isThrownBy(configuration::validate)
				.withMessage("include/exclude overlap for sys_user: [name]");
	}

	private TableMonitor table(String catalog, String schema, String name) {
		TableMonitor table = new TableMonitor();
		table.setCatalog(catalog);
		table.setSchema(schema);
		table.setName(name);
		return table;
	}
}
