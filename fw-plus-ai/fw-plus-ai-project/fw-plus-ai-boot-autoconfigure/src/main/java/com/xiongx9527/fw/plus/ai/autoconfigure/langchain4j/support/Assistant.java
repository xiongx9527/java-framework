package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * {@code Assistant} 的声明式接口：一次对话、一次流式输出。
 *
 * <p>两条链路共用同一个 Assistant 与同一套装配（模型 + 工具 + RAG），保证行为一致。</p>
 *
 * <p>注意：本仓库锁定 LangChain4j {@code 0.35.0}，模型类型是
 * {@link dev.langchain4j.model.chat.ChatLanguageModel} 与
 * {@link dev.langchain4j.model.chat.StreamingChatLanguageModel}，不是 1.x 的
 * {@code ChatModel} / {@code StreamingChatModel}。</p>
 *
 * <p>第一个参数带 {@link MemoryId}：LangChain4j 每次调用会把该实参作为 memoryId 传给
 * {@code AiServices.systemMessageProvider}，系统提示因此可以按会话定制。该参数
 * <b>不可传 null</b>——{@code DefaultAiServices.findMemoryId} 会直接抛
 * {@code IllegalArgumentException}，由调用方兜底为 {@code default}。</p>
 *
 * <p>每个参数都必须带注解（{@code @V} / {@code @UserMessage} / {@code @UserName} /
 * {@code @MemoryId} 至少其一），裸参数会在调用时才抛 IllegalConfigurationException。</p>
 *
 * <p>系统提示（人设 + profile）走 {@code systemMessageProvider}，由 {@link SystemPromptBuilder}
 * 拼装；检索资料不在其中——它靠 query，provider 入参只有 memoryId，改由
 * {@code retrievalAugmentor} 拼进 user message。</p>
 */
public interface Assistant {

	/**
	 * 同步对话：返回完整回复。
	 *
	 * @param memoryId 会话标识，同时是会话记忆与个人资料的键
	 * @param context  上下文，用于定制系统提示
	 * @param message   用户消息
	 */
	String chat(@MemoryId String memoryId, @V("context") String context, @UserMessage String message);

	/**
	 * 流式对话：逐 token 回调。
	 *
	 * @param memoryId 会话标识，同时是会话记忆与个人资料的键
	 * @param context  上下文，用于定制系统提示
	 * @param message   用户消息
	 */
	TokenStream stream(@MemoryId String memoryId, @V("context") String context, @UserMessage String message);
}
