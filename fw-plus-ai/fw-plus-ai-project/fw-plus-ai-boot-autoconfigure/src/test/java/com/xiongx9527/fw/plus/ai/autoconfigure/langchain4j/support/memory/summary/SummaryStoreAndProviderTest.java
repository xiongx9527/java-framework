package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.summary;

import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.context.ContextRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 摘要存储（首次写入 / 更新 / 按会话读取）与 {@link SummaryContextProvider} 的会话隔离。
 */
class SummaryStoreAndProviderTest {

	@Test
	void firstWriteUpdateAndRead() {
		SummaryStore store = new InMemorySummaryStore();
		assertThat(store.get("k1").isEmpty()).isTrue();

		store.put("k1", new SummaryRecord("摘要 v1", 1));
		assertThat(store.get("k1").getSummary()).isEqualTo("摘要 v1");

		store.put("k1", new SummaryRecord("摘要 v2", 2));
		assertThat(store.get("k1").getSummary()).isEqualTo("摘要 v2");
		assertThat(store.get("k1").getRefineCount()).isEqualTo(2);
	}

	@Test
	void removeClearsSessionSummary() {
		SummaryStore store = new InMemorySummaryStore();
		store.put("k1", new SummaryRecord("摘要", 1));
		store.remove("k1");
		assertThat(store.get("k1").isEmpty()).isTrue();
	}

	@Test
	void providerIsScopedBySessionKey() {
		SummaryStore store = new InMemorySummaryStore();
		store.put("t1:crm:bot:u1:s1", new SummaryRecord("会话 s1 的摘要", 1));

		SummaryContextProvider provider = new SummaryContextProvider(store);

		assertThat(provider.provide(request("t1:crm:bot:u1:s1"))).isEqualTo("会话 s1 的摘要");
		// 新会话以空摘要开始，不串用其它会话
		assertThat(provider.provide(request("t1:crm:bot:u1:s2"))).isEmpty();
	}

	private static ContextRequest request(String sessionKey) {
		return new ContextRequest(sessionKey, sessionKey, "u1", "你好");
	}
}