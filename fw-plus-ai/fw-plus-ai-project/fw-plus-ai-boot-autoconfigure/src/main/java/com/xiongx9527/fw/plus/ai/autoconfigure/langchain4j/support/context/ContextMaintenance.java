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

}	