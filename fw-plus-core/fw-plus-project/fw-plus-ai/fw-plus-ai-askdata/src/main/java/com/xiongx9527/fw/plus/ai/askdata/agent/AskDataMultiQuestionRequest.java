package com.xiongx9527.fw.plus.ai.askdata.agent;

import com.xiongx9527.fw.plus.ai.askdata.context.AskDataProjectContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 多问题执行请求，含协作取消、总超时预算和并发上限。 */
public final class AskDataMultiQuestionRequest {

	private final AskDataProjectContext context;
	private final String requestId;
	private final List<AskDataSubQuestion> questions;
	private final List<AskDataSubQuestionResult> completedResults;
	private final long timeoutMillis;
	private final int maxConcurrency;
	private volatile boolean cancelled;

	public AskDataMultiQuestionRequest(AskDataProjectContext context, String requestId,
			List<AskDataSubQuestion> questions, long timeoutMillis) {
		this(context, requestId, questions, timeoutMillis, 1);
	}

	public AskDataMultiQuestionRequest(AskDataProjectContext context, String requestId,
			List<AskDataSubQuestion> questions, long timeoutMillis, int maxConcurrency) {
		this(context, requestId, questions, timeoutMillis, maxConcurrency,
				Collections.<AskDataSubQuestionResult>emptyList());
	}

	public AskDataMultiQuestionRequest(AskDataProjectContext context, String requestId,
			List<AskDataSubQuestion> questions, long timeoutMillis, int maxConcurrency,
			List<AskDataSubQuestionResult> completedResults) {
		if (context == null || blank(requestId) || questions == null || questions.isEmpty()
				|| timeoutMillis <= 0L || maxConcurrency <= 0 || completedResults == null) {
			throw new IllegalArgumentException("多问题请求参数无效");
		}
		this.context = context;
		this.requestId = requestId.trim();
		this.questions = Collections.unmodifiableList(new ArrayList<AskDataSubQuestion>(questions));
		this.completedResults = Collections.unmodifiableList(
				new ArrayList<AskDataSubQuestionResult>(completedResults));
		this.timeoutMillis = timeoutMillis;
		this.maxConcurrency = maxConcurrency;
	}

	public AskDataProjectContext getContext() { return context; }
	public String getRequestId() { return requestId; }
	public List<AskDataSubQuestion> getQuestions() { return questions; }
	public List<AskDataSubQuestionResult> getCompletedResults() { return completedResults; }
	public long getTimeoutMillis() { return timeoutMillis; }
	public int getMaxConcurrency() { return maxConcurrency; }
	public void cancel() { cancelled = true; }
	public boolean isCancelled() { return cancelled; }

	private static boolean blank(String value) {
		return value == null || value.trim().length() == 0;
	}
}
