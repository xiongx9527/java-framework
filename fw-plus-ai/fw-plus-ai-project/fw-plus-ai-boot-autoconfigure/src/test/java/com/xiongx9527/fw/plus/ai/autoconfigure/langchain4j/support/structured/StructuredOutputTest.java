package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.structured;

import com.alibaba.fastjson2.JSONObject;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.assistant.SimpleAssistant;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 结构化输出工具契约测试：指令生成、宽容解析、双层校验（JSONSchema + 轻量规则）、错误回喂重试。
 */
class StructuredOutputTest {

	private static final String SCHEMA = "{"
			+ "\"type\":\"object\","
			+ "\"properties\":{"
			+ "\"orderId\":{\"type\":\"string\"},"
			+ "\"quantity\":{\"type\":\"integer\"},"
			+ "\"status\":{\"type\":\"string\",\"enum\":[\"paid\",\"unpaid\"]}},"
			+ "\"required\":[\"orderId\",\"quantity\"],"
			+ "\"additionalProperties\":false}";

	private static final String GOOD = "{\"orderId\":\"A1\",\"quantity\":3,\"status\":\"paid\"}";

	// -------------------------------------------------- 构建与指令

	@Test
	void instructionCarriesSchemaAndJsonOnlyRule() {
		String instruction = StructuredOutput.of(SCHEMA).instruction();
		assertThat(instruction).contains("只输出 JSON");
		assertThat(instruction).contains("\"orderId\"");
		assertThat(instruction).contains("\"required\"");
	}

	@Test
	void ofTypeGeneratesSchemaFromPojo() {
		StructuredOutput so = StructuredOutput.of(Order.class);
		assertThat(so.jsonSchema()).contains("\"orderId\"").contains("\"quantity\"");
		assertThat(so.instruction()).contains("只输出 JSON");
		// fastjson2 从 POJO 生成的 schema 不含 required（字段全可选）
		assertThat(so.validate(JSONObject.parseObject("{}")).isValid()).isTrue();
	}

	// -------------------------------------------------- 宽容解析

	@Test
	void parsesBareJson() {
		assertThat(StructuredOutput.of(SCHEMA).parse(GOOD).getString("orderId")).isEqualTo("A1");
	}

	@Test
	void stripsJsonFence() {
		assertThat(StructuredOutput.of(SCHEMA).parse("```json\n" + GOOD + "\n```").getIntValue("quantity"))
				.isEqualTo(3);
	}

	@Test
	void stripsSurroundingTalk() {
		String reply = "好的，以下是抽取结果：\n" + GOOD + "\n希望对你有帮助。";
		assertThat(StructuredOutput.of(SCHEMA).parse(reply).getString("status")).isEqualTo("paid");
	}

	@Test
	void rejectsTopLevelArray() {
		assertThatThrownBy(() -> StructuredOutput.of(SCHEMA).parse("[{\"orderId\":\"A1\"}]"))
				.isInstanceOf(StructuredOutputException.class)
				.hasMessageContaining("顶层");
	}

	@Test
	void rejectsNonJson() {
		assertThatThrownBy(() -> StructuredOutput.of(SCHEMA).parse("我无法完成这个任务"))
				.isInstanceOf(StructuredOutputException.class);
	}

	@Test
	void staticParseReplyMatchesInstanceParse() {
		// 静态入口供不持有 schema 的调用方（信封收敛件）复用同一套宽容解析
		assertThat(StructuredOutput.parseReply("```json\n" + GOOD + "\n```"))
				.isEqualTo(StructuredOutput.of(SCHEMA).parse(GOOD));
		assertThatThrownBy(() -> StructuredOutput.parseReply(null))
				.isInstanceOf(StructuredOutputException.class);
		assertThatThrownBy(() -> StructuredOutput.parseReply("我无法完成这个任务"))
				.isInstanceOf(StructuredOutputException.class);
	}

	// -------------------------------------------------- 双层校验

