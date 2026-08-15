package com.xiongx9527.fw.plus.ai.askdata.agent;

/** 单个子问题的不可变独立终态。 */
public final class AskDataSubQuestionResult {

	public enum State {
		SUCCEEDED,
		FAILED,
		TIMED_OUT,
		CANCELLED,
		SKIPPED
	}

	private final String questionId;
	private final State state;
	private final String answer;
	private final String errorCode;
	private final String errorMessage;

	public AskDataSubQuestionResult(String questionId, String answer) {
		this(questionId, State.SUCCEEDED, answer, null, null);
	}

	private AskDataSubQuestionResult(String questionId, State state, String answer, String errorCode,
			String errorMessage) {
		if (blank(questionId) || state == null) {
			throw new IllegalArgumentException("questionId 和 state 不能为空");
		}
		if (state != State.SUCCEEDED && blank(errorCode)) {
			throw new IllegalArgumentException("非成功终态必须携带 errorCode");
		}
		this.questionId = questionId.trim();
		this.state = state;
		this.answer = answer;
		this.errorCode = normalize(errorCode);
		this.errorMessage = normalize(errorMessage);
	}

	public static AskDataSubQuestionResult success(String questionId, String answer) {
		return new AskDataSubQuestionResult(questionId, State.SUCCEEDED, answer, null, null);
	}

	public static AskDataSubQuestionResult failed(String questionId, String errorCode, String errorMessage) {
		return new AskDataSubQuestionResult(questionId, State.FAILED, null, errorCode, errorMessage);
	}

	public static AskDataSubQuestionResult timedOut(String questionId) {
		return new AskDataSubQuestionResult(questionId, State.TIMED_OUT, null,
				"SUBQUESTION_TIMED_OUT", "子问题执行超时");
	}

	public static AskDataSubQuestionResult cancelled(String questionId) {
		return new AskDataSubQuestionResult(questionId, State.CANCELLED, null,
				"SUBQUESTION_CANCELLED", "子问题已取消");
	}

	public static AskDataSubQuestionResult skipped(String questionId) {
		return new AskDataSubQuestionResult(questionId, State.SKIPPED, null,
				"SUBQUESTION_SKIPPED", "子问题因依赖失败被跳过");
	}

	public String getQuestionId() { return questionId; }
	public State getState() { return state; }
	public boolean isSuccess() { return state == State.SUCCEEDED; }
	public String getAnswer() { return answer; }
	public String getErrorCode() { return errorCode; }
	public String getErrorMessage() { return errorMessage; }

	private static String normalize(String value) {
		return blank(value) ? null : value.trim();
	}

	private static boolean blank(String value) {
		return value == null || value.trim().length() == 0;
	}
}
