package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.structured;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.schema.JSONSchema;
import com.alibaba.fastjson2.schema.ValidateResult;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.assistant.SimpleAssistant;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 结构化输出工具：把一份 JSON Schema 变成「提示词指令 + 宽容解析 + 双层校验 + 错误回喂重试」，
 * 用于让**任意模型**（不支持原生 JSON Schema 的也算）稳定返回可用的 JSON 对象。
 *
 * <p>它不是一个新入口：只消费既有对象与既有方法，不改变任何现有入参/出参。
 * 一份 schema 同时驱动两件事——生成提示词指令、在应用侧校验返回值。</p>
 *
 * <pre>{@code
 * StructuredOutput so = StructuredOutput.of(schemaText);
 *
 * // 一条龙：指令 → 调用 → 解析 → 校验 → 失败回喂重试
 * JSONObject order = so.chat(assistant, memoryId, "把订单信息抽出来");
 *
 * // 零件模式：循环自己写
 * String reply = assistant.chat(memoryId, so.prompt(userText));
 * JSONObject value = so.parse(reply);
 * StructuredOutput.Validation validation = so.validate(value);
 * if (!validation.isValid()) {
 *     // validation.errors() 就是回喂给模型 / 记日志的料
 * }
 * }</pre>
 *
 * <p>校验分两道：先是 fastjson2 的 JSON Schema（结构合法性，只报第一个错），
 * 再是 {@link Rule} 轻量规则（补齐全量错误清单与业务语义）。顶层必须是 JSON 对象；
 * 数组请包一层（如 {@code {"items": [...]}}）。</p>
 */
public final class StructuredOutput {

	/** {@code chat(...)} 默认尝试次数（含首次） */
	public static final int DEFAULT_MAX_ATTEMPTS = 3;

	private static final String INSTRUCTION_PREFIX = "你必须只输出 JSON。不要输出 Markdown 代码块，不要输出解释或任何多余文字。\n"
			+ "严格遵循以下 JSON 结构：\n";

	private static final String RETRY_PREFIX = "你上一次的输出未通过校验：";

	private static final String RETRY_SUFFIX = "\n请重新只输出 JSON。\n\n";

	private static final String USER_TEXT_PREFIX = "待处理文本：\n";

	private final String jsonSchema;

	private final JSONSchema validator;

	private final List<Rule> rules = new ArrayList<Rule>();

	private StructuredOutput(String jsonSchema, JSONSchema validator) {
		this.jsonSchema = jsonSchema;
		this.validator = validator;
	}

	// -------------------------------------------------- 构建

	/** 从 JSON Schema 文本构建 */
	public static StructuredOutput of(String jsonSchemaText) {
		if (jsonSchemaText == null || jsonSchemaText.trim().isEmpty()) {
			throw new IllegalArgumentException("jsonSchema 不能为空");
		}
		JSONSchema validator;
		try {
			validator = JSONSchema.parseSchema(jsonSchemaText);
		} catch (RuntimeException e) {
			throw new IllegalArgumentException("jsonSchema 不是合法的 JSON Schema：" + e.getMessage(), e);
		}
		return new StructuredOutput(jsonSchemaText.trim(), validator);
	}

	/**
	 * 从类型构建（Java 侧已有 POJO 时用），schema 由 fastjson2 反推。
	 *
	 * <p>注意：反推出来的 schema <b>不含 required</b>（字段全部可选），需要必填约束就补 {@link #rules(Rule...)}。</p>
	 */
	public static StructuredOutput of(Type type) {
		if (type == null) {
			throw new IllegalArgumentException("type 不能为 null");
		}
		JSONSchema validator = JSONSchema.of(type);
		return new StructuredOutput(validator.toString(), validator);
	}

	/** 追加轻量规则（可多次调用累加），返回自身便于链式书写 */
	public StructuredOutput rules(Rule... rules) {
		if (rules != null) {
			for (Rule rule : rules) {
				if (rule != null) {
					this.rules.add(rule);
				}
			}
		}
		return this;
	}

	/** 本工具使用的 JSON Schema 文本 */
	public String jsonSchema() {
		return jsonSchema;
	}

	/** 提示词指令：由 schema 生成，拼进 message 或系统提示即可 */
	public String instruction() {
		return INSTRUCTION_PREFIX + jsonSchema;
	}

	// -------------------------------------------------- 提示词

	/** 首轮提示词：指令 + 用户输入 */
	public String prompt(String userText) {
		return instruction() + "\n\n" + USER_TEXT_PREFIX + userText;
	}

	/** 重试提示词：把上一次的校验错误回喂给模型；{@code previousErrors} 为空时等价于 {@link #prompt(String)} */
	public String prompt(String userText, String previousErrors) {
		if (previousErrors == null || previousErrors.trim().isEmpty()) {
			return prompt(userText);
		}
		return instruction() + "\n\n" + RETRY_PREFIX + previousErrors + RETRY_SUFFIX + USER_TEXT_PREFIX + userText;
	}

	// -------------------------------------------------- 解析

	/**
	 * 宽容解析：剥掉 {@code ```json} 围栏、忽略前后话术，取首个 {@code &#123;} 到末个 {@code &#125;} 解析。
	 *
	 * @throws StructuredOutputException 返回为空 / 顶层是数组 / 找不到或解析不出 JSON 对象
	 */
	public JSONObject parse(String reply) {
		return parseReply(reply);
	}

