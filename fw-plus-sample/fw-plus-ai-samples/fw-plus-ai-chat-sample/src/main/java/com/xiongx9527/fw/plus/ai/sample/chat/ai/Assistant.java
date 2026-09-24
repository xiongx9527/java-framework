package com.xiongx9527.fw.plus.ai.sample.chat.ai;

import dev.langchain4j.service.TokenStream;

/**
 * {@code Assistant} 的声明式接口：一次对话、一次流式输出。
 *
 * <p>两条链路共用同一个 Assistant 与同一套装配（模型 + 工具 + RAG），保证行为一致。</p>
 *
 * <p>注意：本仓库锁定 LangChain4j {@code 0.35.0}，模型类型是
 * {@link dev.langchain4j.model.chat.ChatLanguageModel} 与
 * {@link dev.langchain4j.model.chat.StreamingChatLanguageModel}，不是 1.x 的
 * {@code ChatModel} / {@code StreamingChatModel}。</p>
 */
public interface Assistant {

	/**
	 * 同步对话：返回完整回复。
	 */
	String chat(String message);

	/**
	 * 流式对话：逐 token 回调。
	 */
	TokenStream stream(String message);
}
