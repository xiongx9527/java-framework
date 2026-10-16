package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.summary;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;

/**
 * 基于 {@link ChatModel} 的 {@link SummaryModel} 生产实现：把摘要提示词交给模型
 * （通常配置为比主模型便宜的 {@code summarize-model}，未配置时按设计回退主模型），返回摘要文本。
 *
 * <p>模型侧异常向上抛出，由 {@link RollingSummaryEngine} 统一吞掉并记日志——摘要失败不影响主对话。</p>
 */
public class ChatLanguageModelSummaryModel implements SummaryModel {

	private final ChatModel model;

	public ChatLanguageModelSummaryModel(ChatModel model) {
		if (model == null) {
			throw new IllegalArgumentException("ChatLanguageModelSummaryModel: model 不能为空");
		}
		this.model = model;
	}

	@Override
	public String summarize(String prompt) {
		String summary = model.chat(ChatRequest.builder().messages(UserMessage.from(prompt)).build())
				.aiMessage().text();
		return summary == null ? "" : summary;
	}

}
