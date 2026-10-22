package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.facts;

import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.context.ContextProvider;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.context.ContextRequest;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.chatstore.ChatMemoryIds;
import com.xiongx9527.fw.plus.common.i18n.I18nMessageResolver;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 跨会话事实来源（读路径）：按本轮用户输入从用户维度事实集合召回并渲染为固定格式的
 * 「长期记忆」块；无命中 / 无显式用户段 / 召回失败均返回空串，不影响其它来源与消息窗口。
 *
 * <p>渲染（v1 不配置化）：</p>
 * <pre>
 * 【用户长期记忆】
 * - 事实1
 * - 事实2
 * </pre>
 *
 * <p>预算：{@code maxFactsTokens} × {@value #CHARS_PER_TOKEN} 字符/token 折算为字符软上限；
 * 逐条追加，超限截断当前条目并停止。</p>
 *
 * <p>作用域守卫（D11）：仅当 {@code effectiveMemoryId} 存在显式用户段（段[3]）时召回，
 * 防匿名用户共享 {@code default} 桶。</p>
 */
@Slf4j
public class FactContextProvider implements ContextProvider {

	/** 事实段固定标题（v1 不配置化） */
	public static final String TITLE = "【用户长期记忆】";

	/** token → 字符折算比（设计 §3.6） */
	static final int CHARS_PER_TOKEN = 4;

	private static final String BULLET = "\n- ";

	private final FactRetriever retriever;

	private final int topK;

	private final int maxFactsTokens;

	public FactContextProvider(FactRetriever retriever, int topK, int maxFactsTokens) {
		if (retriever == null) {
			throw new IllegalArgumentException(
					I18nMessageResolver.resolveKey("fw.ai.FactContextProvider.retrieverRequired"));
		}
		this.retriever = retriever;
		this.topK = topK < 1 ? 1 : topK;
		this.maxFactsTokens = maxFactsTokens < 1 ? 1 : maxFactsTokens;
	}

	@Override
	public String provide(ContextRequest request) {
		if (request == null) {
			return "";
		}
		String latest = request.getLatestUserMessage();
		if (latest == null || latest.trim().isEmpty()) {
			return "";
		}
		// 作用域守卫（D11）：仅显式用户段可召回
		String userId = ChatMemoryIds.parseUserId(request.getEffectiveMemoryId(), null);
		if (userId == null || userId.trim().isEmpty()) {
			return "";
		}
		List<String> facts;
		try {
			facts = retriever.retrieve(latest.trim(), userId.trim(), topK);
		} catch (Throwable t) {
			// 失败隔离（含 Error 级）：本轮按空来源处理，其余来源与窗口不受影响
			log.warn("事实召回失败，本轮按空来源处理：{}", t.toString());
			return "";
		}
		if (facts == null || facts.isEmpty()) {
			return "";
		}
		return render(facts);
	}

	private String render(List<String> facts) {
		int budget = maxFactsTokens * CHARS_PER_TOKEN;
		StringBuilder builder = new StringBuilder(TITLE);
		int used = TITLE.length();
		boolean appended = false;
		for (String fact : facts) {
			String text = fact == null ? null : fact.trim();
			if (text == null || text.isEmpty()) {
				continue;
			}
			if (used + BULLET.length() + text.length() <= budget) {
				builder.append(BULLET).append(text);
				used += BULLET.length() + text.length();
				appended = true;
				continue;
			}
			// 预算不足：截断当前条目并停止
			int remaining = budget - used - BULLET.length();
			if (remaining > 0) {
				builder.append(BULLET).append(text, 0, Math.min(remaining, text.length()));
				appended = true;
			}
			break;
		}
		// 全部条目为空或放不下时视为无内容，避免只注入标题
		return appended ? builder.toString() : "";
	}

}
