package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.envelope;

import com.alibaba.fastjson2.JSONObject;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 信封值对象契约测试：5 键恒定、成功/失败同形、取值语义、`v` 语义、不可变。
 */
class ChatEnvelopeTest {

	/** 信封键清单（顺序固定）：外层不允许出现任何业务语义字段 */
	private static final List<String> KEYS = Arrays.asList("ok", "business", "v", "result", "text");

	private static JSONObject result() {
		return JSONObject.parseObject("{\"orderId\":\"A1\"}");
	}

	// -------------------------------------------------- 四条取值路径

	@Test
	void structuredSuccessCarriesResultAndEmptyText() {
		JSONObject result = result();
		ChatEnvelope envelope = ChatEnvelope.structured("order", result, "2");

		assertThat(envelope.isOk()).isTrue();
		assertThat(envelope.getBusiness()).isEqualTo("order");
		assertThat(envelope.getVersion()).isEqualTo("2");
		assertThat(envelope.getResult()).isEqualTo(result);
		assertThat(envelope.getText()).isEmpty();
	}

	@Test
	void failureCarriesFallbackTextAndNullResult() {
		ChatEnvelope envelope = ChatEnvelope.failure("order", ChatEnvelope.DEFAULT_FAILURE_TEXT);

		assertThat(envelope.isOk()).isFalse();
		assertThat(envelope.getBusiness()).isEqualTo("order");
		assertThat(envelope.getResult()).isNull();
		assertThat(envelope.getText()).isEqualTo(ChatEnvelope.DEFAULT_FAILURE_TEXT).isNotEmpty();
	}

	@Test
	void chatRoundCarriesRawTextAndNullResult() {
		ChatEnvelope envelope = ChatEnvelope.chat("chat", "你好呀");

		assertThat(envelope.isOk()).isTrue();
		assertThat(envelope.getBusiness()).isEqualTo("chat");
		assertThat(envelope.getResult()).isNull();
		assertThat(envelope.getText()).isEqualTo("你好呀");
	}

	@Test
	void emptyReplyIsRepresentedByFailureEnvelope() {
		// 空回复不属于闲聊成功：没有正文可展示，只能降级为失败信封（真实降级在收敛件里发生）
		ChatEnvelope envelope = ChatEnvelope.failure("chat", ChatEnvelope.DEFAULT_FAILURE_TEXT);

		assertThat(envelope.isOk()).isFalse();
		assertThat(envelope.getResult()).isNull();
		assertThat(envelope.getText()).isEqualTo(ChatEnvelope.DEFAULT_FAILURE_TEXT);
	}

	// -------------------------------------------------- 键恒定

	@Test
	void allPathsShareTheSameFiveKeys() {
		List<ChatEnvelope> all = Arrays.asList(
				ChatEnvelope.structured("order", result()),
				ChatEnvelope.structured("order", result(), "2"),
				ChatEnvelope.chat("chat", "你好呀"),
				ChatEnvelope.failure("order", ChatEnvelope.DEFAULT_FAILURE_TEXT));

		for (ChatEnvelope envelope : all) {
			JSONObject json = envelope.toJsonObject();
			assertThat(json.keySet()).containsExactlyElementsOf(KEYS);
			// 失败路径的 result 键必须存在（值为 null），不得因失败而缺键
			assertThat(json).containsKey("result");
			assertThat(json).containsKey("ok");
		}
		assertThat(ChatEnvelope.keys()).containsExactlyElementsOf(KEYS);
	}

	@Test
	void failedEnvelopeSerializesWithNullResultKey() {
		JSONObject json = ChatEnvelope.failure("order", ChatEnvelope.DEFAULT_FAILURE_TEXT).toJsonObject();

		assertThat(json).containsEntry("ok", false);
		assertThat(json).containsEntry("business", "order");
		assertThat(json).containsEntry("v", "1");
		assertThat(json).containsEntry("text", ChatEnvelope.DEFAULT_FAILURE_TEXT);
		assertThat(json).containsKey("result");
		assertThat(json.get("result")).isNull();
	}

