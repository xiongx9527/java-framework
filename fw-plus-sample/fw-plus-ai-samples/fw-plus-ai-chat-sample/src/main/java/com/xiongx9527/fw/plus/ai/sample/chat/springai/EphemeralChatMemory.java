package com.xiongx9527.fw.plus.ai.sample.chat.springai;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 可配置的 Ephemeral ChatMemory 装饰器（Spring AI 版）——与 LangChain4j 链路的框架
 * {@code EphemeralChatMemory} 同款 builder 开关，作用于 Spring AI 的 {@link ChatMemory} 接口。
 *
 * <p><b>时序映射</b>（{@code MessageChatMemoryAdvisor} 已确认的行为）：</p>
 * <ul>
 *   <li>每轮 before：先 {@code get()}（此时 store 尚无当前 user）→ 组装 prompt → 末尾
 *       {@code add(当前 user)}；after：{@code add(AI 回复)}。</li>
 *   <li>因此「收到新 UserMessage = 新一轮开始」——固化上一轮发生在 {@link #add} 拦截中，
 *       与框架版在 {@code add(UserMessage)} 前固化 {@code store} 的时机状态完全一致。</li>
 * </ul>
 *
 * <p><b>核心行为</b>：</p>
 * <ul>
 *   <li><b>写入固化</b>：新 user 到来时，把「最后一条 user 之后的上一轮输出」按
 *       {@link AiPersistenceStrategy} 筛选后写回 delegate（历史用户原话全保留、按开关剥离检索段）；</li>
 *   <li><b>读取回放</b>：{@link #get} 返回前——历史 user 按 {@code stripRagFromHistory} 剥离检索段，
 *       最后一条 user 按 {@code includeCurrentUserWithEphemeral} 决定是否原样，
 *       「最后一条 user 之后的输出段」按工具开关 + 同一策略回放（保证 prompt 视图与持久视图一致）；</li>
 *   <li><b>默认不放</b>：RAG 检索段、工具往返默认只存在于本轮请求窗口，不沉淀进持久历史。</li>
 * </ul>
 *
 * <p><b>与框架版的差异</b>：Spring AI 没有独立 ChatMemoryStore（存储封装在 delegate 内，
 * 固化用 {@code get → clear → add} 模拟写回）；记忆中没有 SystemMessage（系统提示每轮由
 * 服务层直接携带，此处对 SystemMessage 仅作防御性透传）；不支持按位置插花
 * （见 {@link AiPersistenceStrategy} 的差异说明）。</p>
 *
 * <p><b>装配</b>：见 {@link SpringAiChatConfig}——两条 Spring AI 链路共享此装饰后的
 * ChatMemory Bean。</p>
 */
public class EphemeralChatMemory implements ChatMemory {

	/**
	 * 默认的检索内容注入分隔串。
	 * <p>与 Spring AI 官方 {@code ContextualQueryAugmenter} 模板的开头一致
	 * （「Context information is below.」），锚点在开头时从模板的「Query: 」段提取用户原话。</p>
	 */
	public static final String DEFAULT_RAG_SEPARATOR = "Context information is below.";

	private static final String QUERY_MARKER = "Query: ";
	private static final String ANSWER_MARKER = "\n\nAnswer:";

	private final ChatMemory delegate;
	private final AiPersistenceStrategy aiPersistenceStrategy;
	private final List<String> ephemeralSeparators;
	private final boolean stripRagFromHistory;
	private final boolean includeCurrentUserWithEphemeral;
	private final boolean sendToolResultsInCurrentTurn;
	private final boolean sendToolRequestsInCurrentTurn;

	private EphemeralChatMemory(Builder builder) {
		this.delegate = builder.delegate;
		this.aiPersistenceStrategy = builder.aiPersistenceStrategy;
		this.ephemeralSeparators = builder.ephemeralSeparators;
		this.stripRagFromHistory = builder.stripRagFromHistory;
		this.includeCurrentUserWithEphemeral = builder.includeCurrentUserWithEphemeral;
		this.sendToolResultsInCurrentTurn = builder.sendToolResultsInCurrentTurn;
		this.sendToolRequestsInCurrentTurn = builder.sendToolRequestsInCurrentTurn;
	}

	/**
	 * @param delegate 底层记忆（通常为 {@code MessageWindowChatMemory}），存储由其内部封装
	 */
	public static Builder builder(ChatMemory delegate) {
		return new Builder(delegate);
	}

	@Override
	public void add(String conversationId, Message message) {
		if (message instanceof UserMessage) {
			// 新一轮开始：先固化「上一轮」输出到持久历史，再追加当前用户消息。
			// 必须在 delegate.add 之前做——此时 store 里还没有本轮 user，正是策略筛选的窗口。
			List<Message> stored = delegate.get(conversationId);
			if (stored != null && !stored.isEmpty()) {
				List<Message> durable = buildDurableHistory(stored);
				delegate.clear(conversationId);
				if (!durable.isEmpty()) {
					delegate.add(conversationId, durable);
				}
			}
		}
		delegate.add(conversationId, message);
	}

	@Override
	public void add(String conversationId, List<Message> messages) {
		if (messages == null) {
			return;
		}
		// 逐条走单条 add：新 user 触发固化，其余透传（与 delegate 批量语义一致）
		for (Message message : messages) {
			add(conversationId, message);
		}
	}

	@Override
	public List<Message> get(String conversationId) {
		List<Message> stored = delegate.get(conversationId);
		if (stored == null || stored.isEmpty()) {
			return stored == null ? Collections.emptyList() : stored;
		}

		int lastUserIndex = lastUserIndex(stored);
		if (lastUserIndex < 0) {
			// 防御：没有 user 的记忆不做过滤，原样回放
			return new ArrayList<>(stored);
		}

		List<Message> sendable = new ArrayList<>(stored.size());
		for (int i = 0; i <= lastUserIndex; i++) {
			Message message = stored.get(i);
			if (i < lastUserIndex) {
				// 更早轮次的历史用户原话：按开关剥离检索段
				sendable.add((message instanceof UserMessage && stripRagFromHistory)
						? stripRag((UserMessage) message) : message);
			} else {
				// 最后一条 user（Spring AI 中滞后一轮的「上一轮原话」）：等价框架的
				// includeCurrentUserWithEphemeral——true 保留原样，false 剥离检索段
				sendable.add((message instanceof UserMessage && !includeCurrentUserWithEphemeral)
						? stripRag((UserMessage) message) : message);
			}
		}

		// 最后一条 user 之后 = 上一轮输出（尚未固化）：工具消息按当前轮开关，
		// 最终回答按策略回放——与 add 时的固化使用同一策略，保证两视图一致
		if (lastUserIndex + 1 < stored.size()) {
			List<Message> outputs = new ArrayList<>(stored.subList(lastUserIndex + 1, stored.size()));
			List<Message> selected = aiPersistenceStrategy == null
					? Collections.emptyList()
					: aiPersistenceStrategy.selectForPersistence(outputs);
			for (Message message : outputs) {
				if (AiPersistenceStrategy.isToolResultMessage(message)) {
					if (sendToolResultsInCurrentTurn) {
						sendable.add(message);
					}
				} else if (AiPersistenceStrategy.isToolCallMessage(message)) {
					if (sendToolRequestsInCurrentTurn) {
						sendable.add(message);
					}
				} else if (selected.contains(message)) {
					sendable.add(message);
				}
				// 其余（未被策略选中的最终回答等）不回放
			}
		}
		return sendable;
	}

	@Override
	public void clear(String conversationId) {
		delegate.clear(conversationId);
	}

	/**
	 * 构建持久历史：历史用户原话（按开关剥离检索段）+ 按策略筛出的上一轮输出。
	 * 系统提示若存在则无条件保留（Spring AI 记忆里通常没有，防御性处理）。
	 */
	private List<Message> buildDurableHistory(List<Message> stored) {
		int lastUserIndex = lastUserIndex(stored);
		if (lastUserIndex < 0) {
			return new ArrayList<>(stored);
		}

		List<Message> durable = new ArrayList<>(stored.size());
		for (int i = 0; i < stored.size(); i++) {
			Message message = stored.get(i);
			if (message instanceof SystemMessage) {
				durable.add(message);
			} else if (message instanceof UserMessage) {
				durable.add(stripRagFromHistory ? stripRag((UserMessage) message) : message);
			}
		}

		// 上一轮输出交给策略筛选（选中项按原序追加在历史用户之后）
		if (lastUserIndex + 1 < stored.size()) {
			List<Message> outputs = new ArrayList<>(stored.subList(lastUserIndex + 1, stored.size()));
			if (aiPersistenceStrategy != null) {
				durable.addAll(aiPersistenceStrategy.selectForPersistence(outputs));
			}
		}
		return durable;
	}

	private static int lastUserIndex(List<Message> stored) {
		for (int i = stored.size() - 1; i >= 0; i--) {
			if (stored.get(i) instanceof UserMessage) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * 去掉 user message 中由注入模板拼入的临时检索段：
	 * <ul>
	 *   <li>锚点在开头（Spring AI {@code ContextualQueryAugmenter} 前缀模板）——从
	 *       「Query: 」段提取用户原话；</li>
	 *   <li>锚点在中间/尾部（后缀注入模板，兼容自定义分隔串）——保留锚点之前的部分。</li>
	 * </ul>
	 * 提取失败时返回原消息（宁可不剥离，也不丢用户原话）。
	 */
	private UserMessage stripRag(UserMessage message) {
		String text = message.getText();
		if (text == null || text.isEmpty()) {
			return message;
		}
		for (String separator : ephemeralSeparators) {
			if (separator == null || separator.isEmpty()) {
				continue;
			}
			int anchor = text.indexOf(separator);
			if (anchor < 0) {
				continue;
			}
			if (anchor == 0) {
				// 前缀模板：原话在「Query: 」之后（到「Answer:」提示词之前）
				int queryIndex = text.lastIndexOf(QUERY_MARKER);
				if (queryIndex < 0) {
					continue;
				}
				String query = text.substring(queryIndex + QUERY_MARKER.length());
				int answerIndex = query.indexOf(ANSWER_MARKER);
				return new UserMessage(answerIndex >= 0 ? query.substring(0, answerIndex) : query.trim());
			}
			// 后缀注入：锚点之前是用户原话
			return new UserMessage(text.substring(0, anchor));
		}
		return message;
	}

	public static class Builder {
		private final ChatMemory delegate;
		private AiPersistenceStrategy aiPersistenceStrategy = AiPersistenceStrategy.PERSIST_NON_TOOL_FINAL_ANSWER;
		private List<String> ephemeralSeparators =
				Collections.singletonList(DEFAULT_RAG_SEPARATOR);
		private boolean stripRagFromHistory = true;
		private boolean includeCurrentUserWithEphemeral = true;
		private boolean sendToolResultsInCurrentTurn = true;
		private boolean sendToolRequestsInCurrentTurn = true;

		private Builder(ChatMemory delegate) {
			this.delegate = delegate;
		}

		/** 设置 AI 持久化策略（实例优先；null 回落到默认策略）。 */
		public Builder aiPersistenceStrategy(AiPersistenceStrategy aiPersistenceStrategy) {
			this.aiPersistenceStrategy = aiPersistenceStrategy == null
					? AiPersistenceStrategy.PERSIST_NON_TOOL_FINAL_ANSWER
					: aiPersistenceStrategy;
			return this;
		}

		/** 临时检索段的剥离分隔串（任一命中即剥离）。 */
		public Builder ephemeralSeparators(List<String> separators) {
			if (separators != null && !separators.isEmpty()) {
				this.ephemeralSeparators = new ArrayList<>(separators);
			}
			return this;
		}

		public Builder ephemeralSeparators(String... separators) {
			if (separators != null && separators.length > 0) {
				this.ephemeralSeparators = Arrays.asList(separators);
			}
			return this;
		}

		/**
		 * 更早轮次的历史用户消息是否剥离检索段。默认 true。
		 * <p>Spring AI 中原始 user 本就不含检索段（Advisor 在记忆写入后才注入），此开关是防御——
		 * 若历史被自定义写入方污染（带注入模板），读取时按分隔串剥离。</p>
		 */
		public Builder stripRagFromHistory(boolean stripRagFromHistory) {
			this.stripRagFromHistory = stripRagFromHistory;
			return this;
		}

		/**
		 * 最后一条历史 user 是否保留原样（等价框架「当前轮 user 是否带 ephemeral 上下文」）。默认 true。
		 * <p>注意：Spring AI 中<b>本轮</b> user 消息不经过 ChatMemory（MemoryAdvisor 在 get 之后
		 * 直接携带并写入），该开关作用于 store 中最后一条 user。</p>
		 */
		public Builder includeCurrentUserWithEphemeral(boolean includeCurrentUserWithEphemeral) {
			this.includeCurrentUserWithEphemeral = includeCurrentUserWithEphemeral;
			return this;
		}

		/** 输出段中的工具执行结果是否回放给模型。默认 true。 */
		public Builder sendToolResultsInCurrentTurn(boolean sendToolResultsInCurrentTurn) {
			this.sendToolResultsInCurrentTurn = sendToolResultsInCurrentTurn;
			return this;
		}

		/** 输出段中的工具调用中间步是否回放给模型。默认 true。 */
		public Builder sendToolRequestsInCurrentTurn(boolean sendToolRequestsInCurrentTurn) {
			this.sendToolRequestsInCurrentTurn = sendToolRequestsInCurrentTurn;
			return this;
		}

		public EphemeralChatMemory build() {
			if (delegate == null) {
				throw new IllegalStateException("EphemeralChatMemory.Builder: delegate 不能为空");
			}
			return new EphemeralChatMemory(this);
		}
	}
}
