package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.structured;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;

/**
 * {@link Rule} 内置工厂的取值/类型判断，包内共享。
 *
 * <p>字段定位支持点号路径（如 {@code order.buyer.name}）：无点号即顶层字段，
 * 有点号则逐层读取嵌套对象，中间层不是对象时按“缺失”处理。字段名本身不得含点号
 * （含点号的字面量键需自行写 lambda）。更复杂的语义直接写 {@link Rule} lambda。</p>
 *
 * <p>从 schema 派生规则见 {@link Rule#fromSchema(String)}：不支持 {@code anyOf} / {@code oneOf} /
 * {@code allOf}、{@code exclusiveMinimum} / {@code exclusiveMaximum} / {@code multipleOf}、
 * {@code items} 内部结构与非字符串枚举值；JSON Schema 侧只报第一个错误，全量错误清单由规则侧补齐。</p>
 */
final class Rules {

	private Rules() {
	}

	/** 已知的期望类型名（{@link Rule#type(String, String)} 用） */
	static final String[] TYPE_NAMES = {"string", "integer", "number", "boolean", "array", "object"};

	/** 类型名合法性检查：不认识就 fail-fast，避免规则静默失效 */
	static void requireTypeName(String type) {
		String target = type == null ? "" : type.trim().toLowerCase();
		for (String name : TYPE_NAMES) {
			if (name.equals(target)) {
				return;
			}
		}
		throw new IllegalArgumentException("未知的期望类型：" + type + "（可用：" + String.join(", ", TYPE_NAMES) + "）");
	}

	static boolean matchesType(Object value, String type) {
		String target = type == null ? "" : type.trim().toLowerCase();
		if ("string".equals(target)) {
			return value instanceof String;
		}
		if ("boolean".equals(target)) {
			return value instanceof Boolean;
		}
		if ("array".equals(target)) {
			return value instanceof Iterable || value instanceof Object[];
		}
		if ("object".equals(target)) {
			return value instanceof java.util.Map;
		}
		if ("integer".equals(target)) {
			return isInteger(value);
		}
		if ("number".equals(target)) {
			return value instanceof Number;
		}
		return false;
	}

	/** 整数判定：JSON 里的 3 / 3.0 / 大整数都算整数（宽松，减少无谓重试） */
	private static boolean isInteger(Object value) {
		if (value instanceof Integer || value instanceof Long || value instanceof Short
				|| value instanceof Byte || value instanceof BigInteger) {
			return true;
		}
		if (value instanceof BigDecimal) {
			return ((BigDecimal) value).scale() <= 0;
		}
		if (value instanceof Double || value instanceof Float) {
			double number = ((Number) value).doubleValue();
			return !Double.isInfinite(number) && number == Math.floor(number);
		}
		return false;
	}

	/** 按点号路径读值：无点号即顶层字段；中间层不是对象时按“缺失”处理（返回 {@code null}），不抛异常 */
	static Object read(JSONObject value, String path) {
		if (value == null || path == null) {
			return null;
		}
		String field = path.trim();
		Object current = value;
		int start = 0;
		while (current instanceof Map) {
			int dot = field.indexOf('.', start);
			String segment = dot < 0 ? field.substring(start) : field.substring(start, dot);
			current = ((Map<?, ?>) current).get(segment);
			if (dot < 0) {
				return current;
			}
			start = dot + 1;
		}
		return null;
	}

	/** 键存在性检查（值可为 {@code null}）：语义对齐 JSON Schema 的 {@code required} */
	static boolean contains(JSONObject value, String path) {
		if (value == null || path == null) {
			return false;
		}
		String field = path.trim();
		Object current = value;
		int start = 0;
		while (current instanceof Map) {
			Map<?, ?> map = (Map<?, ?>) current;
			int dot = field.indexOf('.', start);
			String segment = dot < 0 ? field.substring(start) : field.substring(start, dot);
			if (!map.containsKey(segment)) {
				return false;
			}
			if (dot < 0) {
				return true;
			}
			start = dot + 1;
			current = map.get(segment);
		}
		return false;
	}

	/** 错误信息里的取值描述：原文 + 实际类型 */
	static String describe(Object value) {
		if (value == null) {
			return "null";
		}
		return JSON.toJSONString(value) + "（" + value.getClass().getSimpleName() + "）";
	}

	/** 数值的精确十进制表示：避免用 {@code longValue()} 截断小数 */
	static BigDecimal decimal(Number value) {
		if (value instanceof BigDecimal) {
			return (BigDecimal) value;
		}
		if (value instanceof BigInteger) {
			return new BigDecimal((BigInteger) value);
		}
		if (value instanceof Double || value instanceof Float) {
			double number = value.doubleValue();
			if (Double.isNaN(number) || Double.isInfinite(number)) {
				throw new IllegalArgumentException("不支持的数值：" + value);
			}
		}
		return new BigDecimal(value.toString());
	}

	/** 数值的展示文本：去掉无意义的小数尾巴（{@code 3.0} → {@code 3}） */
	static String plain(BigDecimal value) {
		return value.stripTrailingZeros().toPlainString();
	}
}
