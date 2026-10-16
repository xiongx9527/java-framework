package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.context;

import dev.langchain4j.data.message.ChatMessage;

import java.util.List;

/**
 * 上下文维护钩子：在 {@link ContextAwareChatMemory} 渲染前被调用，用于「读时兜底」——
 * 例如按会话历史滚动维护会话摘要，保证写路径未及时更新时读路径可补齐。
 *
 * <p>实现约定：不得抛出中断本轮对话的异常；维护失败应自行吞掉并记日志。</p>
 */
public interface ContextMaintenance {

	/**
	 * @param request         本轮请求（含会话键）
	 * @param currentMessages 记忆当前全部消息（含窗口内近期消息）
	 */
	void maintain(ContextRequest request, List<ChatMessage> currentMessages);

	/**
	 * 携带当轮消息的维护入口（**纯增量**，default 保证既有实现零改动）：
	 * 写路径在 {@code add} 之后调用，{@code turnMessage} 为本次写入的消息；
	 * 读时兜底等无当轮消息场景传 {@code null}。
	 *
	 * <p>默认实现委托 2 参方法；需要当轮消息的实现（如事实通道）覆写本方法。</p>
	 */
	default void maintain(ContextRequest request, List<ChatMessage> currentMessages, ChatMessage turnMessage) {
		maintain(request, currentMessages);
	}

}	