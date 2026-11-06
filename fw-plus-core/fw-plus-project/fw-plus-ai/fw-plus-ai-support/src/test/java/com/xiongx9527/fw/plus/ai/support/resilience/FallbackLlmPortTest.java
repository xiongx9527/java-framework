package com.xiongx9527.fw.plus.ai.support.resilience;

import com.xiongx9527.fw.plus.ai.base.model.ChatMessage;
import com.xiongx9527.fw.plus.ai.base.model.LlmPort;
import com.xiongx9527.fw.plus.ai.base.model.LlmRequest;
import com.xiongx9527.fw.plus.ai.base.model.LlmResponse;
import com.xiongx9527.fw.plus.ai.base.model.LlmStreamHandler;
import com.xiongx9527.fw.plus.ai.base.model.LlmTokenStream;
import com.xiongx9527.fw.plus.ai.base.model.LlmUsage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FallbackLlmPortTest {

	/** 候选端口每次被调用时记录实际收到的模型名，用于校验跨端点切换时 request.model 被重写。 */
	private static final class ScriptedLlm implements LlmPort {

		final String id;
		final StreamAction streamAction;
		final String failMessage;
		/** complete 通道：前 remainingCompleteFails 次抛错，之后成功。 */
		int remainingCompleteFails;
		/** stream 通道：前 remainingStreamFails 次「吐字前报错」，之后按 streamAction 正常。 */
		int remainingStreamFails;
		String lastModel;

		ScriptedLlm(String id, StreamAction streamAction) {
			this(id, streamAction, "err-" + id);
		}

		ScriptedLlm(String id, StreamAction streamAction, String failMessage) {
			this.id = id;
			this.streamAction = streamAction;
			this.failMessage = failMessage;
		}

		@Override
		public LlmResponse complete(LlmRequest request) {
			lastModel = request.getModel();
			if (remainingCompleteFails > 0) {
				remainingCompleteFails--;
				throw new RuntimeException(failMessage);
			}
			return LlmResponse.builder().content("ok-" + request.getModel()).usage(LlmUsage.ZERO).build();
		}

		@Override
		public LlmTokenStream stream(LlmRequest request) {
			lastModel = request.getModel();
			final String msg = failMessage;
			if (remainingStreamFails > 0) {
				remainingStreamFails--;
				return LlmTokenStream.create(h -> h.onError(msg));
			}
			return LlmTokenStream.create(handler -> streamAction.run(handler, id, msg));
		}
	}

	private enum StreamAction {
		/** 正常吐字并完成。 */
		NORMAL((h, id, msg) -> {
			h.onToken("tok-" + id, null);
			h.onComplete();
		}),
		/** 尚未吐字就报错（应触发透明切换）。 */
		ERROR_BEFORE_TOKEN((h, id, msg) -> h.onError(msg)),
		/** 已吐字后才报错（应如实上报，不再切换）。 */
		TOKEN_THEN_ERROR((h, id, msg) -> {
			h.onToken("tok-" + id, null);
			h.onError(msg);
		});

		private final StreamFn fn;

		StreamAction(StreamFn fn) {
			this.fn = fn;
		}

		void run(LlmStreamHandler h, String id, String msg) {
			fn.run(h, id, msg);
		}
	}

	private interface StreamFn {
		void run(LlmStreamHandler h, String id, String msg);
	}

	private static final class Collector implements LlmStreamHandler {
		final List<String> tokens = new ArrayList<>();
		String error;
		boolean completed;

		@Override
		public void onToken(String content, String reasoningContent) {
			if (content != null) {
				tokens.add(content);
			}
		}

		@Override
		public void onComplete() {
			completed = true;
		}

		@Override
		public void onError(String message) {
			error = message;
		}
	}

	private static LlmRequest req() {
		return LlmRequest.builder()
				.messages(Collections.singletonList(ChatMessage.user("hi")))
				.build();
	}

	@Test
	public void completeUsesPrimaryWhenHealthy() {
		ScriptedLlm primary = new ScriptedLlm("primary", StreamAction.NORMAL);
		FallbackLlmPort port = new FallbackLlmPort(
				() -> Arrays.asList(new LlmModelCandidate(primary, "primary-model")));
		LlmResponse resp = port.complete(req());
		assertEquals("ok-primary-model", resp.getContent());
		assertEquals("primary-model", primary.lastModel);
	}

	@Test
	public void completeFallsBackToSecondary() {
		ScriptedLlm primary = new ScriptedLlm("primary", StreamAction.NORMAL);
		primary.remainingCompleteFails = 1; // 主模型首次失败
		ScriptedLlm secondary = new ScriptedLlm("secondary", StreamAction.NORMAL);
		FallbackLlmPort port = new FallbackLlmPort(() -> Arrays.asList(
				new LlmModelCandidate(primary, "primary-model"),
				new LlmModelCandidate(secondary, "secondary-model")));
		LlmResponse resp = port.complete(req());
		// 主模型失败，应切换到辅助模型并返回其输出
		assertEquals("ok-secondary-model", resp.getContent());
		assertEquals("secondary-model", secondary.lastModel);
	}

	@Test
	public void completeAllFailThrows() {
		ScriptedLlm primary = new ScriptedLlm("primary", StreamAction.NORMAL);
		primary.remainingCompleteFails = 100;
		ScriptedLlm secondary = new ScriptedLlm("secondary", StreamAction.NORMAL);
		secondary.remainingCompleteFails = 100;
		FallbackLlmPort port = new FallbackLlmPort(() -> Arrays.asList(
				new LlmModelCandidate(primary, "primary-model"),
				new LlmModelCandidate(secondary, "secondary-model")));
		assertThrows(RuntimeException.class, () -> port.complete(req()));
	}

	@Test
	public void streamFallsBackBeforeFirstToken() {
		ScriptedLlm primary = new ScriptedLlm("primary", StreamAction.ERROR_BEFORE_TOKEN);
		ScriptedLlm secondary = new ScriptedLlm("secondary", StreamAction.NORMAL);
		FallbackLlmPort port = new FallbackLlmPort(() -> Arrays.asList(
				new LlmModelCandidate(primary, "primary-model"),
				new LlmModelCandidate(secondary, "secondary-model")));
		Collector c = new Collector();
		port.stream(req()).handler(c).start();
		// 主模型吐字前报错，透明切到辅助模型，调用方只看到辅助模型的输出，无错误
		assertEquals(Arrays.asList("tok-secondary"), c.tokens);
		assertTrue(c.completed);
		assertEquals(null, c.error);
	}

	@Test
	public void streamForwardsErrorAfterToken() {
		ScriptedLlm primary = new ScriptedLlm("primary", StreamAction.TOKEN_THEN_ERROR);
		ScriptedLlm secondary = new ScriptedLlm("secondary", StreamAction.NORMAL);
		FallbackLlmPort port = new FallbackLlmPort(() -> Arrays.asList(
				new LlmModelCandidate(primary, "primary-model"),
				new LlmModelCandidate(secondary, "secondary-model")));
		Collector c = new Collector();
		port.stream(req()).handler(c).start();
		// 已吐字后失败，如实上报错误，不切换到辅助模型
		assertEquals(Arrays.asList("tok-primary"), c.tokens);
		assertEquals("err-primary", c.error);
		assertTrue(!c.completed);
	}

	@Test
	public void streamAllFailBeforeTokenReportsError() {
		ScriptedLlm primary = new ScriptedLlm("primary", StreamAction.ERROR_BEFORE_TOKEN);
		ScriptedLlm secondary = new ScriptedLlm("secondary", StreamAction.ERROR_BEFORE_TOKEN);
		FallbackLlmPort port = new FallbackLlmPort(() -> Arrays.asList(
				new LlmModelCandidate(primary, "primary-model"),
				new LlmModelCandidate(secondary, "secondary-model")));
		Collector c = new Collector();
		port.stream(req()).handler(c).start();
		assertTrue(c.tokens.isEmpty());
		assertEquals("err-secondary", c.error);
		assertTrue(!c.completed);
	}

	// ------------------------------------------------------------------
	// 错误分类策略（RETRY / FAILOVER / ABORT，由产品侧配置）
	// ------------------------------------------------------------------

	/** 分类器：rate/429/503/timeout → 重试；insufficient/balance/401/403 → 切换；bad_request/400 → 终止。 */
	private static KeywordFailoverErrorClassifier policy() {
		return KeywordFailoverErrorClassifier.of(Arrays.asList(
				"rate:RETRY", "429:RETRY", "503:RETRY", "timeout:RETRY",
				"insufficient:FAILOVER", "balance:FAILOVER", "401:FAILOVER", "403:FAILOVER",
				"bad_request:ABORT", "400:ABORT"));
	}

	@Test
	public void completeRetriesSameCandidateOnRetryableError() {
		ScriptedLlm primary = new ScriptedLlm("primary", StreamAction.NORMAL, "rate limit exceeded");
		primary.remainingCompleteFails = 2; // 前两次限流，第三次成功
		FallbackLlmPort port = new FallbackLlmPort(
				() -> Arrays.asList(new LlmModelCandidate(primary, "primary-model")),
				null, policy(), 3, 0L);
		LlmResponse resp = port.complete(req());
		assertEquals("ok-primary-model", resp.getContent());
		assertEquals(0, primary.remainingCompleteFails); // 重试后成功
	}

	@Test
	public void completeFallsBackAfterRetriesExhausted() {
		ScriptedLlm primary = new ScriptedLlm("primary", StreamAction.NORMAL, "rate limit exceeded");
		primary.remainingCompleteFails = 100; // 始终限流
		ScriptedLlm secondary = new ScriptedLlm("secondary", StreamAction.NORMAL);
		// maxRetries=1：主模型重试 1 次仍失败 → 切到辅助模型
		FallbackLlmPort port = new FallbackLlmPort(() -> Arrays.asList(
				new LlmModelCandidate(primary, "primary-model"),
				new LlmModelCandidate(secondary, "secondary-model")),
				null, policy(), 1, 0L);
		LlmResponse resp = port.complete(req());
		assertEquals("ok-secondary-model", resp.getContent());
	}

	@Test
	public void completeAbortsOnTerminalError() {
		ScriptedLlm primary = new ScriptedLlm("primary", StreamAction.NORMAL, "bad_request: invalid param");
		primary.remainingCompleteFails = 1;
		ScriptedLlm secondary = new ScriptedLlm("secondary", StreamAction.NORMAL);
		FallbackLlmPort port = new FallbackLlmPort(() -> Arrays.asList(
				new LlmModelCandidate(primary, "primary-model"),
				new LlmModelCandidate(secondary, "secondary-model")),
				null, policy(), 3, 0L);
		assertThrows(RuntimeException.class, () -> port.complete(req()));
		assertEquals(null, secondary.lastModel); // 终止错误不切换、不重试
	}

	@Test
	public void streamRetriesSameCandidateOnRetryableError() {
		ScriptedLlm primary = new ScriptedLlm("primary", StreamAction.NORMAL, "rate limit exceeded");
		primary.remainingStreamFails = 2; // 前两次吐字前限流，第三次成功
		FallbackLlmPort port = new FallbackLlmPort(
				() -> Arrays.asList(new LlmModelCandidate(primary, "primary-model")),
				null, policy(), 3, 0L);
		Collector c = new Collector();
		port.stream(req()).handler(c).start();
		assertEquals(Arrays.asList("tok-primary"), c.tokens);
		assertTrue(c.completed);
		assertEquals(null, c.error);
	}

	@Test
	public void streamAbortsOnTerminalError() {
		ScriptedLlm primary = new ScriptedLlm("primary", StreamAction.ERROR_BEFORE_TOKEN, "bad_request: invalid");
		ScriptedLlm secondary = new ScriptedLlm("secondary", StreamAction.NORMAL);
		FallbackLlmPort port = new FallbackLlmPort(() -> Arrays.asList(
				new LlmModelCandidate(primary, "primary-model"),
				new LlmModelCandidate(secondary, "secondary-model")),
				null, policy(), 3, 0L);
		Collector c = new Collector();
		port.stream(req()).handler(c).start();
		assertTrue(c.tokens.isEmpty());
		assertEquals("bad_request: invalid", c.error);
		assertEquals(null, secondary.lastModel); // 终止错误不切换
	}
}
