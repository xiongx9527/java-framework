package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.summary;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 一级滚动摘要：双阈值（未达不触发、超出后压缩）+ rebuild-every 定期全量重摘。
 */
class RollingSummaryEngineTest {

	private static List<ChatMessage> history(String... texts) {
		List<ChatMessage> messages = new ArrayList<ChatMessage>();
		for (String text : texts) {
			messages.add(UserMessage.from(text));
			messages.add(AiMessage.from("回复：" + text));
		}
		return messages;
	}

	@Test
	void belowThresholdDoesNotTrigger() {
		SummaryStore store = new InMemorySummaryStore();
		RollingSummaryEngine engine = engine(store, 1000, 500, 10);

		boolean changed = engine.maintain("k1", history("短消息"));

		assertThat(changed).isFalse();
		assertThat(store.get("k1").getSummary()).isEmpty();
	}

	@Test
	void aboveThresholdCompressesWithinKeepTokens() {
		SummaryStore store = new InMemorySummaryStore();
		// trigger 很小，保证触发；keep=50 token → 约 200 字符上限
		RollingSummaryEngine engine = engine(store, 10, 50, 0);

		boolean changed = engine.maintain("k1", history(repeat("这是一段很长的历史内容", 40)));

		assertThat(changed).isTrue();
		SummaryRecord record = store.get("k1");
		assertThat(record.getSummary()).isNotBlank();
		assertThat(record.getSummary().length()).isLessThanOrEqualTo(50 * 4);
	}

	@Test
	void rebuildEveryInsertsFullRebuild() {
		SummaryStore store = new InMemorySummaryStore();
		RollingSummaryEngine engine = engine(store, 1, 100, 3);

		// 连续增量精炼到 rebuild-every=3：第 3 次应走全量重摘并把计数归零
		engine.maintain("k1", history("第一轮"));
		assertThat(store.get("k1").getRefineCount()).isEqualTo(1);
		engine.maintain("k1", history("第二轮"));
		assertThat(store.get("k1").getRefineCount()).isEqualTo(2);
		engine.maintain("k1", history("第三轮"));
		assertThat(store.get("k1").getRefineCount()).isEqualTo(0);
	}

	@Test
	void summaryFailureDoesNotThrow() {
		SummaryStore store = new InMemorySummaryStore();
		Summarizer boom = new Summarizer() {
			@Override
			public String name() {
				return "boom";
			}

			@Override
			public String summarize(SummarizeRequest request) {
				throw new IllegalStateException("模型不可用");
			}
		};
		RollingSummaryEngine engine = new RollingSummaryEngine(store, boom, boom, null, 1, 100, 100, 0);

		assertThat(engine.maintain("k1", history("任意"))).isFalse();
		assertThat(store.get("k1").getSummary()).isEmpty();
	}

	private static RollingSummaryEngine engine(SummaryStore store, int trigger, int keep, int rebuildEvery) {
		SummaryModel model = null;
		return new RollingSummaryEngine(store,
				new Summarizers.RefineSummarizer(model),
				new Summarizers.RebuildSummarizer(model),
				null, trigger, keep, keep, rebuildEvery);
	}

	private static String repeat(String text, int times) {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < times; i++) {
			builder.append(text);
		}
		return builder.toString();
	}
}