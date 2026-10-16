package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.summary;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.Tokenizer;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * 一级滚动会话摘要引擎：写时滚动维护、双阈值触发。
 *
 * <ul>
 *   <li>{@code trigger-tokens}：会话历史（窗口外原文 + 既有摘要）累计 token 超过该值才触发摘要；</li>
 *   <li>{@code keep-tokens}：摘要压缩目标（同时也是固有限制：压缩后应不超过它）；</li>
 *   <li>{@code rebuild-every}：每连续 {@code rebuild-every} 次增量精炼插入一次全量重摘，防漂移；</li>
 * </ul>
 *
 * <p>写入路径调用 {@link #maintain(String, List)} 抢在窗口驱逐前更新；读取路径的兜底由
 * {@link SummaryContextProvider} 直读已维护的摘要实现。摘要只压缩进上下文，<b>不删除原文</b>。</p>
 *
 * <p>token 计数复用注入的 {@link Tokenizer}（生产环境用 {@code MagicDynamicTokenizer}）；
 * 未注入时按每 token 约 4 字符折算。</p>
 */
@Slf4j
public class RollingSummaryEngine {

	private static final int CHARS_PER_TOKEN = 4;

	private final SummaryStore store;

	private final Summarizer summarizer;

	private final Summarizer rebuildSummarizer;

	private final Tokenizer tokenizer;

	private final int triggerTokens;

	private final int keepTokens;

	private final int maxSummaryTokens;

	private final int rebuildEvery;

	public RollingSummaryEngine(SummaryStore store, Summarizer summarizer, Summarizer rebuildSummarizer,
			Tokenizer tokenizer, int triggerTokens, int keepTokens, int maxSummaryTokens, int rebuildEvery) {
		this.store = store;
		this.summarizer = summarizer;
		this.rebuildSummarizer = rebuildSummarizer;
		this.tokenizer = tokenizer;
		this.triggerTokens = triggerTokens;
		this.keepTokens = keepTokens;
		this.maxSummaryTokens = maxSummaryTokens;
		this.rebuildEvery = rebuildEvery;
	}

	/**
	 * 写路径滚动维护：窗口外历史累计超阈值时更新该会话摘要。
	 *
	 * @param sessionKey           会话键（摘要锚点）
	 * @param windowExternalHistory 已滚出最近窗口的历史原文
	 * @return 是否执行了摘要更新
	 */
	public boolean maintain(String sessionKey, List<ChatMessage> windowExternalHistory) {
		if (sessionKey == null || sessionKey.trim().isEmpty()) {
			return false;
		}
		List<ChatMessage> external = windowExternalHistory == null
				? java.util.Collections.<ChatMessage>emptyList()
				: windowExternalHistory;
		SummaryRecord record = store.get(sessionKey);
		String historyText = render(external);
		int tokens = countTokens(record.getSummary()) + countTokens(historyText);
		if (triggerTokens > 0 && tokens <= triggerTokens) {
			return false;
		}
		// 写入水位：从后向前定位上次已并入摘要的锚点，其后的才是新增轮次。
		// 锚点缺失（首轮 / 被窗口驱逐）时从头折叠，宁可重摘一次也不漏轮次。
		int from = 0;
		String anchor = record.getLastCovered();
		if (anchor != null && !anchor.isEmpty()) {
			for (int i = external.size() - 1; i >= 0; i--) {
				if (anchor.equals(ChatMessageIdentities.of(external.get(i)))) {
					from = i + 1;
					break;
				}
			}
		}
		List<ChatMessage> newTurns = external.subList(from, external.size());
		if (newTurns.isEmpty()) {
			// 无新增窗口外历史：避免对同一批历史反复精炼
			return false;
		}
		boolean rebuildDue = rebuildEvery > 0 && (record.getRefineCount() + 1) >= rebuildEvery;
		Summarizer chosen = rebuildDue && rebuildSummarizer != null ? rebuildSummarizer : summarizer;
		int budget = maxSummaryTokens > 0 ? maxSummaryTokens : keepTokens;
		SummarizeRequest request = new SummarizeRequest(record.getSummary(),
				splitTurns(newTurns), historyText, budget);
		try {
			String summary = chosen.summarize(request);
			String bounded = truncate(summary, budget);
			String newAnchor = ChatMessageIdentities.of(external.get(external.size() - 1));
			store.put(sessionKey, rebuildDue
					? record.rebuilt(bounded, newAnchor)
					: record.refined(bounded, newAnchor));
			return true;
		} catch (Exception e) {
			// 摘要失败：本轮不动摘要，主对话不受影响，下轮重试
			log.warn("会话 {} 摘要生成失败，本轮退化为最近消息原文：{}", sessionKey, e.getMessage());
			return false;
		}
	}

	/** 读取路径兜底：返回当前会话摘要（无则空串）；摘要缺失时可由调用方决定是否补触发。 */
	public String currentSummary(String sessionKey) {
		return sessionKey == null ? "" : store.get(sessionKey).getSummary();
	}

	/** 是否已达触发阈值（读路径判断是否需要兜底补触发） */
	public boolean exceedsTrigger(String sessionKey, List<ChatMessage> windowExternalHistory) {
		if (triggerTokens <= 0) {
			return false;
		}
		int tokens = countTokens(store.get(sessionKey).getSummary())
				+ countTokens(render(windowExternalHistory));
		return tokens > triggerTokens;
	}

	private int countTokens(String text) {
		if (text == null || text.isEmpty()) {
			return 0;
		}
		if (tokenizer != null) {
			return tokenizer.estimateTokenCountInText(text);
		}
		return (text.length() + CHARS_PER_TOKEN - 1) / CHARS_PER_TOKEN;
	}

	private String truncate(String summary, int budget) {
		if (summary == null) {
			return "";
		}
		String trimmed = summary.trim();
		if (budget <= 0) {
			return trimmed;
		}
		if (tokenizer != null) {
			if (tokenizer.estimateTokenCountInText(trimmed) <= budget) {
				return trimmed;
			}
		} else if (trimmed.length() <= budget * CHARS_PER_TOKEN) {
			return trimmed;
		}
		int maxChars = budget * CHARS_PER_TOKEN;
		return trimmed.length() <= maxChars ? trimmed : trimmed.substring(0, maxChars);
	}

	private static String render(List<ChatMessage> messages) {
		if (messages == null || messages.isEmpty()) {
			return "";
		}
		StringBuilder builder = new StringBuilder();
		for (ChatMessage message : messages) {
			String text = textOf(message);
			if (text == null || text.trim().isEmpty()) {
				continue;
			}
			if (builder.length() > 0) {
				builder.append('\n');
			}
			builder.append(text.trim());
		}
		return builder.toString();
	}

	private static List<String> splitTurns(List<ChatMessage> messages) {
		List<String> turns = new ArrayList<String>();
		if (messages == null) {
			return turns;
		}
		for (ChatMessage message : messages) {
			String text = textOf(message);
			if (text != null && !text.trim().isEmpty()) {
				turns.add(text.trim());
			}
		}
		return turns;
	}

	private static String textOf(ChatMessage message) {
		if (message instanceof UserMessage) {
			return ((UserMessage) message).singleText();
		}
		if (message instanceof AiMessage) {
			return ((AiMessage) message).text();
		}
		if (message instanceof SystemMessage) {
			return ((SystemMessage) message).text();
		}
		return message == null ? null : message.toString();
	}

}