	/**
	 * 宽容解析（静态入口）：逻辑与 {@link #parse(String)} 完全一致，供不持有 schema 的调用方复用，
	 * 避免在多个包内重复实现“剥围栏 + 取首尾大括号”的边界处理。
	 *
	 * @throws StructuredOutputException 返回为空 / 顶层是数组 / 找不到或解析不出 JSON 对象
	 */
	public static JSONObject parseReply(String reply) {
		if (reply == null) {
			throw new StructuredOutputException("模型返回为空，无法解析为 JSON");
		}
		String text = stripFence(reply);
		int firstObject = text.indexOf('{');
		int firstArray = text.indexOf('[');
		if (firstArray >= 0 && (firstObject < 0 || firstArray < firstObject)) {
			throw new StructuredOutputException("返回内容顶层是数组，本工具要求顶层为 JSON 对象（数组请包一层，如 "
					+ "{\"items\": [...]}）", reply, null);
		}
		int lastObject = text.lastIndexOf('}');
		if (firstObject < 0 || lastObject <= firstObject) {
			throw new StructuredOutputException("返回内容里找不到 JSON 对象", reply, null);
		}
		try {
			return JSON.parseObject(text.substring(firstObject, lastObject + 1), JSONReader.Feature.SupportSmartMatch);
		} catch (JSONException e) {
			throw new StructuredOutputException("返回内容不是合法 JSON：" + e.getMessage(), reply, null, e);
		}
	}

	// -------------------------------------------------- 校验

	/**
	 * 双层校验：JSON Schema（结构）+ 轻量规则（全量清单/业务语义）。
	 *
	 * <p>规则只作用于 JSON 对象；规则自身抛异常时会被记为一条错误，不会打断整个校验。</p>
	 */
	public Validation validate(Object value) {
		List<String> errors = new ArrayList<String>();
		if (value == null) {
			errors.add("待校验的值为 null");
			return new Validation(errors);
		}
		ValidateResult result = validator.validate(value);
		if (!result.isSuccess()) {
			errors.add(result.getMessage());
		}
		if (value instanceof JSONObject && !rules.isEmpty()) {
			JSONObject object = (JSONObject) value;
			for (Rule rule : rules) {
				try {
					String error = rule.check(object);
					if (error != null && !error.trim().isEmpty()) {
						errors.add(error);
					}
				} catch (RuntimeException e) {
					errors.add("规则执行异常：" + e);
				}
			}
		}
		return new Validation(errors);
	}

	// -------------------------------------------------- 重试回路

	/** 一条龙（默认尝试 {@value #DEFAULT_MAX_ATTEMPTS} 次）：使用既有 {@link SimpleAssistant#chat(String, String)} */
	public JSONObject chat(SimpleAssistant assistant, String memoryId, String userText) {
		return chat(assistant, memoryId, userText, DEFAULT_MAX_ATTEMPTS);
	}

	/**
	 * 一条龙：指令 → 调用 → 解析 → 校验，失败把错误回喂重试，最多 {@code maxAttempts} 次（含首次）。
	 *
	 * <p>每轮复用同一个 {@code memoryId}，模型因此能看到自己上一轮的错误输出。</p>
	 *
	 * @throws StructuredOutputException 尝试次数用尽仍未通过校验（携带最后一次原文与错误清单）
	 */
	public JSONObject chat(SimpleAssistant assistant, String memoryId, String userText, int maxAttempts) {
		if (assistant == null) {
			throw new IllegalArgumentException("assistant 不能为 null");
		}
		final SimpleAssistant target = assistant;
		final String sessionId = memoryId;
		return chat(prompt -> target.chat(sessionId, prompt), userText, maxAttempts);
	}

	/** 一条龙（回调式）：由调用方决定怎么发这一次请求 */
	public JSONObject chat(ChatFunction chat, String userText, int maxAttempts) {
		if (chat == null) {
			throw new IllegalArgumentException("chat 不能为 null");
		}
		int attempts = maxAttempts < 1 ? 1 : maxAttempts;
		String errors = null;
		List<String> lastErrors = Collections.emptyList();
		String lastReply = null;
		for (int attempt = 1; attempt <= attempts; attempt++) {
			lastReply = chat.send(errors == null ? prompt(userText) : prompt(userText, errors));
			try {
				JSONObject value = parse(lastReply);
				Validation validation = validate(value);
				if (validation.isValid()) {
					return value;
				}
				lastErrors = validation.errors();
				errors = validation.message();
			} catch (StructuredOutputException e) {
				// 解析失败（非 JSON / 顶层是数组 / 空返回）也进错误清单，调用方不必去翻 message
				lastErrors = Collections.singletonList(e.getMessage());
				errors = e.getMessage();
			}
		}
		throw new StructuredOutputException("结构化输出失败（已尝试 " + attempts + " 次）：" + errors, lastReply, lastErrors);
	}

	// -------------------------------------------------- 内部工具

	/** 剥掉 Markdown 代码块围栏（首行 ```json / ```，以及结尾 ```） */
	private static String stripFence(String reply) {
		String text = reply.trim();
		if (text.startsWith("```")) {
			int firstLineEnd = text.indexOf('\n');
			text = firstLineEnd < 0 ? text.substring(3) : text.substring(firstLineEnd + 1);
			int fenceEnd = text.lastIndexOf("```");
			if (fenceEnd >= 0) {
				text = text.substring(0, fenceEnd);
			}
		}
		return text.trim();
	}

	/** 发一次请求：提示词进、原始回复出 */
	@FunctionalInterface
	public interface ChatFunction {

		String send(String prompt);
	}

	/** 校验结果 */
	public static final class Validation {

		private final List<String> errors;

		Validation(List<String> errors) {
			this.errors = Collections.unmodifiableList(new ArrayList<String>(errors));
		}

		public boolean isValid() {
			return errors.isEmpty();
		}

		/** 错误清单：JSON Schema 的错误在前，轻量规则的错误在后 */
		public List<String> errors() {
			return errors;
		}

		/** 错误清单拼接文本（回喂给模型用）；通过时为空串 */
		public String message() {
			return String.join("; ", errors);
		}
	}
}
