package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.envelope;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 收敛件契约测试：六类输入（合法 JSON / 夹带解释文字 / 截断 JSON / 纯文本 / 空回复 `[DONE]` / 异常对象）
 * 都被收敛为合法信封，且收敛过程永不抛异常。
 */
class EnvelopeAssemblerTest {

	/** 信封键清单（与 {@link ChatEnvelope} 一致） */
	private static final List<String> KEYS = Arrays.asList("ok", "business", "v", "result", "text");

	private final EnvelopeAssembler assembler = new EnvelopeAssembler();

	private static JSONObject order() {
		return JSONObject.parseObject("{\"orderId\":\"A1\"}");
	}

	/** 结构化轮次：结果对象进 result、text 为空串 */
	private static void assertStructuredSuccess(ChatEnvelope envelope, String orderId) {
		assertThat(envelope.isOk()).isTrue();
		assertThat(envelope.getResult()).isNotNull().isEqualTo(JSONObject.parseObject("{\"orderId\":\"" + orderId + "\"}"));
		assertThat(envelope.getText()).isEmpty();
	}

	/** 失败轮次：result 为 null、text 为兜底话术 */
	private static void assertFailure(ChatEnvelope envelope) {
		assertThat(envelope.isOk()).isFalse();
		assertThat(envelope.getResult()).isNull();
		assertThat(envelope.getText()).isEqualTo(ChatEnvelope.DEFAULT_FAILURE_TEXT).isNotEmpty();
	}

	// -------------------------------------------------- 结构化轮次的六类输入

	@Test
	void structuredRoundAcceptsParsedObject() {
		assertStructuredSuccess(assembler.ensure("order", "1", EnvelopeAssembler.RoundMode.STRUCTURED, order()), "A1");
	}

	@Test
	void structuredRoundStripsSurroundingTalk() {
		ChatEnvelope envelope = assembler.ensure("order", "1", EnvelopeAssembler.RoundMode.STRUCTURED,
				"好的，以下是抽取结果：\n{\"orderId\":\"A7\"}\n希望对你有帮助。");
		assertStructuredSuccess(envelope, "A7");
	}

	@Test
	void structuredRoundStripsJsonFence() {
		ChatEnvelope envelope = assembler.ensure("order", "1", EnvelopeAssembler.RoundMode.STRUCTURED,
				"```json\n{\"orderId\":\"A9\"}\n```");
		assertStructuredSuccess(envelope, "A9");
	}

	@Test
	void structuredRoundFailsOnTruncatedJson() {
		assertFailure(assembler.ensure("order", "1", EnvelopeAssembler.RoundMode.STRUCTURED,
				"{\"orderId\":\"A1\",\"quantity\":"));
		assertFailure(assembler.ensure("order", "1", EnvelopeAssembler.RoundMode.STRUCTURED, "{\"orderId\":\"A1\""));
	}

	@Test
	void structuredRoundFailsOnPlainText() {
		assertFailure(assembler.ensure("order", "1", EnvelopeAssembler.RoundMode.STRUCTURED, "我无法完成这个任务"));
	}

	@Test
	void structuredRoundFailsOnTopLevelArray() {
		// 内层 result 顶层必须是对象：数组请包一层
		assertFailure(assembler.ensure("order", "1", EnvelopeAssembler.RoundMode.STRUCTURED, "[{\"orderId\":\"A1\"}]"));
		assertFailure(assembler.ensure("order", "1", EnvelopeAssembler.RoundMode.STRUCTURED, new JSONArray()));
	}

	@Test
	void structuredRoundFailsOnEmptyReply() {
		assertFailure(assembler.ensure("order", "1", EnvelopeAssembler.RoundMode.STRUCTURED, null));
		assertFailure(assembler.ensure("order", "1", EnvelopeAssembler.RoundMode.STRUCTURED, ""));
		assertFailure(assembler.ensure("order", "1", EnvelopeAssembler.RoundMode.STRUCTURED, "   \n  "));
		// 模型空回复时既有流式路径给出 [DONE]，必须降级为失败而不是成功
		assertFailure(assembler.ensure("order", "1", EnvelopeAssembler.RoundMode.STRUCTURED,
				com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.assistant.SimpleAssistant.DONE_FALLBACK));
	}

	@Test
	void structuredRoundFailsOnThrowable() {
		assertFailure(assembler.ensure("order", "1", EnvelopeAssembler.RoundMode.STRUCTURED,
				new IllegalStateException("boom")));
	}

	@Test
	void nullRoundModeFailsInsteadOfGuessingStructured() {
		// 形态未声明时不能猜成结构化：合法 JSON 也必须是失败信封
		assertFailure(assembler.ensure("order", "1", null, "{\"orderId\":\"A1\"}"));
		assertFailure(assembler.ensure("order", "1", null, "随便聊聊"));
		// 已是信封仍然幂等（形态未声明也原样返回）
		ChatEnvelope envelope = ChatEnvelope.chat("chat", "你好呀");
		assertThat(assembler.ensure("chat", "1", null, envelope)).isSameAs(envelope);
	}

