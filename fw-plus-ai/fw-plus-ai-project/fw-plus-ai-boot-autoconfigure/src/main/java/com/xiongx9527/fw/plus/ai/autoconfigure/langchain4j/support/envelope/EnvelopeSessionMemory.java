package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.envelope;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageType;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 会话记忆读写件：结构化轮次既不走 AiServices，就必须自己读历史、自己写回。
 *
 * <p>本类不持有会话状态，所有读写都落在**同一个** {@link ChatMemoryStore} 上（键为 {@code memoryId}），
 * 内部用 {@link MessageWindowChatMemory}（+ 该 store）承载窗口裁剪——这正是既有 AiServices 通道
 * （{@code AssistantBuilder} → {@code EphemeralChatMemory(MessageWindowChatMemory, store)}）用的同一份存储，
 * 因此「同一会话共用一份历史、跨业务切换连续」自然成立。</p>
 *
 * <p><b>读路径结论（langchain4j 0.35.0 实测）</b>：{@code MessageWindowChatMemory.messages()} 每次都
 * {@code store.getMessages(id)} 重建列表、{@code add(...)} 回写 {@code store.updateMessages(...)}，
 * 因此「直写 store」对本类的快照读、对既有 {@code MessageWindowChatMemory}、对
 * {@code EphemeralChatMemory} 的读路径**都立即可见**（无需绕道读 store 快照）。唯一时差在既有装饰器
 * 的语义上：{@code EphemeralChatMemory} 把「最后一条用户消息之后的 AI」视为本轮输出，直到**下一轮**
 * 新用户消息到达时才固化进持久历史——即结构化轮次写回的用户原文与摘要，在下一轮进入请求上下文。</p>
 *
 * <p><b>只写这两种内容</b>：本轮用户原文 + 一条结果摘要（{@link #summarize(ChatEnvelope)}）。
 * 模型原文、校验错误清单一律不进历史（失败轮次写的就是兜底失败说明）。</p>
 *
 * <p><b>闲聊轮次不要调用 {@link #recordTurn}</b>：非结构化轮次经 AiServices 走既有记忆通道，
 * 已经写过一次，再写会重复。</p>
 */
public final class EnvelopeSessionMemory {

	/** 历史摘要长度上限（字符）：摘要每轮都要回喂给模型，不能让长结果撑爆后续轮次上下文 */
	public static final int MAX_SUMMARY_LENGTH = 1000;

	private static final String SUCCESS_PREFIX = "已处理业务 ";

	private static final String SUCCESS_INFIX = "，结果：";

	private final ChatMemoryStore store;

	private final int maxMessages;

	public EnvelopeSessionMemory(ChatMemoryStore store, int maxMessages) {
		if (store == null) {
			throw new IllegalArgumentException("chatMemoryStore 不能为 null");
		}
		if (maxMessages < 1) {
			throw new IllegalArgumentException("会话窗口条数必须 >= 1，实际：" + maxMessages);
		}
		this.store = store;
		this.maxMessages = maxMessages;
	}

	/**
	 * 读该会话的历史快照（从旧到新）。
	 *
	 * <p>只读：返回不可变副本，不改写 store。系统提示被过滤掉——结构化轮次自带系统提示
	 * （{@code systemHint}），历史里的系统提示不参与拼接。</p>
	 *
	 * @param memoryId 会话标识；空白时返回空快照
	 */
	public List<ChatMessage> snapshot(String memoryId) {
		if (isBlank(memoryId)) {
			return Collections.emptyList();
		}
		List<ChatMessage> stored = window(memoryId).messages();
		List<ChatMessage> filtered = new ArrayList<ChatMessage>(stored.size());
		for (ChatMessage message : stored) {
			if (message.type() != ChatMessageType.SYSTEM) {
				filtered.add(message);
			}
		}
		return Collections.unmodifiableList(filtered);
	}

	/**
	 * 轮次结束写回：追加本轮用户原文与一条结果摘要。
	 *
	 * <p>调用时机是**整轮结束**（含重试结束后）——重试过程零写入，历史里不会留下失败尝试。</p>
	 *
	 * @param memoryId 会话标识；空白时为无操作
	 * @param userText 本轮用户原文；空白时为无操作
	 * @param envelope 本轮信封；{@code null} 表示本轮没有产出结果，只写用户原文
	 */
	public void recordTurn(String memoryId, String userText, ChatEnvelope envelope) {
		if (isBlank(memoryId) || isBlank(userText)) {
			return;
		}
		ChatMemory window = window(memoryId);
		window.add(UserMessage.from(userText));
		String summary = summarize(envelope);
		if (!isBlank(summary)) {
			window.add(AiMessage.from(summary));
		}
	}

	/**
	 * 本轮信封 → 一条可回喂的历史摘要。
	 *
	 * <ul>
	 *   <li>结构化成功：{@code 已处理业务 {business}，结果：{json}}</li>
	 *   <li>闲聊轮次：模型原文</li>
	 *   <li>失败轮次：兜底失败说明（不透校验错误、不透异常）</li>
	 *   <li>{@code null} 信封：空串（本轮没有产出结果）</li>
	 * </ul>
	 *
	 * <p>超长摘要截断到 {@value #MAX_SUMMARY_LENGTH} 字符（可能截断 JSON 尾部；完整结果由信封
	 * 本身交付给调用方，历史里只需要能接上下文）。</p>
	 */
	public static String summarize(ChatEnvelope envelope) {
		if (envelope == null) {
			return "";
		}
		if (!envelope.isOk()) {
			return ChatEnvelope.DEFAULT_FAILURE_TEXT;
		}
		if (envelope.getResult() == null) {
			return bound(envelope.getText());
		}
		return bound(SUCCESS_PREFIX + envelope.getBusiness() + SUCCESS_INFIX + envelope.getResult().toJSONString());
	}

	// -------------------------------------------------- 内部

	/** 该 memoryId 的窗口记忆：每次现建，实例不持有状态（窗口只在 add 时改写 store） */
	private ChatMemory window(Object memoryId) {
		return MessageWindowChatMemory.builder()
				.id(memoryId)
				.maxMessages(maxMessages)
				.chatMemoryStore(store)
				.build();
	}

	private static String bound(String text) {
		if (text == null) {
			return "";
		}
		if (text.length() <= MAX_SUMMARY_LENGTH) {
			return text;
		}
		return text.substring(0, MAX_SUMMARY_LENGTH - 1) + "…";
	}

	private static boolean isBlank(String text) {
		return text == null || text.trim().isEmpty();
	}
}
