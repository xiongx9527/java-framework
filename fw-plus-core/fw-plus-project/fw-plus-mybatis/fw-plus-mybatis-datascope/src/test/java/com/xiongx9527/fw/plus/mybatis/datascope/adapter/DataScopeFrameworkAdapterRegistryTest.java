package com.xiongx9527.fw.plus.mybatis.datascope.adapter;

import org.apache.ibatis.builder.StaticSqlSource;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class DataScopeFrameworkAdapterRegistryTest {

	@Test
	void selectsHigherPrioritySpecializedAdapterBeforeGenericFallback() {
		DataScopeFrameworkAdapter specialized = new DataScopeFrameworkAdapter() {
			@Override
			public boolean supports(MappedStatement mappedStatement, Object parameterObject) {
				return true;
			}

			@Override
			public Class<?> resolveMapperType(MappedStatement mappedStatement) {
				return RegistryMapper.class;
			}

			@Override
			public int getOrder() {
				return 100;
			}
		};
		DataScopeFrameworkAdapterRegistry registry = new DataScopeFrameworkAdapterRegistry(
				Arrays.asList(specialized));
		MappedStatement statement = statement();

		assertThat(registry.select(statement, null)).isSameAs(specialized);
	}

	@Test
	void nativeMybatisAdapterRemainsAvailableWithoutSpecializedFrameworks() {
		DataScopeFrameworkAdapterRegistry registry = new DataScopeFrameworkAdapterRegistry(null);

		assertThat(registry.select(statement(), null)).isInstanceOf(MybatisDataScopeAdapter.class);
	}

	private MappedStatement statement() {
		Configuration configuration = new Configuration();
		return new MappedStatement.Builder(configuration, RegistryMapper.class.getName() + ".select",
				new StaticSqlSource(configuration, "select 1"), SqlCommandType.SELECT).build();
	}

	interface RegistryMapper {
		void select();
	}
}
