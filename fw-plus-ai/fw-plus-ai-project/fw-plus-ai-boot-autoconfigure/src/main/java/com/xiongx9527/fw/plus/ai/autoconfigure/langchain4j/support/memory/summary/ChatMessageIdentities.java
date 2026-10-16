package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.summary;

import dev.langchain4j.data.message.ChatMessage;

/**
 * 消息身份：把一条 {@link ChatMessage} 折算成可持久化、可比较的稳定字符串，
 * 用作摘要写入水的位锚点（见 {@link SummaryRecord#getLastCovered()}）。
 *
 * <p>不依赖对象引用：窗口滑动/驱逐后消息会重新从存储反序列化，引用必然不同，
 * 只有内容身份才能定位「哪些轮次已并入摘要」。</p>
 */
public final class ChatMessageIdentities {

	private ChatMessageIdentities() {
	}

	/** 消息身份：{@code 类型#toString}。同一会话内相同内容视为同一身份（从后向前取最后一条匹配）。 */
	public static String of(ChatMessage message) {
		if (message == null) {
			return null;
		}
		return message.type().name() + "#" + message;
	}

}
