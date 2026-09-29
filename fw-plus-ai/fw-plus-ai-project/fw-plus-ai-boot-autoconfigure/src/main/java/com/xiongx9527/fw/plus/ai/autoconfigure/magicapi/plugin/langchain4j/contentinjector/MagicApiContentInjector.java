package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.contentinjector;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.contentinjector.module.Langchain4jContentInjectorModule;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.injector.ContentInjector;

import java.util.List;

/**
 * Magic-API 脚本托管的内容注入器：把一个 {@code langchain4j-content-injector} 资源（{@code .ms} 脚本）
 * 包装成 LangChain4j {@link ContentInjector}，检索结果注入策略完全由脚本决定。
 *
 * <p>脚本上下文（由 {@link Langchain4jContentInjectorModule} 注入）：</p>
 * <ul>
 *   <li>{@code contents}：检索到的 {@code List<Content>}（原对象，可调 {@code textSegment().text()}）；</li>
 *   <li>{@code texts}：上面各条正文的 {@code List<String>} 便捷视图；</li>
 *   <li>{@code chatMessage}：原始消息对象；{@code query}：原始用户消息文本（非 UserMessage 时为 null）。</li>
 * </ul>
 *
 * <p>脚本返回值：{@code String} → 作为注入后的用户消息文本（{@code UserMessage.from(text)}）；
 * {@code ChatMessage} → 原样返回；{@code null} → 不注入，返回原始消息。</p>
 */
public class MagicApiContentInjector implements ContentInjector {

	private final Langchain4jContentInjectorModule module;

	private final String path;

	public MagicApiContentInjector(Langchain4jContentInjectorModule module, String path) {
		this.module = module;
		this.path = path;
	}

	@Override
	public ChatMessage inject(List<Content> contents, ChatMessage chatMessage) {
		return module.inject(path, contents, chatMessage);
	}

	@Override
	public UserMessage inject(List<Content> contents, UserMessage userMessage) {
		return (UserMessage) module.inject(path, contents, userMessage);
	}
}
