package com.xiongx9527.fw.plus.mf.datachange;

import com.xiongx9527.fw.plus.mybatis.datachange.adapter.GeneratedKeyAccessor;
import org.apache.ibatis.binding.MapperMethod.ParamMap;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class MybatisFlexGeneratedKeyAccessor implements GeneratedKeyAccessor {
	private static final String[] ENTITY_KEYS = {"$$entity", "entity", "et", "record", "param1", "arg0"};
	private final Map<String, String> propertyColumnMappings;

	public MybatisFlexGeneratedKeyAccessor(Map<String, String> propertyColumnMappings) {
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
		if (!(parameterObject instanceof ParamMap)) { return parameterObject; }
		Map<?, ?> parameters = (Map<?, ?>) parameterObject;
		for (String key : ENTITY_KEYS) {
			if (parameters.containsKey(key) && parameters.get(key) != null) {
				return parameters.get(key);
			}
		}
		return parameterObject;
	}

	private String propertyForColumn(String column) {
		for (Map.Entry<String, String> mapping : propertyColumnMappings.entrySet()) {
			if (equalsIgnoreCase(mapping.getValue(), column)) { return mapping.getKey(); }
		}
		return column;
	}

	private Value read(Object entity, String property, String column) {
		if (entity instanceof Map) {
			for (Map.Entry<?, ?> entry : ((Map<?, ?>) entity).entrySet()) {
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
