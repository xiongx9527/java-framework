package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.envelope;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.structured.Rule;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.structured.StructuredOutput;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 内层结果语义契约测试：内层 `result` 顶层必须是对象（列表包一层）、业务追问是成功结果而非校验失败、
 * 缺字段按失败处理，且不靠关键词猜追问。
 *
 * <p>分工说明：**什么算合法**由 {@link BusinessContract#structuredOutput()} 的 schema + 规则决定（本类直接断言）；
 * **怎么把校验结果变成信封**不再由本类镜像，而是走<b>真实入口</b>
 * （{@link EnvelopeRuntime#decide} → {@link EnvelopeRuntime#structuredRound}，与第 4 组接线同一条代码路径），
 * 因此这里的信封断言是端到端事实。{@link EnvelopeAssembler} 只保证 `result` 只可能是对象。</p>
 */
class InnerResultSemanticsTest {

	/** 顶层对象 + 列表包一层（{@code items}） */
	private static final String LIST_SCHEMA = "{"
			+ "\"type\":\"object\","
			+ "\"properties\":{"
			+ "\"orderId\":{\"type\":\"string\"},"
			+ "\"items\":{\"type\":\"array\",\"items\":{\"type\":\"object\",\"properties\":{\"sku\":{\"type\":\"string\"}}}},"
			+ "\"totalCount\":{\"type\":\"integer\"}},"
			+ "\"required\":[\"orderId\",\"items\",\"totalCount\"]}";

	/** 追问声明成结果里的普通字段：{@code needMoreInfo} + {@code question} 均为必填 */
	private static final String FOLLOW_UP_SCHEMA = "{"
			+ "\"type\":\"object\","
			+ "\"properties\":{"
			+ "\"orderId\":{\"type\":\"string\"},"
			+ "\"needMoreInfo\":{\"type\":\"boolean\"},"
			+ "\"question\":{\"type\":\"string\"}},"
			+ "\"required\":[\"orderId\",\"needMoreInfo\",\"question\"]}";

	/** 本轮用户消息（内容不影响判定：假模型按脚本作答） */
	private static final String TURN_MESSAGE = "帮我查一下订单明细";

	private final EnvelopeAssembler assembler = new EnvelopeAssembler();

	/** 结构化契约（形态由契约自己声明，与生产用法一致） */
	private static BusinessContract contract(String name, String schema) {
		return BusinessContract.builder(name).schema(schema).structured(true).build();
	}

	private static StructuredOutput.Validation validate(BusinessContract contract, String json) {
		return contract.structuredOutput().validate(JSONObject.parseObject(json));
	}

	/**
	 * 真实入口：分类（假模型按契约名作答）→ 结构化轮次（假模型回脚本内容）→ 收敛成信封。
	 *
	 * <p>与生产接线同一条路径，因此断言的是「入参会得到什么信封」，而不是「按规则应该是什么信封」。</p>
	 */
	private ChatEnvelope envelopeOf(BusinessContract contract, String reply) {
		BusinessRegistry registry = new BusinessRegistry();
		registry.register(contract);
		EnvelopeRuntime runtime = new EnvelopeRuntime(new ScriptedModel(contract.getName(), reply),
				new InMemoryChatMemoryStore(), registry, new Langchain4jAssistantProperties());
		String memoryId = "semantics-" + contract.getName();

		EnvelopeRuntime.Round round = runtime.decide(memoryId, TURN_MESSAGE);
		assertThat(round.getBusiness()).as("分类命中本契约").isEqualTo(contract.getName());
		assertThat(round.isStructured()).as("契约声明了结构化").isTrue();
		return runtime.structuredRound(round, memoryId, TURN_MESSAGE);
	}

	// -------------------------------------------------- 列表包一层

	@Test
	void listResultIsWrappedInAField() {
		BusinessContract contract = contract("order-list", LIST_SCHEMA);

		assertThat(validate(contract, "{\"orderId\":\"A1\",\"items\":[{\"sku\":\"S1\"},{\"sku\":\"S2\"}],\"totalCount\":2}")
				.isValid()).isTrue();

		ChatEnvelope envelope = envelopeOf(contract, "{\"orderId\":\"A1\",\"items\":[{\"sku\":\"S1\"}],\"totalCount\":1}");
		assertThat(envelope.isOk()).isTrue();
		assertThat(envelope.getResult().getJSONArray("items")).hasSize(1);
		assertThat(envelope.getResult().getString("orderId")).isEqualTo("A1");
		assertThat(envelope.getText()).isEmpty();
	}

	@Test
	void bareArrayIsRejectedBecauseResultMustBeAnObject() {
		BusinessContract contract = contract("order-list", LIST_SCHEMA);

		// 校验器层面：顶层数组不满足 "type":"object"
		assertThat(contract.structuredOutput().validate(new JSONArray()).isValid()).isFalse();
		// 收敛层面：顶层数组一律降级为失败信封（绝不把数组塞进 result）
		ChatEnvelope envelope = envelopeOf(contract, "[{\"sku\":\"S1\"}]");
		assertThat(envelope.isOk()).isFalse();
		assertThat(envelope.getResult()).isNull();
		assertThat(envelope.getText()).isEqualTo(ChatEnvelope.DEFAULT_FAILURE_TEXT);
		// 形态闸门自身也拒绝数组（顶层只收对象）
		assertThat(assembler.ensure("order-list", "1", EnvelopeAssembler.RoundMode.STRUCTURED, new JSONArray()).isOk())
				.isFalse();
	}

	@Test
	void missingWrapperFieldFailsContractValidation() {
		BusinessContract contract = contract("order-list", LIST_SCHEMA);

		StructuredOutput.Validation validation = validate(contract, "{\"orderId\":\"A1\"}");
		assertThat(validation.isValid()).isFalse();
		assertThat(validation.errors()).anyMatch(error -> error.contains("items"));

		ChatEnvelope envelope = envelopeOf(contract, "{\"orderId\":\"A1\"}");
		assertThat(envelope.isOk()).isFalse();
		assertThat(envelope.getResult()).isNull();
	}

	// -------------------------------------------------- 追问 ≠ 失败

	@Test
	void followUpIsASuccessfulBusinessResult() {
		BusinessContract contract = contract("order", FOLLOW_UP_SCHEMA);

		ChatEnvelope envelope = envelopeOf(contract,
				"{\"orderId\":\"\",\"needMoreInfo\":true,\"question\":\"请问您要查哪一单？\"}");

		// 追问是一次成功的业务结果：ok=true、追问信息在 result 内部、text 为空串
		assertThat(envelope.isOk()).isTrue();
		assertThat(envelope.getResult().getBooleanValue("needMoreInfo")).isTrue();
		assertThat(envelope.getResult().getString("question")).isEqualTo("请问您要查哪一单？");
		assertThat(envelope.getText()).isEmpty();
	}

	@Test
	void followUpIsNotGuessedByKeywords() {
		BusinessContract contract = contract("order", FOLLOW_UP_SCHEMA);

		// 文本里带“请问”但业务字段缺失：校验不通过，绝不因为像追问就放行
		assertThat(validate(contract, "{\"orderId\":\"A1\",\"needMoreInfo\":true}").isValid()).isFalse();
		assertThat(envelopeOf(contract, "{\"orderId\":\"A1\",\"needMoreInfo\":true}").isOk()).isFalse();
		// 字段齐全但不含任何“追问味”的字眼：依然是通过的业务结果
		assertThat(validate(contract, "{\"orderId\":\"A1\",\"needMoreInfo\":false,\"question\":\"\"}").isValid()).isTrue();
	}

	@Test
	void missingRequiredFieldIsAFailureNotAFollowUp() {
		BusinessContract contract = contract("order", FOLLOW_UP_SCHEMA);

		ChatEnvelope envelope = envelopeOf(contract, "{}");

		assertThat(envelope.isOk()).isFalse();
		assertThat(envelope.getResult()).isNull();
		assertThat(envelope.getText()).isEqualTo(ChatEnvelope.DEFAULT_FAILURE_TEXT);
	}

	@Test
	void validationErrorsNeverLeakIntoTheEnvelope() {
		BusinessContract contract = contract("order", FOLLOW_UP_SCHEMA);

		ChatEnvelope envelope = envelopeOf(contract, "{}");

		// 信封里不得出现校验错误原文（错误只回喂模型 / 记日志）
		assertThat(envelope.getText()).doesNotContain("required").doesNotContain("orderId");
		assertThat(envelope.toString()).doesNotContain("required");
	}

	// -------------------------------------------------- 手写规则与派生规则并用

	@Test
	void declaredRulesAndDerivedRulesBothApply() {
		BusinessContract contract = BusinessContract.builder("order")
				.schema(FOLLOW_UP_SCHEMA)
				.structured(true)
				// schema 表达不了的业务语义：追问时 orderId 允许为空，非追问时必须给出
				.rules((Rule) value -> Boolean.TRUE.equals(value.getBoolean("needMoreInfo"))
						|| !value.getString("orderId").trim().isEmpty()
								? null
								: "非追问轮次的 orderId 不能为空")
				.build();

		// 手写规则放行的追问轮次
		assertThat(validate(contract, "{\"orderId\":\"\",\"needMoreInfo\":true,\"question\":\"哪一单？\"}").isValid())
				.isTrue();
		assertThat(envelopeOf(contract, "{\"orderId\":\"\",\"needMoreInfo\":true,\"question\":\"哪一单？\"}").isOk())
				.isTrue();
		// 手写规则拦下的非追问轮次
		assertThat(validate(contract, "{\"orderId\":\"\",\"needMoreInfo\":false,\"question\":\"\"}").isValid()).isFalse();
		// 派生规则同时生效：仍要求 question 字段存在
		assertThat(validate(contract, "{\"orderId\":\"A1\",\"needMoreInfo\":false}").isValid()).isFalse();
	}

	// -------------------------------------------------- 假模型

	/**
	 * 假模型：识别分类请求（提示词以分类器固定句子开头）并按契约名作答，其余请求一律回脚本内容。
	 *
	 * <p>生成请求每次都回同一个内容，因此校验不通过时会走满契约的重试次数——正好覆盖「重试后仍失败」的收场。</p>
	 */
	private static final class ScriptedModel implements ChatLanguageModel {

		/**
		 * 与 {@code BusinessClassifier} 生成的分类提示词一致。
		 *
		 * <p>注意用 {@code contains} 而不是 {@code startsWith}：{@code StructuredOutput} 会在提示词前面再拼一段
		 * schema 指令，分类句子不在最开头。</p>
		 */
		private static final String CLASSIFIER_MARKER = "请判断下面这轮用户消息属于哪个业务";

		private final String business;

		private final String reply;

		private ScriptedModel(String business, String reply) {
			this.business = business;
			this.reply = reply;
		}

		@Override
		public Response<AiMessage> generate(List<ChatMessage> messages) {
			ChatMessage last = messages.get(messages.size() - 1);
			String text = last instanceof UserMessage ? ((UserMessage) last).text() : "";
			if (text.contains(CLASSIFIER_MARKER)) {
				return Response.from(AiMessage.from("{\"business\":\"" + business + "\"}"));
			}
			return Response.from(AiMessage.from(reply));
		}
	}
}