	@Test
	void jsonStringFormKeepsAllFiveKeysOnFailure() {
		// 字符串形态必须也保 5 键（fastjson2 默认会丢 null 值，这里必须显式保留）
		String json = ChatEnvelope.failure("order", ChatEnvelope.DEFAULT_FAILURE_TEXT).toJsonString();

		assertThat(json).contains("\"result\":null").contains("\"v\":\"1\"").contains("\"ok\":false");
		assertThat(JSONObject.parseObject(json).keySet()).containsExactlyElementsOf(KEYS);
	}

	@Test
	void directObjectSerializationUsesTheVKey() throws Exception {
		// 对象直序列化（帧写出侧可能直接传 ChatEnvelope）：键必须是 v，且 result 键不得消失
		ChatEnvelope failure = ChatEnvelope.failure("order", ChatEnvelope.DEFAULT_FAILURE_TEXT);

		String fastjson = com.alibaba.fastjson2.JSON.toJSONString(failure);
		assertThat(fastjson).contains("\"v\":\"1\"").doesNotContain("version").contains("\"result\":null");

		String jackson = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(failure);
		assertThat(jackson).contains("\"v\":\"1\"").doesNotContain("version").contains("\"result\":null");

		// 成功路径同样保持 v 键
		String success = com.alibaba.fastjson2.JSON.toJSONString(ChatEnvelope.structured("order", result(), "2"));
		assertThat(success).contains("\"v\":\"2\"").doesNotContain("version").contains("\"orderId\":\"A1\"");
	}

	@Test
	void versionIsTrimmed() {
		assertThat(ChatEnvelope.structured("order", result(), " 2 ").getVersion()).isEqualTo("2");
	}

	@Test
	void chatEnvelopeSerializesWithEmptyResultKey() {
		JSONObject json = ChatEnvelope.chat("chat", "你好呀").toJsonObject();

		assertThat(json).containsEntry("ok", true);
		assertThat(json).containsKey("result");
		assertThat(json.get("result")).isNull();
		assertThat(json).containsEntry("text", "你好呀");
	}

	// -------------------------------------------------- v 与默认值

	@Test
	void structuredSuccessRequiresResult() {
		assertThatThrownBy(() -> ChatEnvelope.structured("order", null, "1"))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void versionDefaultsToOneAndIsOverridable() {
		assertThat(ChatEnvelope.structured("order", result()).getVersion()).isEqualTo("1");
		assertThat(ChatEnvelope.structured("order", result(), null).getVersion()).isEqualTo("1");
		assertThat(ChatEnvelope.structured("order", result(), "").getVersion()).isEqualTo("1");
		assertThat(ChatEnvelope.chat("chat", "你好").getVersion()).isEqualTo("1");
		assertThat(ChatEnvelope.failure("order", ChatEnvelope.DEFAULT_FAILURE_TEXT).getVersion()).isEqualTo("1");
		assertThat(ChatEnvelope.structured("order", result(), "3").getVersion()).isEqualTo("3");
	}

	@Test
	void blankTextAndBusinessAreNormalized() {
		ChatEnvelope envelope = ChatEnvelope.chat(null, null);

		assertThat(envelope.getBusiness()).isEmpty();
		assertThat(envelope.getText()).isEmpty();
	}

	// -------------------------------------------------- 不可变

	@Test
	void toJsonObjectReturnsFreshCopy() {
		ChatEnvelope envelope = ChatEnvelope.structured("order", result());

		JSONObject first = envelope.toJsonObject();
		first.put("extra", "x");
		first.getJSONObject("result").put("orderId", "MUTATED");

		JSONObject second = envelope.toJsonObject();
		assertThat(second).doesNotContainKey("extra");
		assertThat(second.getJSONObject("result").getString("orderId")).isEqualTo("A1");
	}

	@Test
	void getResultDoesNotLeakInternalState() {
		ChatEnvelope envelope = ChatEnvelope.structured("order", result());

		envelope.getResult().put("orderId", "MUTATED");

		assertThat(envelope.getResult().getString("orderId")).isEqualTo("A1");
	}
}
