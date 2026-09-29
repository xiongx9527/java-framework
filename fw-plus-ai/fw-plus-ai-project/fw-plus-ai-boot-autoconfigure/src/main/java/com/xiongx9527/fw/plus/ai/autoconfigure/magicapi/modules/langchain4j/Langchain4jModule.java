package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.modules.langchain4j;

import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.AssistantBuilder;
import org.ssssssss.magicapi.core.annotation.MagicModule;

/**
 * LangChain4j 脚本模块入口：脚本用 {@code langchain4j.assistant()} 快速构建助手
 * （模型 + 记忆 + 系统提示 + 工具 + 检索增强），链式配置后 {@code build()} 得到 {@link SimpleAssistant}。
 *
 * <pre>{@code
 * import langchain4j;
 * import langchain4jChatModel;
 *
 * var assistant = langchain4j.assistant()
 *     .chatModel(langchain4jChatModel['qwen-main'])
 *     .streamingModel(langchain4jStreamingChatModel['qwen-stream'])
 *     .systemMessage('你是助手，回答简洁专业')
 *     .memory(20)
 *     .tools(weatherTool)
 *     .build();
 * var reply = assistant.chat('session-1', '杭州天气怎么样');
 * }</pre>
 *
 * <p>一次性对话（直连模型）与文本补全 / SSE 推流的便捷方法在 {@link SimpleAssistant} 上，
 * 模型经构造方法传入后直接调用 generate 系列：{@code generate(message)}、{@code generate(messages)}、
 * {@code generateText(prompt)}、{@code generateTextToSse(prompt)}。</p>
 */
@MagicModule("langchain4j")
public class Langchain4jModule {

	/** 快速构建助手：链式配置模型/记忆/系统提示/工具/检索，最后 {@code build()} 得到 {@link SimpleAssistant}。 */
	public AssistantBuilder assistant() {
		return new AssistantBuilder();
	}
}
