package com.xiongx9527.fw.plus.mf.datascope;

import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.builder.StaticSqlSource;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MybatisFlexDataScopeAdapterTest {
	private final MybatisFlexDataScopeAdapter adapter = new MybatisFlexDataScopeAdapter();

	@Test
	void supportsOnlyMybatisFlexMappers() {
		assertThat(adapter.supports(statement(FlexMapper.class), null)).isTrue();
		assertThat(adapter.supports(statement(PlainMapper.class), null)).isFalse();
		assertThat(adapter.getOrder()).isEqualTo(100);
	}

	private MappedStatement statement(Class<?> mapperType) {
		Configuration configuration = new Configuration();
		return new MappedStatement.Builder(configuration, mapperType.getName() + ".select",
				new StaticSqlSource(configuration, "select 1"), SqlCommandType.SELECT).build();
	}

	interface FlexMapper extends BaseMapper<FlexEntity> {
	}

	static class FlexEntity {
	}

	interface PlainMapper {
	}
}
