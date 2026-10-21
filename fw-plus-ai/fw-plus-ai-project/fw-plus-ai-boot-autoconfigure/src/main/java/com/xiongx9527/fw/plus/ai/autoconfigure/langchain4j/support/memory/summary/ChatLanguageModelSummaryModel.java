package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.summary;

import com.xiongx9527.fw.plus.common.i18n.I18nMessageResolver;
import dev.langchain4j.model.chat.ChatLanguageModel;

/**
 * 基于 {@link ChatLanguageModel} 的 {@link SummaryModel} 生产实现：把摘要提示词交给模型
 * （通常配置为比主模型便宜的 {@code summarize-model}，未配置时按设计回退主模型），返回摘要文本。
 *
 * <p>模型侧异常向上抛出，由 {@link RollingSummaryEngine} 统一吞掉并记日志——摘要失败不影响主对话。</p>
 */
public class ChatLanguageModelSummaryModel implements SummaryModel {

	private final ChatLanguageModel model;

	public ChatLanguageModelSummaryModel(ChatLanguageModel model) {
		if (model == null) {
			throw new IllegalArgumentException(
					I18nMessageResolver.resolveKey("fw.ai.ChatLanguageModelSummaryModel.modelRequired"));
		}
		this.model = model;
	}

	@Override
	public String summarize(String prompt) {
		String summary = model.generate(prompt);
		return summary == null ? "" : summary;
	}

}
