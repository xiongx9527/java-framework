package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.assistant;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * 内层纯文本通道：签名与改造前的 {@link Assistant} 完全一致，由 {@code AiServices.build()}
 * 生成动态代理，保留 LangChain4j 原生完整管线——{@code {{context}}} 模板渲染、工具循环
 * （含 {@link GuardedToolExecutor} 缺参保护）、RAG 检索增强、会话记忆读写、系统提示。
 *
 * <p>包内可见：仅供 {@link MultimodalAssistantBridge} 在「纯文本轮」转发，以及同包测试使用，
 * 不对外暴露。业务侧一律通过 {@link Assistant} / {@link SimpleAssistant} 调用——两者对
 * 纯文本消息的转发最终都落到这里，行为与改造前逐字节一致。</p>
 *
 * <p>注解形态与原接口相同：{@code @MemoryId} / {@code @V("context")} / {@code @UserMessage}
 * 缺一不可，{@code AiServices} 在调用时校验（裸参数抛 IllegalConfigurationException）。</p>
 */
interface TextAssistant {

	/**
	 * 同步对话（纯文本）：完整原生管线。
	 *
	 * @param memoryId 会话标识，同时是会话记忆与系统提示 provider 的键
	 * @param context  上下文文本，进用户消息模板的 {@code {{context}}} 变量（无占位则忽略）
	 * @param message  用户消息（可含 {@code {{context}}} 占位）
	 */
	String chat(@MemoryId String memoryId, @V("context") String context, @UserMessage String message);

	/**
	 * 流式对话（纯文本）：完整原生管线，逐 token 回调。
	 *
	 * @param memoryId 会话标识，同时是会话记忆与系统提示 provider 的键
	 * @param context  上下文文本，进用户消息模板的 {@code {{context}}} 变量（无占位则忽略）
	 * @param message  用户消息（可含 {@code {{context}}} 占位）
	 */
	TokenStream stream(@MemoryId String memoryId, @V("context") String context, @UserMessage String message);
}
