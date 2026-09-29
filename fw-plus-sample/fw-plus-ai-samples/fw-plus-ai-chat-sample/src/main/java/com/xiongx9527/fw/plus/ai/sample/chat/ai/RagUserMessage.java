package com.xiongx9527.fw.plus.ai.sample.chat.ai;

import dev.langchain4j.data.message.UserMessage;

/**
 * 带「用户原话」的 user message——{@code RagContentInjector} 的产物。
 *
 * <p>框架还会用 {@code UserMessage.from(text)} 无条件重建 injector 的返回值
 * （{@code DefaultAiServices} :150 与 :323-332），所以本类到不了 memory 层；
 * {@link EphemeralChatMemory} 靠文本锚点拆分。保留它是为了让「原话」在对象上显式存在。</p>
 */
public class RagUserMessage extends UserMessage {

	private final String userText;

	private RagUserMessage(String injectedText, String userText) {
		super(injectedText);
		this.userText = userText;
	}

	/**
	 * @param injectedText 完整文本（原话 + 分隔串 + 检索内容），即模型看到的 {@link #text()}
	 * @param userText     注入前的用户原话
	 */
	static RagUserMessage of(String injectedText, String userText) {
		return new RagUserMessage(injectedText, userText);
	}

	/** 用户原话——只有这一段该进会话记忆。 */
	public String userText() {
		return userText;
	}
}
