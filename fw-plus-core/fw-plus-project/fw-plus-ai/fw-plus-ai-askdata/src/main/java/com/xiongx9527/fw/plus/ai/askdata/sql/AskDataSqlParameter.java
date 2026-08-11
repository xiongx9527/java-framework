package com.xiongx9527.fw.plus.ai.askdata.sql;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * AskData SQL 的命名参数值对象。
 *
 * <p>参数以名称而非 JDBC 下标绑定，Core 只传递此描述，不向插件暴露裸连接或执行器。可变参数在创建和读取时
 * 都会生成受控快照；无法可靠快照的对象会被拒绝，避免插件通过共享引用篡改绑定值。</p>
 */
public final class AskDataSqlParameter {

	private final String name;
	private final Object value;

	private AskDataSqlParameter(String name, Object value) {
		if (name == null || name.trim().length() == 0) {
			throw new IllegalArgumentException("name 不能为空白");
		}
		this.name = name.trim();
		this.value = snapshot(value);
	}

	/**
	 * 创建命名参数。
	 *
	 * @param name SQL 中 {@code :name} 的名称
	 * @param value 待由实际执行适配器绑定的值，可以为 {@code null}
	 * @return 不可变命名参数
	 */
	public static AskDataSqlParameter of(String name, Object value) {
		return new AskDataSqlParameter(name, value);
	}

	public String getName() {
		return name;
	}

	public Object getValue() {
		return snapshot(value);
	}

	public boolean hasSameValue(AskDataSqlParameter other) {
		return other != null && name.equals(other.name) && deepEquals(value, other.value);
	}

	@Override
	public boolean equals(Object other) {
		return this == other || other instanceof AskDataSqlParameter && hasSameValue((AskDataSqlParameter) other);
	}

	@Override
	public int hashCode() {
		return 31 * name.hashCode() + deepHashCode(value);
	}

	private static Object snapshot(Object source) {
		if (source == null || isKnownImmutable(source)) {
			return source;
		}
		if (source instanceof Date) {
			return ((Date) source).clone();
		}
		if (source instanceof Calendar) {
			return ((Calendar) source).clone();
		}
		if (source.getClass().isArray()) {
			return snapshotArray(source);
		}
		if (source instanceof Map) {
			return snapshotMap((Map<?, ?>) source);
		}
		if (source instanceof Set) {
			return snapshotSet((Set<?>) source);
		}
		if (source instanceof Iterable) {
			return snapshotIterable((Iterable<?>) source);
		}
		throw new IllegalArgumentException("不支持不可安全快照的 SQL 参数类型: " + source.getClass().getName());
	}

	private static Object snapshotArray(Object source) {
		int length = Array.getLength(source);
		Object copy = Array.newInstance(source.getClass().getComponentType(), length);
		for (int index = 0; index < length; index++) {
			Array.set(copy, index, snapshot(Array.get(source, index)));
		}
		return copy;
	}

	private static Map<Object, Object> snapshotMap(Map<?, ?> source) {
		Map<Object, Object> copy = new LinkedHashMap<Object, Object>();
		for (Map.Entry<?, ?> entry : source.entrySet()) {
			copy.put(snapshot(entry.getKey()), snapshot(entry.getValue()));
		}
		return Collections.unmodifiableMap(copy);
	}

	private static Set<Object> snapshotSet(Set<?> source) {
		Set<Object> copy = new LinkedHashSet<Object>();
		for (Object value : source) {
			copy.add(snapshot(value));
		}
		return Collections.unmodifiableSet(copy);
	}

	private static List<Object> snapshotIterable(Iterable<?> source) {
		List<Object> copy = new ArrayList<Object>();
		for (Object value : source) {
			copy.add(snapshot(value));
		}
		return Collections.unmodifiableList(copy);
	}

	private static boolean isKnownImmutable(Object value) {
		return value instanceof String || value instanceof Boolean || value instanceof Character || value instanceof Byte
				|| value instanceof Short || value instanceof Integer || value instanceof Long || value instanceof Float
				|| value instanceof Double || value instanceof BigInteger || value instanceof BigDecimal || value instanceof UUID
				|| value instanceof Enum || value instanceof Class || value.getClass().getName().startsWith("java.time.");
	}

	private static boolean deepEquals(Object left, Object right) {
		if (left == right) {
			return true;
		}
		if (left == null || right == null || !left.getClass().equals(right.getClass())) {
			return false;
		}
		if (left.getClass().isArray()) {
			int length = Array.getLength(left);
			if (length != Array.getLength(right)) {
				return false;
			}
			for (int index = 0; index < length; index++) {
				if (!deepEquals(Array.get(left, index), Array.get(right, index))) {
					return false;
				}
			}
			return true;
		}
		if (left instanceof List) {
			List<?> leftList = (List<?>) left;
			List<?> rightList = (List<?>) right;
			if (leftList.size() != rightList.size()) {
				return false;
			}
			for (int index = 0; index < leftList.size(); index++) {
				if (!deepEquals(leftList.get(index), rightList.get(index))) {
					return false;
				}
			}
			return true;
		}
		if (left instanceof Map) {
			Map<?, ?> leftMap = (Map<?, ?>) left;
			Map<?, ?> rightMap = (Map<?, ?>) right;
			if (leftMap.size() != rightMap.size()) {
				return false;
			}
			for (Map.Entry<?, ?> entry : leftMap.entrySet()) {
				if (!rightMap.containsKey(entry.getKey())
						|| !deepEquals(entry.getValue(), rightMap.get(entry.getKey()))) {
					return false;
				}
			}
			return true;
		}
		if (left instanceof Set) {
			List<?> rightValues = new ArrayList<Object>((Set<?>) right);
			boolean[] matched = new boolean[rightValues.size()];
			for (Object leftValue : (Set<?>) left) {
				boolean found = false;
				for (int index = 0; index < rightValues.size(); index++) {
					if (!matched[index] && deepEquals(leftValue, rightValues.get(index))) {
						matched[index] = true;
						found = true;
						break;
					}
				}
				if (!found) {
					return false;
				}
			}
			return true;
		}
		return left.equals(right);
	}

	private static int deepHashCode(Object value) {
		if (value == null) {
			return 0;
		}
		if (value.getClass().isArray()) {
			int hash = 1;
			for (int index = 0; index < Array.getLength(value); index++) {
				hash = 31 * hash + deepHashCode(Array.get(value, index));
			}
			return hash;
		}
		if (value instanceof Set) {
			int hash = 0;
			for (Object item : (Set<?>) value) {
				hash += deepHashCode(item);
			}
			return hash;
		}
		if (value instanceof Iterable) {
			int hash = 1;
			for (Object item : (Iterable<?>) value) {
				hash = 31 * hash + deepHashCode(item);
			}
			return hash;
		}
		if (value instanceof Map) {
			int hash = 0;
			for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
				hash += deepHashCode(entry.getKey()) ^ deepHashCode(entry.getValue());
			}
			return hash;
		}
		return value.hashCode();
	}
}
