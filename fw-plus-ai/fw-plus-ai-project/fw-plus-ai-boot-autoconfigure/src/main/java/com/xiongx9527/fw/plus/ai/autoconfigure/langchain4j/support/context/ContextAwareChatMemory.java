package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.context;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * 会话上下文维护装饰器：捕获本轮用户输入，**写时为主**触发上下文维护
 * （{@link ContextMaintenance}，如会话摘要）——每轮 AI 回复落库后维护一次，抢在窗口驱逐前滚动更新；
 * **读时兜底**——进程新加载历史（本轮尚未写过）时在 {@link #messages()} 前补一次，
 * 保证写路径未及时更新时读路径也能补齐。维护失败不中断本轮（自行吞掉并记日志）。
 *
 * <p>注入点说明（langchain4j 1.x 时序）：AiServices 一轮内<b>先读</b> {@code messages()}、
 * <b>再写</b>本轮 {@link UserMessage}，记忆读侧拿不到本轮输入；因此「上下文渲染注入」不再落在
 * 记忆装饰器上，而是在 {@code AssistantBuilder} 装配时挂到 AiServices 的 {@code chatRequestTransformer}
 * （该点能看到含本轮用户消息的最终请求）。本类只承担维护调度与本轮用户输入捕获。</p>
 */
@Slf4j
public class ContextAwareChatMemory implements ChatMemory {

	private final ChatMemory delegate;

	private final ContextRequestFactory requestFactory;

	private final List<ContextMaintenance> maintenances;

	private volatile String latestUserMessage;

	/** 自上次写入以来是否已跑过维护：读路径据此决定是否兜底（避免每轮读取重复维护） */
	private volatile boolean maintainedSinceLastWrite;

	public ContextAwareChatMemory(ChatMemory delegate, ContextRequestFactory requestFactory) {
		this(delegate, requestFactory, java.util.Collections.<ContextMaintenance>emptyList());
	}

	public ContextAwareChatMemory(ChatMemory delegate, ContextRequestFactory requestFactory,
			List<ContextMaintenance> maintenances) {
		this.delegate = delegate;
		this.requestFactory = requestFactory;
		this.maintenances = maintenances == null
				? java.util.Collections.<ContextMaintenance>emptyList()
				: java.util.Collections.unmodifiableList(new ArrayList<ContextMaintenance>(maintenances));
	}

	@Override
	public Object id() {
		return delegate.id();
	}

	@Override
	public void add(ChatMessage message) {
		if (message instanceof UserMessage) {
			UserMessage userMessage = (UserMessage) message;
			if (userMessage.hasSingleText()) {
				this.latestUserMessage = userMessage.singleText();
			}
		}
		// 写前快照：本次 add 可能触发底层窗口驱逐，先取快照保证被驱逐的历史不丢
		List<ChatMessage> beforeWrite = delegate.messages();
		delegate.add(message);
		if (message instanceof AiMessage) {
			// 写时为主：AI 回复落库后滚动维护一次，维护输入 = 写前历史 + 本轮回复（抢在窗口驱逐前）
			List<ChatMessage> fullHistory = new ArrayList<ChatMessage>(beforeWrite.size() + 1);
			fullHistory.addAll(beforeWrite);
			fullHistory.add(message);
			maintain(fullHistory);
			this.maintainedSinceLastWrite = true;
		}
	}

	@Override
	public List<ChatMessage> messages() {
		List<ChatMessage> base = delegate.messages();
		// 读时兜底：本轮尚未写过（如进程新加载历史）时补一次维护
		if (!maintainedSinceLastWrite) {
			maintain(base);
			this.maintainedSinceLastWrite = true;
		}
		return base;
	}

	private void maintain(List<ChatMessage> currentMessages) {
		if (maintenances.isEmpty()) {
			return;
		}
		ContextRequest request = requestFactory.create(latestUserMessage);
		for (ContextMaintenance maintenance : maintenances) {
			try {
				maintenance.maintain(request, currentMessages);
			} catch (Exception e) {
				// 维护失败不中断本轮，来源照旧（可能为空）
				log.warn("上下文维护 {} 失败，本轮跳过：{}",
						maintenance.getClass().getName(), e.getMessage());
			}
		}
	}

	@Override
	public void clear() {
		delegate.clear();
		this.latestUserMessage = null;
		this.maintainedSinceLastWrite = false;
	}

	/** 由装载方提供：把当前会话上下文（memoryId / userId 等）与本轮用户输入组装成 {@link ContextRequest} */
	public interface ContextRequestFactory {

		ContextRequest create(String latestUserMessage);

	}

}
