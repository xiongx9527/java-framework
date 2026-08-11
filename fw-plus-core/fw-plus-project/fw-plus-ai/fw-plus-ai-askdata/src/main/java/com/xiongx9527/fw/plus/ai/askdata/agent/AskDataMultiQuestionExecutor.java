package com.xiongx9527.fw.plus.ai.askdata.agent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/** 在受限并发度内执行子问题依赖 DAG。 */
public final class AskDataMultiQuestionExecutor {

	public AskDataMultiQuestionResult execute(AskDataMultiQuestionRequest request,
			final AskDataSubQuestionExecutor executor) {
		return execute(request, executor, result -> { });
	}

	public AskDataMultiQuestionResult execute(AskDataMultiQuestionRequest request,
			final AskDataSubQuestionExecutor executor, AskDataSubQuestionResultListener listener) {
		if (request == null || executor == null || listener == null) {
			throw new IllegalArgumentException("request、executor 和 listener 不能为空");
		}
		final Map<String, AskDataSubQuestion> questions = validate(request.getQuestions());
		Map<String, AskDataSubQuestionResult> results = restoredResults(request.getCompletedResults(), questions);
		Map<String, RunningQuestion> running = new LinkedHashMap<String, RunningQuestion>();
		ExecutorService workers = Executors.newFixedThreadPool(
				Math.min(request.getMaxConcurrency(), questions.size()));
		long deadlineNanos = deadlineAfter(request.getTimeoutMillis());
		try {
			while (results.size() < questions.size()) {
				if (request.isCancelled() || Thread.currentThread().isInterrupted()) {
					request.cancel();
					cancelRunning(running);
					fillMissing(questions, results, TerminalState.CANCELLED, listener);
					break;
				}
				long now = System.nanoTime();
				if (now >= deadlineNanos) {
					cancelRunning(running);
					fillMissing(questions, results, TerminalState.TIMED_OUT, listener);
					break;
				}

				boolean progressed = collectFinished(running, results, now, listener);
				if (request.isCancelled()) {
					continue;
				}
				for (AskDataSubQuestion question : questions.values()) {
					if (results.containsKey(question.getId()) || running.containsKey(question.getId())) {
						continue;
					}
					if (!dependenciesTerminal(question, results)) {
						continue;
					}
					if (dependencyFailed(question, results)
							&& question.getFailurePolicy() == AskDataSubQuestion.FailurePolicy.SKIP) {
						record(results, AskDataSubQuestionResult.skipped(question.getId()), listener);
						progressed = true;
						continue;
					}
					if (running.size() >= request.getMaxConcurrency()) {
						continue;
					}
					Future<AskDataSubQuestionResult> future = workers.submit(() -> executor.execute(question));
					long questionDeadline = question.getTimeoutMillis() > 0L
							? Math.min(deadlineNanos, deadlineAfter(question.getTimeoutMillis())) : deadlineNanos;
					running.put(question.getId(), new RunningQuestion(question, future, questionDeadline));
					progressed = true;
				}

				if (running.isEmpty() && !progressed && results.size() < questions.size()) {
					throw new IllegalArgumentException("多问题计划存在循环依赖");
				}
				if (!running.isEmpty()) {
					pause(request);
				}
			}
			return orderedResult(questions, results);
		} finally {
			cancelRunning(running);
			workers.shutdownNow();
		}
	}

	private Map<String, AskDataSubQuestionResult> restoredResults(List<AskDataSubQuestionResult> values,
			Map<String, AskDataSubQuestion> questions) {
		Map<String, AskDataSubQuestionResult> results = new LinkedHashMap<String, AskDataSubQuestionResult>();
		for (AskDataSubQuestionResult value : values) {
			if (value == null || !questions.containsKey(value.getQuestionId())
					|| results.put(value.getQuestionId(), value) != null) {
				throw new IllegalArgumentException("已恢复子问题结果无效或 ID 重复");
			}
		}
		for (String questionId : results.keySet()) {
			if (!results.keySet().containsAll(questions.get(questionId).getDependsOn())) {
				throw new IllegalArgumentException("已恢复子问题缺少前置依赖终态: " + questionId);
			}
		}
		return results;
	}

	private Map<String, AskDataSubQuestion> validate(List<AskDataSubQuestion> values) {
		if (values == null || values.isEmpty()) {
			throw new IllegalArgumentException("questions 不能为空");
		}
		Map<String, AskDataSubQuestion> questions = new LinkedHashMap<String, AskDataSubQuestion>();
		for (AskDataSubQuestion question : values) {
			if (question == null || questions.put(question.getId(), question) != null) {
				throw new IllegalArgumentException("子问题不能为空且 ID 不可重复");
			}
		}
		for (AskDataSubQuestion question : questions.values()) {
			for (String dependency : question.getDependsOn()) {
				if (!questions.containsKey(dependency)) {
					throw new IllegalArgumentException("子问题依赖不存在: " + dependency);
				}
			}
		}
		return Collections.unmodifiableMap(questions);
	}

