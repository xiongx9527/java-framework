package com.xiongx9527.fw.plus.mybatis.datachange.adapter;

import com.xiongx9527.fw.plus.mybatis.datachange.api.DataChangeException;
import com.xiongx9527.fw.plus.mybatis.datachange.api.TableIdentifier;
import com.xiongx9527.fw.plus.mybatis.datachange.config.LogicalDeleteConfiguration;
import com.xiongx9527.fw.plus.mybatis.datachange.config.TableMonitor;
import com.xiongx9527.fw.plus.mybatis.datachange.snapshot.JdbcTableMetadata;
import org.apache.ibatis.builder.StaticSqlSource;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;

class DataChangeFrameworkAdapterRegistryTest {

	@Test
	void selectsHighestOrderedSupportingAdapter() {
		DataChangeFrameworkAdapter fallback = adapter("mybatis", 0, true);
		DataChangeFrameworkAdapter plus = adapter("plus", 100, true);
		DataChangeFrameworkAdapterRegistry registry =
				new DataChangeFrameworkAdapterRegistry(Arrays.asList(fallback, plus));

		assertThat(registry.select(invocation())).isSameAs(plus);
	}

	@Test
	void ignoresHigherOrderedAdapterThatDoesNotSupportInvocation() {
		DataChangeFrameworkAdapter fallback = adapter("mybatis", 0, true);
		DataChangeFrameworkAdapter unsupported = adapter("plus", 100, false);
		DataChangeFrameworkAdapterRegistry registry =
				new DataChangeFrameworkAdapterRegistry(Arrays.asList(fallback, unsupported));

		assertThat(registry.select(invocation())).isSameAs(fallback);
	}

	@Test
	void rejectsAmbiguousAdaptersAtSameOrder() {
		DataChangeFrameworkAdapterRegistry registry = new DataChangeFrameworkAdapterRegistry(
				Arrays.asList(adapter("a", 100, true), adapter("b", 100, true)));

		assertThatThrownBy(() -> registry.select(invocation()))
				.isInstanceOf(DataChangeException.class)
				.hasMessageContaining("AMBIGUOUS_FRAMEWORK_ADAPTER");
	}

	@Test
	void nativeMybatisAdapterUsesDmlTypeAndExplicitMetadataWithoutMapperNameGuessing() {
		TableMonitor monitor = new TableMonitor();
		monitor.setSchema("PUBLIC");
		monitor.setName("ORDERS");
		monitor.setKeyColumns(Collections.singletonList("CONFIG_ID"));
		LogicalDeleteConfiguration logicalDelete = new LogicalDeleteConfiguration();
		logicalDelete.setColumn("DELETED");
		logicalDelete.setDeletedValue(1);
		logicalDelete.setNotDeletedValue(0);
		monitor.setLogicalDelete(logicalDelete);
		JdbcTableMetadata jdbcMetadata = new JdbcTableMetadata(new TableIdentifier(null, "PUBLIC", "ORDERS"),
				Collections.singletonList("JDBC_ID"), Collections.singletonList("JDBC_ID"));
		Map<String, Object> parameter = new LinkedHashMap<String, Object>();
		parameter.put("CONFIG_ID", 7L);
		DataChangeInvocation invocation = new DataChangeInvocation(mappedStatement("queryById", SqlCommandType.UPDATE),
				null, parameter, monitor, jdbcMetadata);
		MybatisDataChangeFrameworkAdapter adapter = new MybatisDataChangeFrameworkAdapter();

		assertThat(adapter.supports(invocation)).isTrue();
		FrameworkChangeMetadata metadata = adapter.resolve(invocation);
		assertThat(metadata.getTable()).isEqualTo(new TableIdentifier(null, "PUBLIC", "ORDERS"));
		assertThat(metadata.getKeyColumns()).containsExactly("CONFIG_ID");
		assertThat(metadata.getLogicalDelete().getColumn()).isEqualTo("DELETED");
		assertThat(metadata.getGeneratedKeyAccessor().readKeyValues(parameter, metadata.getKeyColumns()))
				.containsExactly(entry("CONFIG_ID", 7L));
	}

	private DataChangeInvocation invocation() {
		return new DataChangeInvocation(null, null, null);
	}

	private DataChangeFrameworkAdapter adapter(final String name, final int order, final boolean supports) {
		return new DataChangeFrameworkAdapter() {
			@Override
			public boolean supports(DataChangeInvocation invocation) {
				return supports;
			}

			@Override
			public FrameworkChangeMetadata resolve(DataChangeInvocation invocation) {
				return null;
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

	private MappedStatement mappedStatement(String id, SqlCommandType commandType) {
		Configuration configuration = new Configuration();
		return new MappedStatement.Builder(configuration, id,
				new StaticSqlSource(configuration, "update orders set total = total"), commandType).build();
	}
}
