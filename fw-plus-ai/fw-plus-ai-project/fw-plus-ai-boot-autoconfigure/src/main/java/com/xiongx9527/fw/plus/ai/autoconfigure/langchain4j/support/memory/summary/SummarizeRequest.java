package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.summary;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 摘要请求：一次摘要操作的全部输入。策略实现按自身需要取用其中字段。
 */
public final class SummarizeRequest {

	/** 已有摘要（增量精炼的起点；首次为空串） */
	private final String existingSummary;

	/** 本次新增的轮次文本（滚动新增、待并入摘要） */
	private final List<String> newTurns;

	/** 窗口外历史全文（全量重摘用；可为空） */
	private final String fullHistory;

	/** 目标摘要 token 上限（<=0 表示不限制） */
	private final int maxTokens;

	public SummarizeRequest(String existingSummary, List<String> newTurns, String fullHistory, int maxTokens) {
		this.existingSummary = existingSummary == null ? "" : existingSummary;
		this.newTurns = newTurns == null
				? Collections.<String>emptyList()
				: Collections.unmodifiableList(new ArrayList<String>(newTurns));
		this.fullHistory = fullHistory == null ? "" : fullHistory;
		this.maxTokens = maxTokens;
	}

	public String getExistingSummary() {
		return existingSummary;
	}

	public List<String> getNewTurns() {
		return newTurns;
	}

	public String getFullHistory() {
		return fullHistory;
	}

	public int getMaxTokens() {
		return maxTokens;
	}

	/** 待压缩的原文（fullHistory 优先，为空时退化为 newTurns 拼接） */
	public String sourceText() {
		if (fullHistory != null && !fullHistory.trim().isEmpty()) {
			return fullHistory;
		}
		StringBuilder builder = new StringBuilder();
		for (String turn : newTurns) {
			if (turn == null || turn.trim().isEmpty()) {
				continue;
			}
			if (builder.length() > 0) {
				builder.append('\n');
			}
			builder.append(turn.trim());
		}
		return builder.toString();
	}

}