	// -------------------------------------------------- 闲聊轮次

	@Test
	void chatRoundCarriesRawText() {
		ChatEnvelope envelope = assembler.ensure("chat", "1", EnvelopeAssembler.RoundMode.CHAT, "你好呀，我是助手。");

		assertThat(envelope.isOk()).isTrue();
		assertThat(envelope.getBusiness()).isEqualTo("chat");
		assertThat(envelope.getResult()).isNull();
		assertThat(envelope.getText()).isEqualTo("你好呀，我是助手。");
	}

	@Test
	void chatRoundKeepsOriginalWhitespaceOnlyText() {
		// 闲聊原文不做任何改写（只判空）
		ChatEnvelope envelope = assembler.ensure("chat", "1", EnvelopeAssembler.RoundMode.CHAT, "  缩进保留  ");
		assertThat(envelope.getText()).isEqualTo("  缩进保留  ");
	}

	@Test
	void chatRoundFailsOnEmptyReply() {
		assertFailure(assembler.ensure("chat", "1", EnvelopeAssembler.RoundMode.CHAT, null));
		assertFailure(assembler.ensure("chat", "1", EnvelopeAssembler.RoundMode.CHAT, "   "));
		assertFailure(assembler.ensure("chat", "1", EnvelopeAssembler.RoundMode.CHAT,
				com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.assistant.SimpleAssistant.DONE_FALLBACK));
	}

	@Test
	void chatRoundFailsOnThrowable() {
		assertFailure(assembler.ensure("chat", "1", EnvelopeAssembler.RoundMode.CHAT, new RuntimeException("boom")));
	}

	@Test
	void chatRoundRejectsNonStringInput() {
		assertFailure(assembler.ensure("chat", "1", EnvelopeAssembler.RoundMode.CHAT, order()));
		assertFailure(assembler.ensure("chat", "1", EnvelopeAssembler.RoundMode.CHAT, 42));
	}

	// -------------------------------------------------- 闸门不变量

	@Test
	void everyInputConvergesToFiveKeysAndNeverThrows() {
		List<Object> nasty = Arrays.asList(
				null, "", "   ", "[DONE]", "{{{{", "[]", "[1,2]", "123", "{\"a\":1", "好的 {\"a\":1}",
				order(), new JSONArray(), "a", 42, true, new Object(),
				new IllegalStateException("boom"), new StackOverflowError("boom"));

		for (Object input : nasty) {
			for (EnvelopeAssembler.RoundMode mode : EnvelopeAssembler.RoundMode.values()) {
				ChatEnvelope envelope = assembler.ensure("biz", "2", mode, input);
				assertThat(envelope).as("input=%s mode=%s", input, mode).isNotNull();
				assertThat(envelope.toJsonObject().keySet()).containsExactlyElementsOf(KEYS);
				assertThat(envelope.getBusiness()).isEqualTo("biz");
				assertThat(envelope.getVersion()).isEqualTo("2");
				if (!envelope.isOk()) {
					assertThat(envelope.getResult()).isNull();
				}
			}
		}
	}

	@Test
	void failureTextNeverLeaksExceptionDetails() {
		ChatEnvelope envelope = assembler.ensure("order", "1", EnvelopeAssembler.RoundMode.STRUCTURED,
				new IllegalStateException("boom: 内部堆栈信息"));

		assertFailure(envelope);
		assertThat(envelope.getText()).doesNotContain("boom").doesNotContain("Exception");
	}

	@Test
	void businessAndVersionPassThroughAllPaths() {
		assertThat(assembler.ensure("order", "3", EnvelopeAssembler.RoundMode.STRUCTURED, order()).getVersion()).isEqualTo("3");
		assertThat(assembler.ensure("order", "3", EnvelopeAssembler.RoundMode.STRUCTURED, order()).getBusiness()).isEqualTo("order");
		assertThat(assembler.ensure("chat", "3", EnvelopeAssembler.RoundMode.CHAT, "你好").getVersion()).isEqualTo("3");
		assertThat(assembler.ensure("order", "3", EnvelopeAssembler.RoundMode.CHAT, "").getVersion()).isEqualTo("3");
	}

	@Test
	void structuredAndChatConvenienceEntryPointsDelegateToTheGate() {
		assertStructuredSuccess(assembler.structured("order", "1", order()), "A1");
		assertStructuredSuccess(assembler.structured("order", "1", "{\"orderId\":\"A2\"}"), "A2");
		assertFailure(assembler.structured("order", "1", "不是 JSON"));

		ChatEnvelope chat = assembler.chat("chat", "1", "你好呀");
		assertThat(chat.isOk()).isTrue();
		assertThat(chat.getText()).isEqualTo("你好呀");
		assertFailure(assembler.chat("chat", "1", "[DONE]"));
	}

	@Test
	void failureEntryPointUsesFallbackText() {
		ChatEnvelope envelope = assembler.failure("order", "1");

		assertFailure(envelope);
		assertThat(envelope.getBusiness()).isEqualTo("order");
		assertThat(envelope.getVersion()).isEqualTo("1");
	}
}
