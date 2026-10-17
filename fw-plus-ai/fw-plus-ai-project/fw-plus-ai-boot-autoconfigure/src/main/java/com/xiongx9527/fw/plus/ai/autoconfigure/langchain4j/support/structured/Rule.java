package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.structured;

import com.alibaba.fastjson2.JSONObject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 轻量校验规则：在 JSON Schema 结构校验之外，补齐「全量错误清单」与业务语义校验。
 *
 * <p>{@link #check(JSONObject)} 返回 {@code null}（或空白）表示通过，否则返回错误文本，
 * 错误文本会进错误清单并被回喂给模型。内置工厂的字段参数接受点号路径（如 {@code order.buyer.name}）
 * 定位嵌套字段，错误文本包含完整路径；无点号时行为与顶层字段规则一致。
 * 更复杂的语义直接写 lambda：</p>
 *
 * <pre>{@code
 * Rule paidNeedsQuantity = value -> "paid".equals(value.getString("status"))
 *         && value.getIntValue("quantity") <= 0 ? "status=paid 时 quantity 必须大于 0" : null;
 * }</pre>
 */
@FunctionalInterface
public interface Rule {

	/** 校验：通过返回 {@code null}，否则返回错误文本 */
	String check(JSONObject value);

	/** 必填：字段缺失或值为 {@code null} 时报错 */
	static Rule required(String field) {
		return value -> Rules.read(value, field) == null ? "缺少必填字段：" + field : null;
	}

	/**
	 * 存在性：键不存在时报错，值可以为 {@code null}（对齐 JSON Schema 的 {@code required} 语义）。
	 *
	 * <p>从 schema 派生必填约束时用本工厂，避免把「显式为 null 的合法字段」误判为缺失。</p>
	 */
	static Rule present(String field) {
		return value -> Rules.contains(value, field) ? null : "缺少必填字段：" + field;
	}

	/** 类型：字段存在时按类型报错（缺字段交由 {@link #required(String)} 负责） */
	static Rule type(String field, String type) {
		Rules.requireTypeName(type);
		return value -> {
			Object actual = Rules.read(value, field);
			if (actual == null) {
				return null;
			}
			return Rules.matchesType(actual, type) ? null
					: "字段 " + field + " 类型不符：期望 " + type + "，实际 " + Rules.describe(actual);
		};
	}

	/** 枚举：字段缺失或取值不在允许值内时报错 */
	static Rule enumOf(String field, String... allowed) {
		List<String> allowedValues = allowed == null ? new ArrayList<String>() : Arrays.asList(allowed);
		String allowedText = allowedValues.toString();
		return value -> {
			Object actual = Rules.read(value, field);
			if (actual == null) {
				return "字段 " + field + " 缺失，允许值：" + allowedText;
			}
			if (actual instanceof String && allowedValues.contains(actual)) {
				return null;
			}
			return "字段 " + field + " 取值不允许：" + Rules.describe(actual) + "，允许值：" + allowedText;
		};
	}

	/** 数值区间（闭区间）：字段存在且为数值时报错，缺字段交由 {@link #required(String)} 负责 */
	static Rule range(String field, long min, long max) {
		return value -> {
			Object actual = Rules.read(value, field);
			if (actual == null) {
				return null;
			}
			if (!(actual instanceof Number)) {
				return "字段 " + field + " 不是数值：" + Rules.describe(actual);
			}
			BigDecimal number = Rules.decimal((Number) actual);
			boolean inside = number.compareTo(BigDecimal.valueOf(min)) >= 0
					&& number.compareTo(BigDecimal.valueOf(max)) <= 0;
			return inside ? null
					: "字段 " + field + " 超出范围 [" + min + ", " + max + "]：" + Rules.plain(number);
		};
	}

	/** 正则：字段存在且为字符串时按正则全匹配（{@code matches()}）报错 */
	static Rule regex(String field, String regex) {
		Pattern pattern = Pattern.compile(regex);
		return value -> {
			Object actual = Rules.read(value, field);
			if (actual == null) {
				return null;
			}
			if (!(actual instanceof String)) {
				return "字段 " + field + " 不是字符串：" + Rules.describe(actual);
			}
			return pattern.matcher((String) actual).matches() ? null
					: "字段 " + field + " 不匹配正则 " + regex + "：" + actual;
		};
	}

	/** 数值下限（闭区间）：字段存在且为数值时报错，缺字段交由 {@link #required(String)} 负责 */
	static Rule minimum(String field, long min) {
		return value -> {
			Object actual = Rules.read(value, field);
			if (actual == null) {
				return null;
			}
			if (!(actual instanceof Number)) {
				return "字段 " + field + " 不是数值：" + Rules.describe(actual);
			}
			BigDecimal number = Rules.decimal((Number) actual);
			return number.compareTo(BigDecimal.valueOf(min)) >= 0 ? null
					: "字段 " + field + " 小于下限 " + min + "：" + Rules.plain(number);
		};
	}

	/** 数值上限（闭区间）：字段存在且为数值时报错，缺字段交由 {@link #required(String)} 负责 */
	static Rule maximum(String field, long max) {
		return value -> {
			Object actual = Rules.read(value, field);
			if (actual == null) {
				return null;
			}
			if (!(actual instanceof Number)) {
				return "字段 " + field + " 不是数值：" + Rules.describe(actual);
			}
			BigDecimal number = Rules.decimal((Number) actual);
			return number.compareTo(BigDecimal.valueOf(max)) <= 0 ? null
					: "字段 " + field + " 大于上限 " + max + "：" + Rules.plain(number);
		};
	}

	/**
	 * 从 JSON Schema 文本派生等效规则：必填、类型、枚举、数值范围、字符串模式，递归处理嵌套对象并生成点号路径。
	 *
	 * <p>派生规则与内置工厂使用同一套错误文本格式，可直接回喂给模型。必填派生用 {@link #present(String)}
	 * （键存在即可，值可为 {@code null}），以对齐 JSON Schema 的 {@code required} 语义。
	 * 能力边界见 {@code SchemaRules} 的说明：{@code anyOf} / {@code oneOf} / {@code allOf}、
	 * {@code exclusiveMinimum} / {@code exclusiveMaximum} / {@code multipleOf}、
	 * {@code items} 内部结构与非字符串枚举值都不派生，需手写规则补齐。</p>
	 *
	 * @throws IllegalArgumentException schema 为空、不是合法 JSON、顶层不是对象，或结构非法（如 {@code properties}
	 *                                  不是对象、{@code pattern} 不是合法正则）
	 */
	static Rule[] fromSchema(String jsonSchemaText) {
		return SchemaRules.from(jsonSchemaText);
	}
}
