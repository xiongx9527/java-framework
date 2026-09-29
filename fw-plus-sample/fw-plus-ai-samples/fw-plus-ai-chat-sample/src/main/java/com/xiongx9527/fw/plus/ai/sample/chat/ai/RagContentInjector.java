package com.xiongx9527.fw.plus.ai.sample.chat.ai;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.injector.ContentInjector;
import dev.langchain4j.rag.content.injector.DefaultContentInjector;

import java.util.List;

/**
 * 在默认注入行为之上，把「用户原话」随消息带下去的 {@link ContentInjector}。
 *
 * <p>文本渲染仍委托 {@link DefaultContentInjector}，但**模板由本类自声明**——分隔串是私有常量，
 * 不镜像框架默认值（那是上游实现细节，一改就静默失配）。{@link EphemeralChatMemory} 靠同一个
 * 常量把检索内容从 user message 里剥出去，只让原话进会话记忆。</p>
 *
 * <pre>
 * {{userMessage}}\n\nAnswer using the following information:\n{{contents}}
 * </pre>
 *
 * <p>框架随后会用 {@code UserMessage.from(text)} 重建消息（{@code DefaultAiServices} :150），
 * 本类产出的 {@link RagUserMessage} 类型会丢失，但文本里的分隔串仍在——所以剥离靠锚点而非类型。</p>
 */
class RagContentInjector implements ContentInjector {

	/** 用户原话与检索内容的分隔串：模板与剥离共用同一处定义。 */
	static final String SEPARATOR = "\n\nAnswer using the following information:\n";

	private final ContentInjector delegate =
			new DefaultContentInjector(PromptTemplate.from("{{userMessage}}" + SEPARATOR + "{{contents}}"));

	@Override
	public ChatMessage inject(List<Content> contents, ChatMessage chatMessage) {
		// 非 user 消息直接放过：接口的 ChatMessage 重载是 default 实现，内部会 checkcast UserMessage
		if (!(chatMessage instanceof UserMessage)) {
			return chatMessage;
		}
		UserMessage userMessage = (UserMessage) chatMessage;
		ChatMessage injected = delegate.inject(contents, userMessage);
		if (!(injected instanceof UserMessage)) {
			throw new IllegalStateException("注入结果应为 UserMessage，实际：" + injected.type());
		}
		return RagUserMessage.of(((UserMessage) injected).text(), userMessage.text());
	}

	@Override
	public UserMessage inject(List<Content> contents, UserMessage userMessage) {
		return (UserMessage) inject(contents, (ChatMessage) userMessage);
	}
}
