package com.xiongx9527.fw.plus.ai.askdata.storage;

import com.xiongx9527.fw.plus.ai.askdata.context.AskDataProjectContext;

/**
 * AskData 会话存储 SPI。
 *
 * <p>实现必须按项目和操作人共同隔离会话。内存实现仅适用于单节点部署；集群部署必须使用共享存储实现。</p>
 */
public interface AskDataConversationStore {

	/**
	 * 保存会话内容。
	 *
	 * <p>实现必须以 {@code projectId + operatorId + conversationId} 作为唯一隔离范围，禁止同一会话标识
	 * 在不同项目或操作人之间互相覆盖或读取。调用方应传入非空上下文、非空白会话标识和非空会话内容。</p>
	 *
	 * <p>SPI 不限定具体存储介质；默认内存实现仅在单机进程内有效，集群部署必须替换为可在节点间共享的存储实现。</p>
	 */
	void save(AskDataProjectContext context, String conversationId, Conversation conversation);

	/**
	 * 按当前项目和操作人读取会话内容。
	 *
	 * <p>查询范围必须与 {@link #save(AskDataProjectContext, String, Conversation)} 完全一致，即只读取
	 * {@code projectId + operatorId + conversationId} 对应的数据；不存在时返回 {@code null}，不得回退读取其他项目
	 * 或操作人的会话。</p>
	 *
	 * <p>默认内存实现只保存当前单机进程的数据，不能作为集群节点间会话共享机制。</p>
	 */
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
