package com.xiongx9527.fw.plus.mybatis.datascope.adapter;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.MappedStatement;

public class MybatisDataScopeAdapter implements DataScopeFrameworkAdapter {
	@Override
	public boolean supports(MappedStatement mappedStatement, Object parameterObject) {
		return mappedStatement != null && resolveMapperType(mappedStatement) != null;
	}

	@Override
	public Class<?> resolveMapperType(MappedStatement mappedStatement) {
		return MapperTypeResolver.resolve(mappedStatement);
	}

	static final class MapperTypeResolver {
		private MapperTypeResolver() {
		}

		static Class<?> resolve(MappedStatement mappedStatement) {
			if (mappedStatement == null || mappedStatement.getId() == null) {
				return null;
			}
			String id = mappedStatement.getId();
			int separator = id.lastIndexOf('.');
			if (separator <= 0) {
				return null;
			}
			try {
				return Resources.classForName(id.substring(0, separator));
			} catch (ClassNotFoundException ignored) {
				return null;
			}
		}
	}
}
