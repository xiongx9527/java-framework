package com.xiongx9527.fw.plus.ai.askdata.agent;

import com.xiongx9527.fw.plus.ai.askdata.context.AskDataProjectContext;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AskDataMultiQuestionExecutorTest {

	@Test
	void 无依赖问题应在并发上限内执行且依赖节点随后执行() {
		List<AskDataSubQuestion> questions = Arrays.asList(
				new AskDataSubQuestion("q1", "first", Collections.<String>emptyList()),
				new AskDataSubQuestion("q2", "second", Collections.<String>emptyList()),
				new AskDataSubQuestion("q3", "third", Arrays.asList("q1", "q2")));
		AskDataMultiQuestionRequest request = new AskDataMultiQuestionRequest(context(), "request-1",
				questions, 2000L, 2);
		CountDownLatch independentStarted = new CountDownLatch(2);
		AtomicInteger active = new AtomicInteger();
		AtomicInteger maxActive = new AtomicInteger();

		AskDataMultiQuestionResult result = new AskDataMultiQuestionExecutor().execute(request, question -> {
			int current = active.incrementAndGet();
			updateMax(maxActive, current);
			try {
				if (!"q3".equals(question.getId())) {
					independentStarted.countDown();
					assertTrue(independentStarted.await(1, TimeUnit.SECONDS));
				}
				return AskDataSubQuestionResult.success(question.getId(), question.getQuestion() + "-answer");
			} finally {
				active.decrementAndGet();
			}
		});

		assertEquals(2, maxActive.get());
		assertEquals(AskDataSubQuestionResult.State.SUCCEEDED, result.getResult("q1").getState());
		assertEquals(AskDataSubQuestionResult.State.SUCCEEDED, result.getResult("q2").getState());
		assertEquals(AskDataSubQuestionResult.State.SUCCEEDED, result.getResult("q3").getState());
	}

	@Test
	void 失败节点应保留终态并按策略跳过依赖节点() {
		AskDataMultiQuestionRequest request = new AskDataMultiQuestionRequest(context(), "request-2", Arrays.asList(
				new AskDataSubQuestion("q1", "first", Collections.<String>emptyList()),
				new AskDataSubQuestion("q2", "second", Collections.singletonList("q1")),
				new AskDataSubQuestion("q3", "independent", Collections.<String>emptyList())), 1000L, 2);

		AskDataMultiQuestionResult result = new AskDataMultiQuestionExecutor().execute(request, question ->
				"q1".equals(question.getId())
						? AskDataSubQuestionResult.failed("q1", "MODEL_FAILED", "model failed")
						: AskDataSubQuestionResult.success(question.getId(), "ok"));

		assertEquals(AskDataSubQuestionResult.State.FAILED, result.getResult("q1").getState());
		assertEquals(AskDataSubQuestionResult.State.SKIPPED, result.getResult("q2").getState());
		assertEquals(AskDataSubQuestionResult.State.SUCCEEDED, result.getResult("q3").getState());
	}

	@Test
	void 节点超时应取消任务并跳过依赖节点() {
		AskDataSubQuestion slow = new AskDataSubQuestion("q1", "slow", Collections.<String>emptyList(),
				AskDataSubQuestion.FailurePolicy.SKIP, 30L);
		AskDataSubQuestion dependent = new AskDataSubQuestion("q2", "dependent", Collections.singletonList("q1"));
		AskDataMultiQuestionRequest request = new AskDataMultiQuestionRequest(context(), "request-3",
				Arrays.asList(slow, dependent), 1000L, 1);

		AskDataMultiQuestionResult result = new AskDataMultiQuestionExecutor().execute(request, question -> {
			Thread.sleep(500L);
			return AskDataSubQuestionResult.success(question.getId(), "late");
		});

		assertEquals(AskDataSubQuestionResult.State.TIMED_OUT, result.getResult("q1").getState());
		assertEquals(AskDataSubQuestionResult.State.SKIPPED, result.getResult("q2").getState());
	}

	@Test
	void 已取消请求的所有未开始节点应保持取消终态() {
		AskDataMultiQuestionRequest request = new AskDataMultiQuestionRequest(context(), "request-4", Arrays.asList(
				new AskDataSubQuestion("q1", "first", Collections.<String>emptyList()),
				new AskDataSubQuestion("q2", "second", Collections.<String>emptyList())), 1000L, 2);
		request.cancel();

		AskDataMultiQuestionResult result = new AskDataMultiQuestionExecutor().execute(request,
				question -> AskDataSubQuestionResult.success(question.getId(), "must-not-run"));

		assertEquals(AskDataSubQuestionResult.State.CANCELLED, result.getResult("q1").getState());
		assertEquals(AskDataSubQuestionResult.State.CANCELLED, result.getResult("q2").getState());
	}

	@Test
	void 已恢复终态节点不应重跑且新终态应通知监听器() {
		List<AskDataSubQuestion> questions = Arrays.asList(
				new AskDataSubQuestion("q1", "completed", Collections.<String>emptyList()),
				new AskDataSubQuestion("q2", "pending", Collections.singletonList("q1")));
		AskDataMultiQuestionRequest request = new AskDataMultiQuestionRequest(context(), "request-5",
				questions, 1000L, 1, Collections.singletonList(
						AskDataSubQuestionResult.success("q1", "restored")));
		AtomicInteger calls = new AtomicInteger();
		List<String> notified = new ArrayList<String>();

		AskDataMultiQuestionResult result = new AskDataMultiQuestionExecutor().execute(request, question -> {
			calls.incrementAndGet();
			return AskDataSubQuestionResult.success(question.getId(), "executed");
		}, terminal -> notified.add(terminal.getQuestionId()));

		assertEquals(1, calls.get());
		assertEquals("restored", result.getAnswer("q1"));
		assertEquals(Collections.singletonList("q2"), notified);
	}

	@Test
	void continue策略应在依赖失败后继续执行() {
		AskDataMultiQuestionRequest request = new AskDataMultiQuestionRequest(context(), "request-6", Arrays.asList(
				new AskDataSubQuestion("q1", "failed", Collections.<String>emptyList()),
				new AskDataSubQuestion("q2", "continued", Collections.singletonList("q1"),
						AskDataSubQuestion.FailurePolicy.CONTINUE, 0L)), 1000L, 1);

		AskDataMultiQuestionResult result = new AskDataMultiQuestionExecutor().execute(request, question ->
				"q1".equals(question.getId())
						? AskDataSubQuestionResult.failed("q1", "MODEL_FAILED", "failed")
						: AskDataSubQuestionResult.success("q2", "continued"));

		assertEquals(AskDataSubQuestionResult.State.FAILED, result.getResult("q1").getState());
		assertEquals(AskDataSubQuestionResult.State.SUCCEEDED, result.getResult("q2").getState());
	}

	@Test
	void 循环依赖和缺失恢复依赖应被拒绝() {
		AskDataMultiQuestionRequest cyclic = new AskDataMultiQuestionRequest(context(), "request-7", Arrays.asList(
				new AskDataSubQuestion("q1", "first", Collections.singletonList("q2")),
				new AskDataSubQuestion("q2", "second", Collections.singletonList("q1"))), 1000L, 1);
		assertThrows(IllegalArgumentException.class, () -> new AskDataMultiQuestionExecutor().execute(cyclic,
				question -> AskDataSubQuestionResult.success(question.getId(), "unexpected")));

		AskDataMultiQuestionRequest invalidRestore = new AskDataMultiQuestionRequest(context(), "request-8",
				Arrays.asList(new AskDataSubQuestion("q1", "first", Collections.<String>emptyList()),
						new AskDataSubQuestion("q2", "second", Collections.singletonList("q1"))),
				1000L, 1, Collections.singletonList(AskDataSubQuestionResult.success("q2", "forged")));
		assertThrows(IllegalArgumentException.class, () -> new AskDataMultiQuestionExecutor().execute(invalidRestore,
				question -> AskDataSubQuestionResult.success(question.getId(), "unexpected")));
	}

	private AskDataProjectContext context() {
		return new AskDataProjectContext("project", "operator");
	}

	private void updateMax(AtomicInteger maximum, int current) {
		while (true) {
			int previous = maximum.get();
			if (previous >= current || maximum.compareAndSet(previous, current)) {
				return;
			}
		}
	}
}
