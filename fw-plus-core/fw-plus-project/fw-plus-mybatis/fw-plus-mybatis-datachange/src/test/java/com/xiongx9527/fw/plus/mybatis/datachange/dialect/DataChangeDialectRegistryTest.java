package com.xiongx9527.fw.plus.mybatis.datachange.dialect;

import com.xiongx9527.fw.plus.mybatis.datachange.api.DataChangeException;
import com.xiongx9527.fw.plus.mybatis.datachange.api.TableIdentifier;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DataChangeDialectRegistryTest {

	@Test
	void selectsHighestOrderedSupportingDialect() throws Exception {
		DataChangeDialect generic = dialect("generic", 0, true);
		DataChangeDialect h2 = dialect("h2", 100, true);
		DataChangeDialectRegistry registry = new DataChangeDialectRegistry(Arrays.asList(generic, h2));

		assertThat(registry.select(connection("H2", 2, 2))).isSameAs(h2);
	}

	@Test
	void rejectsAmbiguousDialectsAtSameOrder() throws Exception {
		DataChangeDialectRegistry registry = new DataChangeDialectRegistry(
				Arrays.asList(dialect("a", 100, true), dialect("b", 100, true)));

		assertThatThrownBy(() -> registry.select(connection("H2", 2, 2)))
				.isInstanceOf(DataChangeException.class)
				.hasMessageContaining("AMBIGUOUS_DIALECT");
	}

	@Test
	void cachesSelectionByDatabaseProductAndVersion() throws Exception {
		final AtomicInteger supportChecks = new AtomicInteger();
		DataChangeDialect dialect = new DataChangeDialect() {
			@Override
			public boolean supports(DatabaseMetadata metadata) {
				supportChecks.incrementAndGet();
				return true;
			}

			@Override
			public TableIdentifier normalize(TableIdentifier table) { return table; }

			@Override
			public int getOrder() { return 100; }
		};
		DataChangeDialectRegistry registry = new DataChangeDialectRegistry(Arrays.asList(dialect));

		assertThat(registry.select(connection("H2", 2, 2))).isSameAs(dialect);
		assertThat(registry.select(connection("H2", 2, 2))).isSameAs(dialect);
		assertThat(supportChecks).hasValue(1);
	}

	private Connection connection(String productName, int majorVersion, int minorVersion) throws Exception {
		Connection connection = Mockito.mock(Connection.class);
		DatabaseMetaData metadata = Mockito.mock(DatabaseMetaData.class);
		Mockito.when(connection.getMetaData()).thenReturn(metadata);
		Mockito.when(metadata.getDatabaseProductName()).thenReturn(productName);
		Mockito.when(metadata.getDatabaseMajorVersion()).thenReturn(majorVersion);
		Mockito.when(metadata.getDatabaseMinorVersion()).thenReturn(minorVersion);
		return connection;
	}

	private DataChangeDialect dialect(final String name, final int order, final boolean supports) {
		return new DataChangeDialect() {
			@Override
			public boolean supports(DatabaseMetadata metadata) {
				return supports;
			}

			@Override
			public TableIdentifier normalize(TableIdentifier table) {
				return table;
			}

			@Override
			public int getOrder() {
				return order;
			}

			@Override
			public String toString() {
				return name;
			}
		};
	}
}
