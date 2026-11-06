package com.xiongx9527.fw.plus.ai.support.resilience;

import com.xiongx9527.fw.plus.ai.base.model.RerankPort;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class FallbackRerankPortTest {

	/** 候选重排端口，记录实际收到的模型名以校验跨端点切换重写。 */
	private static final class ScriptedReranker implements RerankPort {

		final String id;
		final String failMessage;
		/** 前 remainingFails 次抛错，之后成功。 */
		int remainingFails;
		String lastModel;

		ScriptedReranker(String id) {
			this(id, "fail-" + id);
		}

		ScriptedReranker(String id, String failMessage) {
			this.id = id;
			this.failMessage = failMessage;
		}

		@Override
		public List<Double> score(String query, List<String> documents) {
			return score(null, query, documents);
		}

		@Override
		public List<Double> score(String model, String query, List<String> documents) {
			lastModel = model;
			if (remainingFails > 0) {
				remainingFails--;
				throw new RuntimeException(failMessage);
			}
			return Arrays.asList(0.9, 0.1);
		}
	}

	private static final List<String> DOCS = Arrays.asList("a", "b");

	/** 分类器：rate/429/503/timeout → 重试；insufficient/balance/401/403 → 切换；bad_request/400 → 终止。 */
	private static KeywordFailoverErrorClassifier policy() {
		return KeywordFailoverErrorClassifier.of(Arrays.asList(
				"rate:RETRY", "429:RETRY", "503:RETRY", "timeout:RETRY",
				"insufficient:FAILOVER", "balance:FAILOVER", "401:FAILOVER", "403:FAILOVER",
				"bad_request:ABORT", "400:ABORT"));
	}

	@Test
	public void scoreUsesPrimaryWhenHealthy() {
		ScriptedReranker primary = new ScriptedReranker("primary");
		FallbackRerankPort port = new FallbackRerankPort(
				() -> Arrays.asList(new RerankModelCandidate(primary, "primary-model")));
		List<Double> scores = port.score("q", DOCS);
		assertEquals(Arrays.asList(0.9, 0.1), scores);
		assertEquals("primary-model", primary.lastModel);
	}

	@Test
	public void scoreFallsBackToSecondary() {
		ScriptedReranker primary = new ScriptedReranker("primary");
		primary.remainingFails = 1;
		ScriptedReranker secondary = new ScriptedReranker("secondary");
		FallbackRerankPort port = new FallbackRerankPort(() -> Arrays.asList(
				new RerankModelCandidate(primary, "primary-model"),
				new RerankModelCandidate(secondary, "secondary-model")));
		List<Double> scores = port.score("q", DOCS);
		assertEquals(Arrays.asList(0.9, 0.1), scores);
		assertEquals("secondary-model", secondary.lastModel);
	}

	@Test
	public void scoreAllFailThrows() {
		ScriptedReranker primary = new ScriptedReranker("primary");
		primary.remainingFails = 100;
		ScriptedReranker secondary = new ScriptedReranker("secondary");
		secondary.remainingFails = 100;
		FallbackRerankPort port = new FallbackRerankPort(() -> Arrays.asList(
				new RerankModelCandidate(primary, "primary-model"),
				new RerankModelCandidate(secondary, "secondary-model")));
		assertThrows(RuntimeException.class, () -> port.score("q", DOCS));
	}

	@Test
	public void scoreRetriesSameCandidateOnRetryableError() {
		ScriptedReranker primary = new ScriptedReranker("primary", "rate limit exceeded");
		primary.remainingFails = 2; // 前两次限流，第三次成功
		FallbackRerankPort port = new FallbackRerankPort(
				() -> Arrays.asList(new RerankModelCandidate(primary, "primary-model")),
				null, policy(), 3, 0L);
		List<Double> scores = port.score("q", DOCS);
		assertEquals(Arrays.asList(0.9, 0.1), scores);
		assertEquals(0, primary.remainingFails);
	}

	@Test
	public void scoreFallsBackAfterRetriesExhausted() {
		ScriptedReranker primary = new ScriptedReranker("primary", "rate limit exceeded");
		primary.remainingFails = 100;
		ScriptedReranker secondary = new ScriptedReranker("secondary");
		// maxRetries=1：主模型重试 1 次仍失败 → 切到辅助模型
		FallbackRerankPort port = new FallbackRerankPort(() -> Arrays.asList(
				new RerankModelCandidate(primary, "primary-model"),
				new RerankModelCandidate(secondary, "secondary-model")),
				null, policy(), 1, 0L);
		List<Double> scores = port.score("q", DOCS);
		assertEquals(Arrays.asList(0.9, 0.1), scores);
		assertEquals("secondary-model", secondary.lastModel);
	}

	@Test
	public void scoreAbortsOnTerminalError() {
		ScriptedReranker primary = new ScriptedReranker("primary", "bad_request: invalid");
		primary.remainingFails = 1;
		ScriptedReranker secondary = new ScriptedReranker("secondary");
		FallbackRerankPort port = new FallbackRerankPort(() -> Arrays.asList(
				new RerankModelCandidate(primary, "primary-model"),
				new RerankModelCandidate(secondary, "secondary-model")),
				null, policy(), 3, 0L);
		assertThrows(RuntimeException.class, () -> port.score("q", DOCS));
		assertEquals(null, secondary.lastModel); // 终止错误不切换、不重试
	}

	@Test
	public void scoreIgnoresNullCandidateList() {
		FallbackRerankPort port = new FallbackRerankPort(() -> null);
		assertThrows(IllegalStateException.class, () -> port.score("q", Collections.singletonList("x")));
	}
}
