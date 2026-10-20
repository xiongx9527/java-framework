package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.facts;

import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.context.ContextAwareChatMemory;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.context.ContextInjector;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.context.ContextProvider;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.context.ContextRequest;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.context.ContextStrategy;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 任务 3.3 + 3.4：事实注入来源（读路径）——守卫、渲染、预算、注入链位置
 * （事实在会话摘要之前、上下文整体在最近原文之前）与失败隔离。
 */
class FactContextProviderTest {

	private static final String MEM_ID = "t1:crm:bot:u1:s1";

	// -------------------------------------------------- 守卫（3.3）

	@Test
	void guardsReturnEmptyWithoutRecall() {
		RecordingRetriever retriever = new RecordingRetriever();
		FactContextProvider provider = provider(retriever);

		assertThat(provider.provide(null)).isEmpty();
		assertThat(provider.provide(request("t1:crm:bot", "深色主题"))).as("无显式用户段").isEmpty();
		assertThat(provider.provide(request(MEM_ID, "   "))).as("本轮输入空白").isEmpty();
		assertThat(provider.provide(request(MEM_ID, null))).as("本轮输入缺失").isEmpty();
		assertThat(retriever.calls).as("守卫命中不触发召回").isZero();
	}

	// -------------------------------------------------- 渲染与预算（3.3）

	@Test
	void rendersTitleAndBullets() {
		FactContextProvider provider = provider(fixed("用户叫张三", "用户喜欢深色主题"));

		String text = provider.provide(request(MEM_ID, "我叫什么"));

		assertThat(text).isEqualTo("【用户长期记忆】\n- 用户叫张三\n- 用户喜欢深色主题");
	}

	@Test
	void passesTrimmedQueryAndTopK() {
		RecordingRetriever retriever = new RecordingRetriever();
		FactContextProvider provider = provider(retriever);

		provider.provide(request(MEM_ID, "  深色主题  "));

		assertThat(retriever.lastQuery).isEqualTo("深色主题");
		assertThat(retriever.lastUserId).isEqualTo("u1");
		assertThat(retriever.lastTopK).isEqualTo(5);
	}

	@Test
	void truncatesToTokenBudgetAndStops() {
		FactContextProvider provider = new FactContextProvider(fixed("用户叫张三用户叫张三", "第二条不应出现"), 5, 5);

		String text = provider.provide(request(MEM_ID, "查询"));

		// 预算 5×4=20 字符：标题 8 + "\n- " 3 → 正文 9 字符后截断
		assertThat(text).isEqualTo("【用户长期记忆】\n- 用户叫张三用户叫张");
		assertThat(text).doesNotContain("第二条");
	}

	// -------------------------------------------------- 隔离（3.4）

	@Test
	void returnsEmptyWhenNoHit() {
		FactContextProvider provider = provider(fixed());

		assertThat(provider.provide(request(MEM_ID, "查询"))).isEmpty();
	}

	@Test
	void returnsEmptyWhenRetrieverFails() {
		FactRetriever failing = (query, userId, topK) -> {
			throw new IllegalStateException("mem0 不可用");
		};
		FactContextProvider provider = provider(failing);

		assertThat(provider.provide(request(MEM_ID, "查询"))).as("失败隔离为空来源").isEmpty();
	}

	@Test
	void returnsEmptyWhenRetrieverThrowsError() {
		FactRetriever failing = (query, userId, topK) -> {
			throw new AssertionError("模拟 Error 级失败");
		};
		FactContextProvider provider = provider(failing);

		assertThat(provider.provide(request(MEM_ID, "查询"))).as("Error 级失败同样按空来源隔离").isEmpty();
	}

	@Test
	void returnsEmptyWhenAllEntriesBlank() {
		FactContextProvider provider = provider(fixed((String) null, "   "));

		assertThat(provider.provide(request(MEM_ID, "查询"))).as("全空条目不出只含标题的块").isEmpty();
	}

	// -------------------------------------------------- 注入链位置（3.3）

	@Test
	void rendersBeforeSummaryInInjectorChain() {
		ContextProvider summaryProvider = request -> "【会话摘要】\n- 用户问过天气";
		ContextInjector injector = new ContextInjector(
				Arrays.asList(provider(fixed("用户叫张三")), summaryProvider),
				ContextStrategy.MEMORY_AND_ENRICHED);

		String rendered = injector.render(request(MEM_ID, "查询"));

		assertThat(rendered).isEqualTo("【用户长期记忆】\n- 用户叫张三\n\n【会话摘要】\n- 用户问过天气");
	}

