package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.structured;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.PatternSyntaxException;

/**
 * JSON Schema → {@link Rule} 的派生器，包内共享。
 *
 * <p>递归读取 {@code properties} / {@code required} / {@code type} / {@code enum} / {@code minimum} /
 * {@code maximum} / {@code pattern}，为嵌套字段生成点号路径规则，错误文本与内置工厂保持同一套格式
 * （因此可直接回喂给模型）。</p>
 *
 * <p>能力边界（不派生的约束，需手写规则补齐）：{@code anyOf} / {@code oneOf} / {@code allOf}、
 * {@code exclusiveMinimum} / {@code exclusiveMaximum} / {@code multipleOf}、{@code items} 内部结构、
 * 非字符串枚举值、{@code object} 类型本身（嵌套存在性由 {@code required} 承担）。</p>
 *
 * <p>宽松忽略（不派生也不报错，宁可不校验也不因写法怪就整体失败）：非数值的
 * {@code minimum} / {@code maximum}、非对象的属性 schema（如 {@code {"a":123}}）、
 * 不认识的 {@code type} 名。与之相对，结构非法（{@code properties} 不是对象、
 * {@code required} 不是数组、{@code pattern} 不是合法正则）会 fail-fast。</p>
 */
final class SchemaRules {

	private SchemaRules() {
	}

	/** 从 JSON Schema 文本派生规则；schema 不合法时 fail-fast */
	static Rule[] from(String jsonSchemaText) {
		if (jsonSchemaText == null || jsonSchemaText.trim().isEmpty()) {
			throw new IllegalArgumentException("jsonSchema 不能为空");
		}
		Object parsed;
		try {
			parsed = JSON.parse(jsonSchemaText.trim());
		} catch (RuntimeException e) {
			throw new IllegalArgumentException("jsonSchema 不是合法的 JSON：" + e.getMessage(), e);
		}
		if (!(parsed instanceof JSONObject)) {
			throw new IllegalArgumentException("jsonSchema 不是合法的 JSON Schema：顶层必须是 JSON 对象");
		}
		Set<Rule> rules = new LinkedHashSet<Rule>();
		try {
			collect((JSONObject) parsed, "", rules);
		} catch (PatternSyntaxException e) {
			throw new IllegalArgumentException("jsonSchema 的 pattern 不是合法正则：" + e.getDescription()
					+ "（" + e.getPattern() + "）", e);
		} catch (IllegalArgumentException e) {
			// 我们自己抛出的结构错误，保留原始信息
			throw e;
		} catch (RuntimeException e) {
			throw new IllegalArgumentException("jsonSchema 结构非法：" + e.getMessage(), e);
		}
		return rules.toArray(new Rule[0]);
	}

	private static void collect(JSONObject schema, String prefix, Set<Rule> rules) {
		Object rawProperties = schema.get("properties");
		if (rawProperties != null) {
			if (!(rawProperties instanceof JSONObject)) {
				throw new IllegalArgumentException("jsonSchema 结构非法：properties 必须是对象");
			}
			JSONObject properties = (JSONObject) rawProperties;
			for (Map.Entry<String, Object> entry : properties.entrySet()) {
				Object raw = entry.getValue();
				if (!(raw instanceof JSONObject)) {
					continue;
				}
				String path = join(prefix, entry.getKey());
				JSONObject field = (JSONObject) raw;
				append(field, path, rules);
				collect(field, path, rules);
			}
		}
		Object rawRequired = schema.get("required");
		if (rawRequired != null) {
			if (!(rawRequired instanceof JSONArray)) {
				throw new IllegalArgumentException("jsonSchema 结构非法：required 必须是数组");
			}
			JSONArray required = (JSONArray) rawRequired;
			// 声明顺序去重：同一字段被重复声明时只派生一条规则
			Set<String> names = new LinkedHashSet<String>();
			for (int i = 0; i < required.size(); i++) {
				String name = required.getString(i);
				if (name != null && !name.trim().isEmpty()) {
					names.add(join(prefix, name));
				}
			}
			for (String name : names) {
				// 键存在性（值可为 null）：对齐 JSON Schema 的 required 语义，避免 nullable 字段假阳性
				rules.add(Rule.present(name));
			}
		}
	}

