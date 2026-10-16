package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.summary;

import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.context.ContextMaintenance;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.context.ContextRequest;
import dev.langchain4j.data.message.ChatMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * 读时兜底维护：把「已滚出最近窗口」的历史交给 {@link RollingSummaryEngine} 滚动维护会话摘要，
 * 使写路径未及时更新时读路径也能补齐。
 */
public class SummaryMaintenance implements ContextMaintenance {

	private final RollingSummaryEngine engine;

	/** 最近窗口条数：超出部分视为窗口外历史 */
	private final int windowSize;

	public SummaryMaintenance(RollingSummaryEngine engine, int windowSize) {
		this.engine = engine;
		this.windowSize = windowSize < 1 ? 1 : windowSize;
	}

	@Override
	public void maintain(ContextRequest request, List<ChatMessage> currentMessages) {
		if (request == null || request.getSessionKey() == null || currentMessages == null) {
			return;
		}
		int keepFrom = Math.max(0, currentMessages.size() - windowSize);
		if (keepFrom <= 0) {
			return;
		}
		List<ChatMessage> windowExternal = new ArrayList<ChatMessage>(currentMessages.subList(0, keepFrom));
		engine.maintain(request.getSessionKey(), windowExternal);
	}

}	