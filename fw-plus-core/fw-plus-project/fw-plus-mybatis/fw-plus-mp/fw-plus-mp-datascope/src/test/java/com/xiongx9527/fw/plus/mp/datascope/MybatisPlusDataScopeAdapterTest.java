package com.xiongx9527.fw.plus.mp.datascope;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.builder.StaticSqlSource;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MybatisPlusDataScopeAdapterTest {
	private final MybatisPlusDataScopeAdapter adapter = new MybatisPlusDataScopeAdapter();

	@Test
	void supportsOnlyMybatisPlusMappers() {
		assertThat(adapter.supports(statement(PlusMapper.class), null)).isTrue();
		assertThat(adapter.supports(statement(PlainMapper.class), null)).isFalse();
		assertThat(adapter.getOrder()).isEqualTo(200);
	}

	private MappedStatement statement(Class<?> mapperType) {
		Configuration configuration = new Configuration();
		return new MappedStatement.Builder(configuration, mapperType.getName() + ".select",
				new StaticSqlSource(configuration, "select 1"), SqlCommandType.SELECT).build();
	}

	interface PlusMapper extends BaseMapper<PlusEntity> {
	}

	static class PlusEntity {
	}

	interface PlainMapper {
	}
}
