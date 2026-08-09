package com.xiongx9527.fw.plus.mp.encrypt;

import com.baomidou.mybatisplus.annotation.TableField;
import com.xiongx9527.fw.plus.mybatis.encrypt.metadata.FieldNameResolver;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Field;

/**
 * MyBatis-Plus 字段名解析：基于 {@code @TableField(value)} 提供属性到列名的映射。
 */
public class MybatisPlusFieldNameResolver implements FieldNameResolver {

	@Override
	public String resolveColumnName(Class<?> entityType, String propertyName) {
		Field field = findField(entityType, propertyName);
		if (field == null) {
			return null;
		}
		TableField tableField = field.getAnnotation(TableField.class);
		if (tableField != null && StringUtils.isNotEmpty(tableField.value())) {
			return tableField.value();
		}
		return null;
	}

	private static Field findField(Class<?> entityType, String propertyName) {
		Class<?> current = entityType;
		while (current != null && current != Object.class) {
			try {
				Field field = current.getDeclaredField(propertyName);
				field.setAccessible(true);
				return field;
			} catch (NoSuchFieldException ignored) {
				current = current.getSuperclass();
			}
		}
		return null;
	}
}
