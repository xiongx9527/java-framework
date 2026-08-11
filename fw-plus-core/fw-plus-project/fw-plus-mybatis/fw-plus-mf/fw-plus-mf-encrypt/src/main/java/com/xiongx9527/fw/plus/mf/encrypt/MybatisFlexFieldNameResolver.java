package com.xiongx9527.fw.plus.mf.encrypt;

import com.mybatisflex.core.table.TableInfo;
import com.mybatisflex.core.table.TableInfoFactory;
import com.xiongx9527.fw.plus.mybatis.encrypt.metadata.FieldNameResolver;

/**
 * MyBatis-Flex 字段名解析：基于 Flex 实体元数据（{@code @Column} 与默认列名规则）提供
 * 属性到列名的映射。
 */
public class MybatisFlexFieldNameResolver implements FieldNameResolver {

	@Override
	public String resolveColumnName(Class<?> entityType, String propertyName) {
		TableInfo tableInfo = TableInfoFactory.ofEntityClass(entityType);
		if (tableInfo == null) {
			return null;
		}
		return tableInfo.getPropertyColumnMapping().get(propertyName);
	}
}