	private boolean collectFinished(Map<String, RunningQuestion> running,
			Map<String, AskDataSubQuestionResult> results, long now,
			AskDataSubQuestionResultListener listener) {
		boolean progressed = false;
		Iterator<Map.Entry<String, RunningQuestion>> iterator = running.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<String, RunningQuestion> entry = iterator.next();
			RunningQuestion runningQuestion = entry.getValue();
			if (runningQuestion.future.isDone()) {
				record(results, completedResult(runningQuestion), listener);
				iterator.remove();
				progressed = true;
			} else if (now >= runningQuestion.deadlineNanos) {
				runningQuestion.future.cancel(true);
				record(results, AskDataSubQuestionResult.timedOut(entry.getKey()), listener);
				iterator.remove();
				progressed = true;
			}
		}
		return progressed;
	}

	private AskDataSubQuestionResult completedResult(RunningQuestion runningQuestion) {
		String questionId = runningQuestion.question.getId();
		try {
			AskDataSubQuestionResult result = runningQuestion.future.get();
			if (result == null || !questionId.equals(result.getQuestionId())) {
				return AskDataSubQuestionResult.failed(questionId, "SUBQUESTION_FAILED",
						"子问题执行器返回了无效结果");
			}
			return result;
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			return AskDataSubQuestionResult.cancelled(questionId);
		} catch (ExecutionException ex) {
			return AskDataSubQuestionResult.failed(questionId, "SUBQUESTION_FAILED", "子问题执行失败");
		} catch (RuntimeException ex) {
			return AskDataSubQuestionResult.failed(questionId, "SUBQUESTION_FAILED", "子问题执行失败");
		}
	}

	private boolean dependenciesTerminal(AskDataSubQuestion question,
			Map<String, AskDataSubQuestionResult> results) {
		return results.keySet().containsAll(question.getDependsOn());
	}

	private boolean dependencyFailed(AskDataSubQuestion question,
			Map<String, AskDataSubQuestionResult> results) {
		for (String dependency : question.getDependsOn()) {
			AskDataSubQuestionResult result = results.get(dependency);
			if (result == null || !result.isSuccess()) {
				return true;
			}
		}
		return false;
	}

	private void fillMissing(Map<String, AskDataSubQuestion> questions,
			Map<String, AskDataSubQuestionResult> results, TerminalState state,
			AskDataSubQuestionResultListener listener) {
		for (String questionId : questions.keySet()) {
			if (!results.containsKey(questionId)) {
				record(results, state == TerminalState.CANCELLED
						? AskDataSubQuestionResult.cancelled(questionId)
						: AskDataSubQuestionResult.timedOut(questionId), listener);
			}
		}
	}

	private void record(Map<String, AskDataSubQuestionResult> results, AskDataSubQuestionResult result,
			AskDataSubQuestionResultListener listener) {
		results.put(result.getQuestionId(), result);
		listener.onTerminal(result);
	}

	private AskDataMultiQuestionResult orderedResult(Map<String, AskDataSubQuestion> questions,
			Map<String, AskDataSubQuestionResult> results) {
		List<AskDataSubQuestionResult> ordered = new ArrayList<AskDataSubQuestionResult>(questions.size());
		for (String questionId : questions.keySet()) {
			ordered.add(results.get(questionId));
		}
		return new AskDataMultiQuestionResult(ordered);
	}

	private void cancelRunning(Map<String, RunningQuestion> running) {
		for (RunningQuestion question : running.values()) {
			question.future.cancel(true);
		}
		running.clear();
	}

	private void pause(AskDataMultiQuestionRequest request) {
		try {
			TimeUnit.MILLISECONDS.sleep(2L);
		} catch (InterruptedException ex) {
			request.cancel();
			Thread.currentThread().interrupt();
		}
	}

	private long deadlineAfter(long timeoutMillis) {
		long now = System.nanoTime();
		long timeoutNanos = TimeUnit.MILLISECONDS.toNanos(timeoutMillis);
		return Long.MAX_VALUE - now < timeoutNanos ? Long.MAX_VALUE : now + timeoutNanos;
	}

	private enum TerminalState {
		TIMED_OUT,
		CANCELLED
	}

	private static final class RunningQuestion {

		private final AskDataSubQuestion question;
		private final Future<AskDataSubQuestionResult> future;
		private final long deadlineNanos;

		private RunningQuestion(AskDataSubQuestion question, Future<AskDataSubQuestionResult> future,
				long deadlineNanos) {
			this.question = question;
			this.future = future;
			this.deadlineNanos = deadlineNanos;
		}
	}
}