	@Test
	void reportsSchemaTypeViolation() {
		StructuredOutput.Validation validation = StructuredOutput.of(SCHEMA)
				.validate(JSONObject.parseObject("{\"orderId\":\"A1\",\"quantity\":\"三个\"}"));
		assertThat(validation.isValid()).isFalse();
		assertThat(validation.message()).contains("quantity");
		assertThat(validation.errors()).isNotEmpty();
	}

	@Test
	void reportsMissingRequiredField() {
		StructuredOutput.Validation validation = StructuredOutput.of(SCHEMA)
				.validate(JSONObject.parseObject("{\"quantity\":3}"));
		assertThat(validation.isValid()).isFalse();
		assertThat(validation.message()).contains("orderId");
	}

	@Test
	void acceptsValidValue() {
		assertThat(StructuredOutput.of(SCHEMA).validate(JSONObject.parseObject(GOOD)).isValid()).isTrue();
	}

	@Test
	void collectsAllRuleErrors() {
		StructuredOutput so = StructuredOutput.of(SCHEMA)
				.rules(Rule.required("orderId"), Rule.enumOf("status", "paid", "unpaid"));
		StructuredOutput.Validation validation = so.validate(JSONObject.parseObject("{\"quantity\":3}"));
		assertThat(validation.isValid()).isFalse();
		// 结构层只报第一个错误（required orderId）；轻量规则把剩余两条补齐（缺 orderId、status 非法）
		assertThat(validation.errors()).hasSize(3);
	}

	@Test
	void rulesCatchWhatSchemaAllows() {
		// quantity=0 通过 schema（合法 integer），业务要求 >= 1 → 只能靠规则层拦下
		StructuredOutput so = StructuredOutput.of(SCHEMA).rules(Rule.range("quantity", 1, 99));
		StructuredOutput.Validation validation = so.validate(
				JSONObject.parseObject("{\"orderId\":\"A1\",\"quantity\":0,\"status\":\"paid\"}"));
		assertThat(validation.isValid()).isFalse();
		assertThat(validation.errors()).hasSize(1);
		assertThat(validation.message()).contains("quantity").contains("超出范围");
	}

	@Test
	void builtInRulesCoverTypeEnumAndRange() {
		StructuredOutput so = StructuredOutput.of(SCHEMA)
				.rules(Rule.type("quantity", "integer"),
						Rule.enumOf("status", "paid", "unpaid"),
						Rule.range("quantity", 1, 99));
		StructuredOutput.Validation validation = so.validate(
				JSONObject.parseObject("{\"orderId\":\"A1\",\"quantity\":0,\"status\":\"done\"}"));
		assertThat(validation.isValid()).isFalse();
		// 结构层 1 条（status 不在 schema 枚举内）+ 规则层 2 条（range 越界、enum 不允许）
		assertThat(validation.errors()).hasSize(3);
		assertThat(validation.message()).contains("quantity").contains("status");
	}

	@Test
	void ruleFailureIsReportedNotThrown() {
		Rule exploding = value -> {
			throw new IllegalStateException("boom");
		};
		StructuredOutput.Validation validation = StructuredOutput.of(SCHEMA)
				.rules(exploding)
				.validate(JSONObject.parseObject(GOOD));
		assertThat(validation.isValid()).isFalse();
		assertThat(validation.errors()).anyMatch(error -> error.contains("boom"));
	}

	// -------------------------------------------------- 点号路径（嵌套字段）

	private static final String LOOSE = "{\"type\":\"object\"}";

	@Test
	void dotPathRequiredAcceptsPresentNestedField() {
		StructuredOutput so = StructuredOutput.of(LOOSE).rules(Rule.required("order.buyer.name"));
		assertThat(so.validate(JSONObject.parseObject("{\"order\":{\"buyer\":{\"name\":\"张三\"}}}")).isValid())
				.isTrue();
	}

	@Test
	void dotPathRequiredReportsFullPathWhenLeafMissing() {
		StructuredOutput so = StructuredOutput.of(LOOSE).rules(Rule.required("order.buyer.name"));
		StructuredOutput.Validation validation = so.validate(JSONObject.parseObject("{\"order\":{\"buyer\":{}}}"));
		assertThat(validation.isValid()).isFalse();
		assertThat(validation.message()).contains("order.buyer.name");
	}