	private static void append(JSONObject field, String path, Set<Rule> rules) {
		String type = typeName(field.get("type"));
		if (type != null && !"object".equals(type)) {
			rules.add(Rule.type(path, type));
		}
		JSONArray allowed = field.getJSONArray("enum");
		if (allowed != null && allStrings(allowed)) {
			List<String> values = new ArrayList<String>();
			for (int i = 0; i < allowed.size(); i++) {
				values.add(allowed.getString(i));
			}
			rules.add(Rule.enumOf(path, values.toArray(new String[0])));
		}
		Number minimum = numeric(field.get("minimum"));
		Number maximum = numeric(field.get("maximum"));
		if (minimum != null || maximum != null) {
			rules.add(bounds(path, minimum, maximum));
		}
		String pattern = field.getString("pattern");
		if (pattern != null && !pattern.trim().isEmpty()) {
			rules.add(Rule.regex(path, pattern));
		}
	}

	/**
	 * 数值边界：用 {@link BigDecimal} 精确比较，避免 {@code longValue()} 截断小数边界
	 * （如 {@code minimum: 0.5} 退化成 {@code 0}）。错误文本与 {@link Rule#range}/{@link Rule#minimum}/
	 * {@link Rule#maximum} 完全一致。
	 */
	private static Rule bounds(String path, Number minimum, Number maximum) {
		BigDecimal min = minimum == null ? null : Rules.decimal(minimum);
		BigDecimal max = maximum == null ? null : Rules.decimal(maximum);
		String minText = min == null ? null : Rules.plain(min);
		String maxText = max == null ? null : Rules.plain(max);
		return value -> {
			Object actual = Rules.read(value, path);
			if (actual == null) {
				return null;
			}
			if (!(actual instanceof Number)) {
				return "字段 " + path + " 不是数值：" + Rules.describe(actual);
			}
			BigDecimal number = Rules.decimal((Number) actual);
			String rangeText = "[" + minText + ", " + maxText + "]";
			if (min != null && number.compareTo(min) < 0) {
				return "字段 " + path + (max == null ? " 小于下限 " + minText : " 超出范围 " + rangeText)
						+ "：" + Rules.plain(number);
			}
			if (max != null && number.compareTo(max) > 0) {
				return "字段 " + path + (min == null ? " 大于上限 " + maxText : " 超出范围 " + rangeText)
						+ "：" + Rules.plain(number);
			}
			return null;
		};
	}

	/** schema 的 {@code type}：取第一个被支持的字符串类型名；数组形式（如 {@code ["string","null"]}）同样适用 */
	private static String typeName(Object raw) {
		if (raw instanceof String) {
			return supported((String) raw);
		}
		if (raw instanceof JSONArray) {
			JSONArray types = (JSONArray) raw;
			for (int i = 0; i < types.size(); i++) {
				String candidate = types.getString(i);
				if (candidate != null && supported(candidate) != null) {
					return supported(candidate);
				}
			}
		}
		// 未知/不支持的 type（如自定义类型名，或数组里全是 null 之外的不支持项）不派生类型规则：
		// 宁可不校验，也不要因名字不认识就 fail-fast 掉整份 schema
		return null;
	}

	private static String supported(String type) {
		String candidate = type.trim().toLowerCase();
		for (String name : Rules.TYPE_NAMES) {
			if (name.equals(candidate)) {
				return candidate;
			}
		}
		return null;
	}

	private static boolean allStrings(JSONArray values) {
		for (int i = 0; i < values.size(); i++) {
			if (!(values.get(i) instanceof String)) {
				return false;
			}
		}
		return true;
	}

	private static Number numeric(Object raw) {
		return raw instanceof Number ? (Number) raw : null;
	}

	private static String join(String prefix, String name) {
		String field = name == null ? "" : name.trim();
		return prefix.isEmpty() ? field : prefix + "." + field;
	}
}
