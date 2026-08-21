package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.vector;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

final class AskDataVectorImmutableValues {
	private AskDataVectorImmutableValues() { }

	static Map<String, Object> immutableMap(Map<String, Object> values) {
		if (values == null || values.isEmpty()) return Collections.emptyMap();
		return copyMap(values, new IdentityHashMap<Object, Boolean>());
	}

	private static Map<String, Object> copyMap(Map<String, Object> values,
			IdentityHashMap<Object, Boolean> ancestors) {
		enter(values, ancestors);
		try {
			Map<String, Object> copy = new LinkedHashMap<String, Object>();
			for (Map.Entry<String, Object> entry : values.entrySet()) {
				copy.put(entry.getKey(), copyValue(entry.getValue(), ancestors));
			}
			return Collections.unmodifiableMap(copy);
		} finally { ancestors.remove(values); }
	}

	@SuppressWarnings("unchecked")
	private static Object copyValue(Object value, IdentityHashMap<Object, Boolean> ancestors) {
		if (value == null || immutableScalar(value)) return value;
		if (value instanceof Date) return new Date(((Date) value).getTime());
		if (value instanceof Map) return copyObjectMap((Map<Object, Object>) value, ancestors);
		if (value instanceof Set) return copySet((Collection<Object>) value, ancestors);
		if (value instanceof Collection) return copyList((Collection<Object>) value, ancestors);
		if (value.getClass().isArray()) return copyArray(value, ancestors);
		throw new IllegalArgumentException("filters 包含无法安全复制的类型: " + value.getClass().getName());
	}

	private static Map<Object, Object> copyObjectMap(Map<Object, Object> values,
			IdentityHashMap<Object, Boolean> ancestors) {
		enter(values, ancestors);
		try {
			Map<Object, Object> copy = new LinkedHashMap<Object, Object>();
			for (Map.Entry<Object, Object> entry : values.entrySet()) {
				copy.put(copyValue(entry.getKey(), ancestors), copyValue(entry.getValue(), ancestors));
			}
			return Collections.unmodifiableMap(copy);
		} finally { ancestors.remove(values); }
	}

	private static List<Object> copyList(Collection<Object> values, IdentityHashMap<Object, Boolean> ancestors) {
		enter(values, ancestors);
		try {
			List<Object> copy = new ArrayList<Object>(values.size());
			for (Object value : values) copy.add(copyValue(value, ancestors));
			return Collections.unmodifiableList(copy);
		} finally { ancestors.remove(values); }
	}

	private static Set<Object> copySet(Collection<Object> values, IdentityHashMap<Object, Boolean> ancestors) {
		enter(values, ancestors);
		try {
			Set<Object> copy = new LinkedHashSet<Object>();
			for (Object value : values) copy.add(copyValue(value, ancestors));
			return Collections.unmodifiableSet(copy);
		} finally { ancestors.remove(values); }
	}

	private static Object copyArray(Object values, IdentityHashMap<Object, Boolean> ancestors) {
		enter(values, ancestors);
		try {
			int length = Array.getLength(values);
			Object copy = Array.newInstance(values.getClass().getComponentType(), length);
			for (int index = 0; index < length; index++) Array.set(copy, index, copyValue(Array.get(values, index), ancestors));
			return copy;
		} finally { ancestors.remove(values); }
	}

	private static boolean immutableScalar(Object value) {
		return value instanceof String || value instanceof Boolean || value instanceof Character
				|| value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long
				|| value instanceof Float || value instanceof Double || value instanceof BigInteger
				|| value instanceof BigDecimal || value instanceof UUID || value instanceof Enum;
	}

	private static void enter(Object value, IdentityHashMap<Object, Boolean> ancestors) {
		if (ancestors.put(value, Boolean.TRUE) != null) throw new IllegalArgumentException("filters 不能包含循环引用");
	}
}