	@Test
	void dotPathRequiredReportsFullPathWhenIntermediateMissing() {
		StructuredOutput so = StructuredOutput.of(LOOSE).rules(Rule.required("order.buyer.name"));
		StructuredOutput.Validation validation = so.validate(JSONObject.parseObject("{\"order\":{}}"));
		assertThat(validation.isValid()).isFalse();
		assertThat(validation.message()).contains("order.buyer.name");
	}

	@Test
	void dotPathTypeChecksNestedValue() {
		StructuredOutput so = StructuredOutput.of(LOOSE).rules(Rule.type("order.quantity", "integer"));
		assertThat(so.validate(JSONObject.parseObject("{\"order\":{\"quantity\":2}}")).isValid()).isTrue();
		assertThat(so.validate(JSONObject.parseObject("{\"order\":{\"quantity\":\"两个\"}}")).message())
				.contains("order.quantity");
	}

	@Test
	void dotPathEnumChecksNestedValue() {
		StructuredOutput so = StructuredOutput.of(LOOSE).rules(Rule.enumOf("order.status", "paid", "unpaid"));
		assertThat(so.validate(JSONObject.parseObject("{\"order\":{\"status\":\"paid\"}}")).isValid()).isTrue();
		assertThat(so.validate(JSONObject.parseObject("{\"order\":{\"status\":\"done\"}}")).message())
				.contains("order.status");
	}

	@Test
	void dotPathRangeChecksNestedValue() {
		StructuredOutput so = StructuredOutput.of(LOOSE).rules(Rule.range("order.quantity", 1, 9));
		assertThat(so.validate(JSONObject.parseObject("{\"order\":{\"quantity\":5}}")).isValid()).isTrue();
		assertThat(so.validate(JSONObject.parseObject("{\"order\":{\"quantity\":0}}")).message())
				.contains("order.quantity");
	}

	@Test
	void dotPathRegexChecksNestedValue() {
		StructuredOutput so = StructuredOutput.of(LOOSE).rules(Rule.regex("order.code", "A\\d+"));
		assertThat(so.validate(JSONObject.parseObject("{\"order\":{\"code\":\"A12\"}}")).isValid()).isTrue();
		assertThat(so.validate(JSONObject.parseObject("{\"order\":{\"code\":\"B1\"}}")).message())
				.contains("order.code");
	}

	@Test
	void dotPathTreatsNonObjectIntermediateAsMissing() {
		// 中间层不是对象时按“缺失”处理，不抛异常
		StructuredOutput so = StructuredOutput.of(LOOSE).rules(Rule.required("order.buyer.name"));
		StructuredOutput.Validation validation = so.validate(JSONObject.parseObject("{\"order\":\"普通字符串\"}"));
		assertThat(validation.isValid()).isFalse();
		assertThat(validation.message()).contains("order.buyer.name");
	}

	@Test
	void topLevelRulesKeepWorkingAfterPathSupport() {
		StructuredOutput so = StructuredOutput.of(LOOSE).rules(Rule.required("orderId"), Rule.enumOf("status", "paid"));
		assertThat(so.validate(JSONObject.parseObject("{\"orderId\":\"A1\",\"status\":\"paid\"}")).isValid()).isTrue();
		assertThat(so.validate(JSONObject.parseObject("{\"status\":\"done\"}")).errors()).hasSize(2);
	}

	// -------------------------------------------------- 从 schema 派生规则

