package com.xiongx9527.fw.plus.mybatis.datachange.snapshot;

import com.xiongx9527.fw.plus.mybatis.datachange.adapter.FrameworkChangeMetadata;
import com.xiongx9527.fw.plus.mybatis.datachange.api.DataChangeException;
import com.xiongx9527.fw.plus.mybatis.datachange.api.TableIdentifier;
import com.xiongx9527.fw.plus.mybatis.datachange.config.TableMonitor;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RowIdentityResolverTest {
	private final RowIdentityResolver resolver = new RowIdentityResolver();

	@Test
	void resolvesExplicitThenFrameworkThenJdbcKeys() {
		TableMonitor explicit = monitor("CONFIG_ID");
		FrameworkChangeMetadata framework = framework("ORM_ID");
		JdbcTableMetadata jdbc = jdbc("JDBC_ID");

		assertThat(resolver.resolve(explicit, framework, jdbc)).containsExactly("CONFIG_ID");
		assertThat(resolver.resolve(new TableMonitor(), framework, jdbc)).containsExactly("ORM_ID");
		assertThat(resolver.resolve(new TableMonitor(), framework(), jdbc)).containsExactly("JDBC_ID");
	}

	@Test
	void rejectsMissingDuplicateAndBlankKeyColumns() {
		assertThatThrownBy(() -> resolver.resolve(new TableMonitor(), framework(), jdbc()))
				.isInstanceOf(DataChangeException.class)
				.hasMessageContaining("UNIQUE_KEY_UNAVAILABLE");
		assertThatThrownBy(() -> resolver.resolve(monitor("ID", "id"), framework(), jdbc()))
				.isInstanceOf(DataChangeException.class)
				.hasMessageContaining("INVALID_UNIQUE_KEY");
		assertThatThrownBy(() -> resolver.resolve(monitor("ID", " "), framework(), jdbc()))
				.isInstanceOf(DataChangeException.class)
				.hasMessageContaining("INVALID_UNIQUE_KEY");
	}

	private TableMonitor monitor(String... keys) {
		TableMonitor monitor = new TableMonitor();
		monitor.setKeyColumns(Arrays.asList(keys));
		return monitor;
	}

	private FrameworkChangeMetadata framework(String... keys) {
		return new FrameworkChangeMetadata(new TableIdentifier(null, null, "T"), Arrays.asList(keys), null, null);
	}

	private JdbcTableMetadata jdbc(String... keys) {
		return new JdbcTableMetadata(new TableIdentifier(null, null, "T"), Arrays.asList(keys),
				Collections.singletonList("ID"));
	}
}
