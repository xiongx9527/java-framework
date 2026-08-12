package com.xiongx9527.fw.plus.mf.datascope;

import com.mybatisflex.core.BaseMapper;
import com.xiongx9527.fw.plus.mybatis.datascope.adapter.DataScopeFrameworkAdapter;
import com.xiongx9527.fw.plus.mybatis.datascope.adapter.MybatisDataScopeAdapter;
import org.apache.ibatis.mapping.MappedStatement;

public final class MybatisFlexDataScopeAdapter implements DataScopeFrameworkAdapter {
	private final MybatisDataScopeAdapter fallback = new MybatisDataScopeAdapter();

	@Override
	public boolean supports(MappedStatement mappedStatement, Object parameterObject) {
		Class<?> mapperType = resolveMapperType(mappedStatement);
		return mapperType != null && BaseMapper.class.isAssignableFrom(mapperType);
	}

	@Override
	public Class<?> resolveMapperType(MappedStatement mappedStatement) {
		return fallback.resolveMapperType(mappedStatement);
	}

	@Override
	public int getOrder() {
		return 100;
	}
}
