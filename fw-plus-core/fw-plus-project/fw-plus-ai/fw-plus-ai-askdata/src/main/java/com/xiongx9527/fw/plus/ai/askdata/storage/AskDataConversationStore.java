package com.xiongx9527.fw.plus.ai.askdata.storage;

import com.xiongx9527.fw.plus.ai.askdata.context.AskDataProjectContext;

/**
 * AskData 会话存储 SPI。
 *
 * <p>实现必须按项目和操作人共同隔离会话。内存实现仅适用于单节点部署；集群部署必须使用共享存储实现。</p>
 */
public interface AskDataConversationStore {

	void save(AskDataProjectContext context, String conversationId, Conversation conversation);

	Conversation load(AskDataProjectContext context, String conversationId);

	/**
	 * 会话的最小持久化内容。
	 */
	class Conversation {

		private final String question;
		private final String answer;

		public Conversation(String question, String answer) {
			this.question = question;
			this.answer = answer;
		}

		public String getQuestion() {
			return question;
		}

		public String getAnswer() {
			return answer;
		}
	}
}
