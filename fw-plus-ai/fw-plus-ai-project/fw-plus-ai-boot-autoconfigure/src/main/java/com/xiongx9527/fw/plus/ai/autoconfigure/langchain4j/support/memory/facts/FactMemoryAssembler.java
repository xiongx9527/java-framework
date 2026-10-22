package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.facts;

import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.assistant.AssistantBuilder;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.envelope.Langchain4jAssistantProperties;
import lombok.extern.slf4j.Slf4j;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.scoring.ScoringModel;

/**
 * 跨会话事实记忆装配器：把 {@code session.facts.*} 配置落到 {@link AssistantBuilder}
 * （事实来源 {@link FactContextProvider} + 写路径钩子 {@link FactMaintenance}）。
 *
 * <p>这是把「类已建」接进「运行时装配」的收口点。装配规则：</p>
 * <ul>
 *   <li>{@code session.facts.enabled=false}（默认）：完全不动，行为与会话记忆基线一致；</li>
 *   <li>{@code enabled=true} 但事实存储缺席（base-url 缺失且无自定义 {@link FactStore} Bean）：
 *       记 WARN 后跳过，不中断既有装配；</li>
 *   <li>{@code enabled=true} 且存储可用：按 {@code extractor} / {@code retriever} 解析策略
 *       （未知名启动快速失败），注册事实来源与维护钩子。</li>
 * </ul>
 *
 * <p>注册顺序即注入顺序：调用方必须<b>先</b>本装配、<b>后</b> {@code SummaryMemoryAssembler.apply}，
 * 保证渲染顺序为 人设 → 事实 → 摘要 → 最近 N 条原文。</p>
 */
@Slf4j
public final class FactMemoryAssembler {

	private FactMemoryAssembler() {
	}

	/**
	 * 把跨会话事实配置装配到 builder。
	 *
	 * @param builder      目标装配器
	 * @param properties   事实配置；null 时不做任何装配
	 * @param factStore    事实存储（由调用方按 base-url 或自定义 Bean 解析）；null 时跳过（记 WARN）
	 * @param extractModel 抽取模型（{@code llm}/{@code hybrid} 用）；可为 null（llm 跳过 / hybrid 退化为 rule）
	 * @param rerankModel  重排模型（{@code rerank} 用）；可为 null（退回本地关键词重叠重排）
	 * @return 是否装配了事实来源
	 */
	public static boolean apply(AssistantBuilder builder, Langchain4jAssistantProperties properties,
			FactStore factStore, ChatLanguageModel extractModel, ScoringModel rerankModel) {
		if (builder == null || properties == null) {
			return false;
		}
		Langchain4jAssistantProperties.Session session = properties.getSession();
		Langchain4jAssistantProperties.Session.Facts facts = session == null ? null : session.getFacts();
		if (facts == null || !facts.isEnabled()) {
			return false;
		}
		if (factStore == null) {
			log.warn("session.facts.enabled=true，但无事实存储（session.facts.base-url 未配置且无自定义 "
					+ "FactStore Bean）：事实通道跳过，既有会话记忆不受影响");
			return false;
		}
		FactExtractor extractor = FactExtractors.resolveOrThrow(facts.getExtractor(), factStore, extractModel);
		FactRetriever retriever = FactRetrievers.resolveOrThrow(facts.getRetriever(), factStore, rerankModel);
		builder.contextProvider(new FactContextProvider(retriever, facts.getTopK(), facts.getMaxFactsTokens()));
		builder.contextMaintenance(new FactMaintenance(extractor, FactWriteMode.fromConfig(facts.getWriteMode()),
				facts.getBatchSize(), facts.isExtractAsync(), new FactTurnBuffer()));
		return true;
	}

}
