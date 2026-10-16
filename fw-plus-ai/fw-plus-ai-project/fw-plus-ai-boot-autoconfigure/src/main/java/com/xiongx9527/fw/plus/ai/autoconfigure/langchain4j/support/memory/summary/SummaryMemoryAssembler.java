package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.summary;

import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.assistant.AssistantBuilder;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.context.ContextStrategy;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.envelope.Langchain4jAssistantProperties;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.session.SessionIdResolver;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.session.SessionIdResolvers;

import dev.langchain4j.model.Tokenizer;

/**
 * 会话记忆装配器：把 {@code fw.plus.ai.langchain4j.assistant.session.*} 配置落到
 * {@link AssistantBuilder}（会话解析器 / 上下文策略 / 摘要来源与维护钩子）。
 *
 * <p>这是把「类已建」接进「运行时装配」的收口点——没有它，{@code session.*} /
 * {@code session.summary.*} 配置全部是死配置。装配规则：</p>
 * <ul>
 *   <li>{@code session.enabled=false}：完全不动，保持既有非会话行为；</li>
 *   <li>{@code session.session-id-resolver}：无自定义 {@link SessionIdResolver} 时按配置名解析；</li>
 *   <li>{@code session.context-strategy}：解析为 {@link ContextStrategy}；</li>
 *   <li>{@code session.summary.enabled=true}：装配 {@link SummaryContextProvider}（读来源）
 *       与 {@link SummaryMaintenance}（写时维护 + 读时兜底），摘要模型按
 *       「自定义 {@link SummaryModel} Bean → {@code summarize-model} 指定模型 key/FQCN →
 *       无（抽取式回退，不调 LLM）」解析。</li>
 * </ul>
 */
public final class SummaryMemoryAssembler {

	private SummaryMemoryAssembler() {
	}

	/**
	 * 把会话/摘要配置装配到 builder。
	 *
	 * @param builder         目标装配器
	 * @param properties      会话/摘要配置；null 时不做任何装配
	 * @param summaryStore    摘要存储；null 时用进程内 {@link InMemorySummaryStore}
	 * @param summaryModel    摘要模型（已由调用方按 {@code summarize-model} 解析）；null 时走抽取式回退
	 * @param sessionResolver 自定义会话解析器；null 时按 {@code session.session-id-resolver} 解析
	 * @param tokenizer       token 计数器（通常 {@code MagicDynamicTokenizer}）；可为 null（按 4 字符/token 折算）
	 * @return 是否装配了摘要来源
	 */
	public static boolean apply(AssistantBuilder builder, Langchain4jAssistantProperties properties,
			SummaryStore summaryStore, SummaryModel summaryModel, SessionIdResolver sessionResolver,
			Tokenizer tokenizer) {
		if (builder == null || properties == null) {
			return false;
		}
		Langchain4jAssistantProperties.Session session = properties.getSession();
		if (session == null || !session.isEnabled()) {
			return false;
		}
		builder.sessionResolver(sessionResolver != null
				? sessionResolver
				: SessionIdResolvers.resolve(session.getSessionIdResolver(), null));
		builder.contextStrategy(ContextStrategy.fromConfig(session.getContextStrategy()));

		Langchain4jAssistantProperties.Session.Summary summary = session.getSummary();
		if (summary == null || !summary.isEnabled()) {
			return false;
		}
		SummaryStore store = summaryStore == null ? new InMemorySummaryStore() : summaryStore;
		// summarize-model 留空 → model 为 null → 各策略走抽取式回退（不调 LLM，见属性类契约）
		Summarizer summarizer = Summarizers.resolveOrDefault(summary.getStrategy(), summaryModel);
		Summarizer rebuildSummarizer = Summarizers.resolve(Summarizers.REBUILD, summaryModel);
		RollingSummaryEngine engine = new RollingSummaryEngine(store, summarizer, rebuildSummarizer, tokenizer,
				summary.getTriggerTokens(), summary.getKeepTokens(), summary.getMaxSummaryTokens(),
				summary.getRebuildEvery());
		builder.contextProvider(new SummaryContextProvider(store));
		builder.contextMaintenance(new SummaryMaintenance(engine, session.getWindowSize()));
		return true;
	}

}
