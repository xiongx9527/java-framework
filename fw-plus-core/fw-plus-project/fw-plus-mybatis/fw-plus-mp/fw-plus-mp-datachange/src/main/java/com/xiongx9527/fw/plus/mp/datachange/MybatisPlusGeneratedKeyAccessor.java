package com.xiongx9527.fw.plus.mp.datachange;

import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.xiongx9527.fw.plus.mybatis.datachange.adapter.GeneratedKeyAccessor;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class MybatisPlusGeneratedKeyAccessor implements GeneratedKeyAccessor {
	private final Map<String, String> propertyColumnMappings;

	public MybatisPlusGeneratedKeyAccessor(Map<String, String> propertyColumnMappings) {
		this.propertyColumnMappings = new LinkedHashMap<String, String>(propertyColumnMappings);
	}

	@Override
	public Map<String, Object> readKeyValues(Object parameterObject, List<String> keyColumns) {
		Map<String, Object> values = new LinkedHashMap<String, Object>();
		Object entity = unwrapEntity(parameterObject);
		if (entity == null || keyColumns == null) { return values; }
		for (String keyColumn : keyColumns) {
			String property = propertyForColumn(keyColumn);
			Value value = read(entity, property, keyColumn);
			if (value.present) { values.put(keyColumn, value.value); }
		}
		return values;
	}

	private Object unwrapEntity(Object parameterObject) {
		if (!(parameterObject instanceof Map)) { return parameterObject; }
		Map<?, ?> parameters = (Map<?, ?>) parameterObject;
		return parameters.containsKey(Constants.ENTITY) ? parameters.get(Constants.ENTITY) : parameterObject;
	}

	private String propertyForColumn(String column) {
		for (Map.Entry<String, String> mapping : propertyColumnMappings.entrySet()) {
			if (equalsIgnoreCase(mapping.getValue(), column)) { return mapping.getKey(); }
		}
		return column;
	}

	private Value read(Object entity, String property, String column) {
		if (entity instanceof Map) {
			Map<?, ?> map = (Map<?, ?>) entity;
			for (Map.Entry<?, ?> entry : map.entrySet()) {
				if (entry.getKey() instanceof String && (equalsIgnoreCase((String) entry.getKey(), property)
						|| equalsIgnoreCase((String) entry.getKey(), column))) {
					return new Value(true, entry.getValue());
				}
			}
			return Value.ABSENT;
		}
		MetaObject metaObject = SystemMetaObject.forObject(entity);
		if (metaObject.hasGetter(property)) { return new Value(true, metaObject.getValue(property)); }
		if (!property.equals(column) && metaObject.hasGetter(column)) {
			return new Value(true, metaObject.getValue(column));
		}
		return Value.ABSENT;
	}

	private boolean equalsIgnoreCase(String first, String second) {
		return first != null && second != null && first.equalsIgnoreCase(second);
	}

	private static final class Value {
		private static final Value ABSENT = new Value(false, null);
		private final boolean present;
		private final Object value;

		private Value(boolean present, Object value) {
			this.present = present;
			this.value = value;
		}
	}
}
