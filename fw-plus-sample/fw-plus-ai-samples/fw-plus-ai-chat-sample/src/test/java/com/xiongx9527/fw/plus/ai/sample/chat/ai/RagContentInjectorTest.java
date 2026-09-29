package com.xiongx9527.fw.plus.ai.sample.chat.ai;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.rag.content.Content;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link RagContentInjector}：模板产出的文本必须含 SEPARATOR——EphemeralChatMemory 靠它剥离。
 */
class RagContentInjectorTest {

	private final RagContentInjector injector = new RagContentInjector();

	private static UserMessage user(String text) {
		return UserMessage.from(text);
	}

	@Test
	void injectedTextContainsSeparatorAndOriginalTextFirst() {
		ChatMessage injected = injector.inject(
				Collections.singletonList(Content.from("杭州夏季炎热潮湿")),
				(ChatMessage) user("杭州夏天热吗"));

		String text = ((UserMessage) injected).text();
		int anchor = text.indexOf(RagContentInjector.SEPARATOR);

		assertTrue(anchor >= 0, "注入结果必须含分隔串，实际：" + text);
		assertEquals("杭州夏天热吗", text.substring(0, anchor));
		assertTrue(text.substring(anchor).contains("杭州夏季炎热潮湿"), "分隔串之后应是检索内容");
	}

	@Test
	void carriesOriginalUserTextOnMessageObject() {
		ChatMessage injected = injector.inject(
				Collections.singletonList(Content.from("资料")),
				(ChatMessage) user("我的问题"));

		// 框架随后会重建消息使类型丢失，但对象路径仍应可用
		assertTrue(injected instanceof RagUserMessage);
		assertEquals("我的问题", ((RagUserMessage) injected).userText());
	}

	@Test
	void nonUserMessageIsNotWrapped() {
		ChatMessage system = SystemMessage.from("系统提示");

		ChatMessage injected = injector.inject(
				Collections.singletonList(Content.from("资料")), system);

		assertTrue(!(injected instanceof RagUserMessage), "非 user 消息不应包装");
		assertTrue(injected instanceof SystemMessage);
	}
}
