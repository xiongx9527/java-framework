package com.xiongx9527.fw.plus.mybatis.datascope.adapter;

import org.apache.ibatis.mapping.MappedStatement;

public interface DataScopeFrameworkAdapter {
	boolean supports(MappedStatement mappedStatement, Object parameterObject);

	Class<?> resolveMapperType(MappedStatement mappedStatement);

	default int getOrder() {
		return 0;
	}
}