	private static final String NESTED_SCHEMA = "{"
			+ "\"type\":\"object\","
			+ "\"properties\":{"
			+ "\"order\":{\"type\":\"object\",\"properties\":{"
			+ "\"buyer\":{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"}},\"required\":[\"name\"]},"
			+ "\"quantity\":{\"type\":\"integer\",\"minimum\":1,\"maximum\":9},"
			+ "\"status\":{\"type\":\"string\",\"enum\":[\"paid\",\"unpaid\"]},"
			+ "\"code\":{\"type\":\"string\",\"pattern\":\"^A[0-9]+$\"}"
			+ "}}},"
			+ "\"required\":[\"order\"]}";

	private static final String NESTED_GOOD =
			"{\"order\":{\"buyer\":{\"name\":\"张三\"},\"quantity\":3,\"status\":\"paid\",\"code\":\"A12\"}}";

	private static StructuredOutput withDerivedRules(String schema) {
		return StructuredOutput.of(schema).rules(Rule.fromSchema(schema));
	}

	@Test
	void acceptsValueSatisfyingDerivedRules() {
		assertThat(withDerivedRules(NESTED_SCHEMA).validate(JSONObject.parseObject(NESTED_GOOD)).isValid()).isTrue();
	}

	@Test
	void derivedRulesCoverNestedRequired() {
		StructuredOutput.Validation validation = withDerivedRules(NESTED_SCHEMA)
				.validate(JSONObject.parseObject("{\"order\":{\"buyer\":{},\"quantity\":3,\"status\":\"paid\"}}"));
		assertThat(validation.isValid()).isFalse();
		assertThat(validation.message()).contains("order.buyer.name");
	}

	@Test
	void derivedRulesCoverNestedEnum() {
		StructuredOutput.Validation validation = withDerivedRules(NESTED_SCHEMA)
				.validate(JSONObject.parseObject("{\"order\":{\"buyer\":{\"name\":\"张三\"},\"quantity\":3,\"status\":\"done\"}}"));
		assertThat(validation.isValid()).isFalse();
		assertThat(validation.message()).contains("order.status");
	}

	@Test
	void derivedRulesCoverNestedRange() {
		StructuredOutput.Validation validation = withDerivedRules(NESTED_SCHEMA)
				.validate(JSONObject.parseObject("{\"order\":{\"buyer\":{\"name\":\"张三\"},\"quantity\":0,\"status\":\"paid\"}}"));
		assertThat(validation.isValid()).isFalse();
		assertThat(validation.message()).contains("order.quantity").contains("超出范围");
	}

	@Test
	void derivedRulesCoverNestedPattern() {
		StructuredOutput.Validation validation = withDerivedRules(NESTED_SCHEMA)
				.validate(JSONObject.parseObject("{\"order\":{\"buyer\":{\"name\":\"张三\"},\"quantity\":3,\"status\":\"paid\",\"code\":\"B1\"}}"));
		assertThat(validation.isValid()).isFalse();
		assertThat(validation.message()).contains("order.code");
	}

	@Test
	void derivedRulesCoverTopLevelRequired() {
		String flat = "{\"type\":\"object\",\"properties\":{\"orderId\":{\"type\":\"string\"}},\"required\":[\"orderId\"]}";
		StructuredOutput.Validation validation = withDerivedRules(flat)
				.validate(JSONObject.parseObject("{}"));
		assertThat(validation.isValid()).isFalse();
		assertThat(validation.message()).contains("orderId");
	}

	@Test
	void derivedRulesCoexistWithManualRules() {
		StructuredOutput so = StructuredOutput.of(NESTED_SCHEMA)
				.rules(Rule.fromSchema(NESTED_SCHEMA))
				.rules(Rule.required("渠道"));
		StructuredOutput.Validation validation = so.validate(JSONObject.parseObject(NESTED_GOOD));
		assertThat(validation.isValid()).isFalse();
		assertThat(validation.errors()).hasSize(1);
		assertThat(validation.message()).contains("渠道");
	}

	@Test
	void fromSchemaRejectsInvalidText() {
		assertThatThrownBy(() -> Rule.fromSchema("不是 JSON"))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void fromSchemaRejectsBlank() {
		assertThatThrownBy(() -> Rule.fromSchema(null)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> Rule.fromSchema("   ")).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void fromSchemaRejectsNonObjectTopLevel() {
		assertThatThrownBy(() -> Rule.fromSchema("[]"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("顶层");
		assertThatThrownBy(() -> Rule.fromSchema("123"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("顶层");
	}

	@Test
	void fromSchemaRejectsIllegalStructure() {
		assertThatThrownBy(() -> Rule.fromSchema("{\"properties\":\"abc\"}"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("properties");
		assertThatThrownBy(() -> Rule.fromSchema("{\"properties\":{\"code\":{\"type\":\"string\",\"pattern\":\"[\"}}}"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("pattern");
	}

	@Test
	void derivedRequiredAcceptsExplicitNull() {
		String nullable = "{\"type\":\"object\",\"properties\":{\"note\":{\"type\":[\"string\",\"null\"]}},"
				+ "\"required\":[\"note\"]}";
		// 键存在、显式为 null 是合法值 → 派生的必填规则不得假阳性
		assertThat(withDerivedRules(nullable).validate(JSONObject.parseObject("{\"note\":null}")).isValid()).isTrue();
		assertThat(withDerivedRules(nullable).validate(JSONObject.parseObject("{}")).isValid()).isFalse();
	}

	@Test
	void derivedRequiredDeduplicatesNames() {
		String duplicated = "{\"type\":\"object\",\"properties\":{\"a\":{\"type\":\"string\"}},"
				+ "\"required\":[\"a\",\"a\"]}";
		StructuredOutput.Validation validation = StructuredOutput.of(LOOSE).rules(Rule.fromSchema(duplicated))
				.validate(JSONObject.parseObject("{}"));
		assertThat(validation.errors()).hasSize(1);
	}

	@Test
	void derivedRulesCoverSingleSidedBounds() {
		String minOnly = "{\"type\":\"object\",\"properties\":{\"quantity\":{\"type\":\"integer\",\"minimum\":1}}}";
		StructuredOutput.Validation low = withDerivedRules(minOnly).validate(JSONObject.parseObject("{\"quantity\":0}"));
		assertThat(low.isValid()).isFalse();
		assertThat(low.message()).contains("quantity").contains("小于下限");
		assertThat(withDerivedRules(minOnly).validate(JSONObject.parseObject("{\"quantity\":1}")).isValid()).isTrue();

		String maxOnly = "{\"type\":\"object\",\"properties\":{\"quantity\":{\"type\":\"integer\",\"maximum\":9}}}";
		StructuredOutput.Validation high = withDerivedRules(maxOnly).validate(JSONObject.parseObject("{\"quantity\":10}"));
		assertThat(high.isValid()).isFalse();
		assertThat(high.message()).contains("quantity").contains("大于上限");
		assertThat(withDerivedRules(maxOnly).validate(JSONObject.parseObject("{\"quantity\":9}")).isValid()).isTrue();
	}

	@Test
	void derivedDecimalBoundsAreNotTruncated() {
		// 只测规则层：schema 层用宽松 schema，避免 fastjson2 的边界校验混入
		String schema = "{\"type\":\"object\",\"properties\":{\"score\":{\"type\":\"number\","
				+ "\"minimum\":0.5,\"maximum\":4.5}}}";
		StructuredOutput so = StructuredOutput.of(LOOSE).rules(Rule.fromSchema(schema));

		StructuredOutput.Validation low = so.validate(JSONObject.parseObject("{\"score\":0.4}"));
		assertThat(low.isValid()).isFalse();
		assertThat(low.message()).contains("score").contains("0.5").contains("超出范围");

		StructuredOutput.Validation high = so.validate(JSONObject.parseObject("{\"score\":4.6}"));
		assertThat(high.isValid()).isFalse();
		assertThat(high.message()).contains("score").contains("4.5").contains("超出范围");

		assertThat(so.validate(JSONObject.parseObject("{\"score\":0.5}")).isValid()).isTrue();
		assertThat(so.validate(JSONObject.parseObject("{\"score\":4.5}")).isValid()).isTrue();
	}

	@Test
	void minimumAndMaximumCheckTopLevelAndDotPath() {
		StructuredOutput so = StructuredOutput.of(LOOSE)
				.rules(Rule.minimum("quantity", 1), Rule.maximum("order.limit", 9));
		assertThat(so.validate(JSONObject.parseObject("{\"quantity\":1,\"order\":{\"limit\":9}}")).isValid()).isTrue();
		assertThat(so.validate(JSONObject.parseObject("{\"quantity\":0,\"order\":{\"limit\":9}}")).message())
				.contains("quantity").contains("小于下限");
		assertThat(so.validate(JSONObject.parseObject("{\"quantity\":1,\"order\":{\"limit\":10}}")).message())
				.contains("order.limit").contains("大于上限");
	}

	// -------------------------------------------------- 重试回路

	@Test
	void retriesWithPreviousErrorsFedBack() {
		List<String> prompts = new ArrayList<>();
		StructuredOutput.ChatFunction chat = prompt -> {
			prompts.add(prompt);
			return prompts.size() == 1 ? "这不是 JSON" : GOOD;
		};

		JSONObject result = StructuredOutput.of(SCHEMA).chat(chat, "把订单抽出来", 3);

		assertThat(result.getString("orderId")).isEqualTo("A1");
		assertThat(prompts).hasSize(2);
		assertThat(prompts.get(0)).contains("把订单抽出来");
		assertThat(prompts.get(1)).contains("你上一次的输出未通过校验");
		assertThat(prompts.get(1)).contains("把订单抽出来");
	}

	@Test
	void retriesOnSchemaViolation() {
		List<String> prompts = new ArrayList<>();
		StructuredOutput.ChatFunction chat = prompt -> {
			prompts.add(prompt);
			return prompts.size() == 1 ? "{\"quantity\":3}" : GOOD;
		};

		JSONObject result = StructuredOutput.of(SCHEMA).chat(chat, "把订单抽出来", 3);

		assertThat(result.getString("orderId")).isEqualTo("A1");
		assertThat(prompts.get(1)).contains("orderId");
	}

	@Test
	void throwsWithLastReplyAfterExhaustingAttempts() {
		String[] replies = {"first broken", "second broken"};
		int[] calls = {0};
		StructuredOutput.ChatFunction chat = prompt -> replies[Math.min(calls[0]++, replies.length - 1)];

		assertThatThrownBy(() -> StructuredOutput.of(SCHEMA).chat(chat, "把订单抽出来", 2))
				.isInstanceOf(StructuredOutputException.class)
				.hasMessageContaining("结构化输出失败")
				.satisfies(error -> {
					StructuredOutputException exception = (StructuredOutputException) error;
					assertThat(exception.getLastReply()).isEqualTo("second broken");
					// 解析失败的路径也要留下错误清单，不能只有 message
					assertThat(exception.getErrors()).isNotEmpty();
				});
		assertThat(calls[0]).isEqualTo(2);
	}

	@Test
	void chatWithAssistantUsesExistingChatSignature() {
		SimpleAssistant assistant = mock(SimpleAssistant.class);
		when(assistant.chat(eq("extract-1"), anyString())).thenReturn(GOOD);

		JSONObject result = StructuredOutput.of(SCHEMA).chat(assistant, "extract-1", "把订单抽出来");

		assertThat(result.getString("orderId")).isEqualTo("A1");
		// 传下去的确实是拼好的提示词（指令 + 用户输入），不是原始用户输入
		ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
		verify(assistant).chat(eq("extract-1"), prompt.capture());
		assertThat(prompt.getValue()).contains("只输出 JSON").contains("把订单抽出来");
	}

	// -------------------------------------------------- 零件模式

	@Test
	void promptWithoutErrorsIsInstructionPlusUserText() {
		String prompt = StructuredOutput.of(SCHEMA).prompt("把订单抽出来");
		assertThat(prompt).contains("只输出 JSON").contains("把订单抽出来");
		assertThat(prompt).doesNotContain("你上一次的输出未通过校验");
	}

	@Test
	void promptWithErrorsFeedsBackPreviousFailure() {
		String prompt = StructuredOutput.of(SCHEMA).prompt("把订单抽出来", "required orderId");
		assertThat(prompt).contains("required orderId").contains("把订单抽出来");
	}

	/** 供 of(Type) 测试用：字段全可选（fastjson2 由 POJO 生成 schema 的行为） */
	public static class Order {

		public String orderId;

		public Integer quantity;

		public String status;
	}
}