	@Test
	void contextBlockFollowsPersonaAndPrecedesRecentTurns() {
		ContextProvider summaryProvider = request -> "【会话摘要】\n- 旧摘要";
		ContextInjector injector = new ContextInjector(
				Arrays.asList(provider(fixed("用户叫张三")), summaryProvider),
				ContextStrategy.MEMORY_AND_ENRICHED);
		ContextAwareChatMemory memory = new ContextAwareChatMemory(delegate(), injector,
				latest -> request(MEM_ID, latest));

		memory.add(UserMessage.from("上一问"));
		memory.add(AiMessage.from("上一答"));
		List<ChatMessage> messages = memory.messages();

		// 上下文块整体位于最近原文之前；块内事实在摘要之前
		assertThat(messages.get(0)).isInstanceOf(SystemMessage.class);
		String context = ((SystemMessage) messages.get(0)).text();
		assertThat(context.indexOf("【用户长期记忆】")).isGreaterThanOrEqualTo(0);
		assertThat(context.indexOf("【用户长期记忆】")).isLessThan(context.indexOf("【会话摘要】"));
		assertThat(messages.get(1)).isInstanceOf(UserMessage.class);
		assertThat(messages.get(2)).isInstanceOf(AiMessage.class);

		// 人设（Assistant 层 systemMessageProvider）在本块之前——组装后的完整序列
		List<ChatMessage> full = new ArrayList<ChatMessage>();
		full.add(SystemMessage.from("系统人设"));
		full.addAll(messages);
		assertThat(full.get(0)).isEqualTo(SystemMessage.from("系统人设"));
		assertThat(((SystemMessage) full.get(1)).text()).startsWith("【用户长期记忆】");
	}

	// -------------------------------------------------- 失败时其余来源与窗口仍可用（3.4）

	@Test
	void failureKeepsOtherSourcesAndWindowUsable() {
		FactRetriever failing = (query, userId, topK) -> {
			throw new IllegalStateException("mem0 不可用");
		};
		ContextProvider summaryProvider = request -> "【会话摘要】\n- 旧摘要";
		ContextInjector injector = new ContextInjector(
				Arrays.asList(provider(failing), summaryProvider), ContextStrategy.MEMORY_AND_ENRICHED);
		ContextAwareChatMemory memory = new ContextAwareChatMemory(delegate(), injector,
				latest -> request(MEM_ID, latest));

		memory.add(UserMessage.from("上一问"));
		memory.add(AiMessage.from("上一答"));
		List<ChatMessage> messages = memory.messages();

		assertThat(injector.render(request(MEM_ID, "查询"))).as("事实失败仍保留摘要来源")
				.isEqualTo("【会话摘要】\n- 旧摘要");
		assertThat(messages).hasSize(3);
		assertThat(messages.get(0)).isInstanceOf(SystemMessage.class);
		assertThat(messages.get(1)).isInstanceOf(UserMessage.class);
		assertThat(messages.get(2)).isInstanceOf(AiMessage.class);
	}

	// -------------------------------------------------- 基础设施

	private static ContextRequest request(String memoryId, String latest) {
		return new ContextRequest(memoryId, memoryId, "u1", latest);
	}

	private static FactContextProvider provider(FactRetriever retriever) {
		return new FactContextProvider(retriever, 5, 500);
	}

	private static FactRetriever fixed(String... facts) {
		return (query, userId, topK) -> new ArrayList<String>(Arrays.asList(facts));
	}

	private static ChatMemory delegate() {
		return MessageWindowChatMemory.builder()
				.id(MEM_ID)
				.maxMessages(20)
				.chatMemoryStore(new InMemoryChatMemoryStore())
				.build();
	}

	/** 记录调用的召回桩（守卫测试断言不被调用）。 */
	static final class RecordingRetriever implements FactRetriever {

		final List<String> facts = new ArrayList<String>();

		int calls;

		String lastQuery;

		String lastUserId;

		int lastTopK;

		@Override
		public List<String> retrieve(String query, String userId, int topK) {
			calls++;
			lastQuery = query;
			lastUserId = userId;
			lastTopK = topK;
			return new ArrayList<String>(facts);
		}
	}
}
