package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.chatstore;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageJsonCodec;
import dev.langchain4j.data.message.GsonChatMessageJsonCodec;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * 对真实 mem0 服务的写读往返联调（路径 / 认证 / 去重 / append-only 全链路实测）。
 *
 * <p>通过 {@code MEM0_BASE_URL} + {@code MEM0_API_KEY}（系统属性或环境变量）提供
 * 服务地址与 API Key 时执行，否则整体跳过；测试数据使用独立 run_id 并在结束时清理。</p>
 */
class Mem0ChatMemoryStoreRealServerTest {

	private static final ChatMessageJsonCodec CODEC = new GsonChatMessageJsonCodec();

	@Test
	void realServerWriteReadRoundTrip() {
		String baseUrl = config("MEM0_BASE_URL");
		String apiKey = config("MEM0_API_KEY");
		assumeTrue(baseUrl != null && !baseUrl.isEmpty(), "未提供 MEM0_BASE_URL，跳过真实 mem0 联调");
		assumeTrue(apiKey != null && !apiKey.isEmpty(), "未提供 MEM0_API_KEY，跳过真实 mem0 联调");

		String memId = "it:roundtrip:" + System.currentTimeMillis();
		Mem0ChatMemoryStore store = new Mem0ChatMemoryStore(() -> "it", "it-app",
				baseUrl, apiKey, Duration.ofSeconds(5), Duration.ofSeconds(15));
		try {
			// 1. 写读往返（四段 memId → 三参过滤读回）
			List<ChatMessage> messages = Arrays.asList(
					SystemMessage.from("你是助手"),
					UserMessage.from("联调问题"),
					AiMessage.from("联调回答"));
			store.updateMessages(memId, messages);
			assertThat(jsonOf(store.getMessages(memId))).isEqualTo(jsonOf(messages));

			// 2. 原位 upsert + append-only：窗口收缩不删超窗历史
			store.updateMessages(memId, Arrays.asList(
					UserMessage.from("联调问题（改）"),
					AiMessage.from("联调回答")));
			assertThat(jsonOf(store.getMessages(memId))).isEqualTo(jsonOf(Arrays.asList(
					UserMessage.from("联调问题（改）"),
					AiMessage.from("联调回答"),
					AiMessage.from("联调回答"))));

			// 3. 主动清空历史
			store.deleteMessages(memId);
			assertThat(store.getMessages(memId)).isEmpty();
		} finally {
			safeDelete(store, memId);
		}
	}

	private static void safeDelete(Mem0ChatMemoryStore store, String memId) {
		try {
			store.deleteMessages(memId);
		} catch (RuntimeException ignore) {
			// 清理失败不掩盖断言结果
		}
	}

	private static String config(String name) {
		String value = System.getProperty(name);
		return value != null ? value : System.getenv(name);
	}

	private static String jsonOf(List<ChatMessage> messages) {
		return CODEC.messagesToJson(messages);
	}
}
