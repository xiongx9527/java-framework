package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.facts;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 任务 2.3：会话级轮次缓冲（batched / session-end 共用）——顺序、冲空语义与并发追加。
 */
class FactTurnBufferTest {

	@Test
	void appendAndDrainKeepOrder() {
		FactTurnBuffer buffer = new FactTurnBuffer();
		buffer.append("s1", "u1", Arrays.asList((ChatMessage) UserMessage.from("第一轮")));
		buffer.append("s1", "u1", Arrays.asList((ChatMessage) UserMessage.from("第二轮")));

		List<FactTurnBuffer.BufferedTurn> drained = buffer.drain("s1");

		assertThat(drained).hasSize(2);
		assertThat(drained.get(0).getUserId()).isEqualTo("u1");
		assertThat(FactExtractors.FactTexts.textOf(drained.get(0).getMessages().get(0))).isEqualTo("第一轮");
		assertThat(FactExtractors.FactTexts.textOf(drained.get(1).getMessages().get(0))).isEqualTo("第二轮");
	}

	@Test
	void drainRemovesBufferedTurns() {
		FactTurnBuffer buffer = new FactTurnBuffer();
		buffer.append("s1", "u1", Arrays.asList((ChatMessage) UserMessage.from("第一轮")));

		assertThat(buffer.size("s1")).isEqualTo(1);
		assertThat(buffer.drain("s1")).hasSize(1);
		assertThat(buffer.drain("s1")).as("已冲空").isEmpty();
		assertThat(buffer.size("s1")).isZero();
	}

	@Test
	void drainUnknownSessionReturnsEmpty() {
		FactTurnBuffer buffer = new FactTurnBuffer();
		assertThat(buffer.drain("nope")).isEmpty();
		assertThat(buffer.drain(null)).isEmpty();
	}

	@Test
	void concurrentAppendsAreAllCaptured() throws Exception {
		FactTurnBuffer buffer = new FactTurnBuffer();
		int threads = 8;
		int perThread = 200;
		ExecutorService pool = Executors.newFixedThreadPool(threads);
		CountDownLatch start = new CountDownLatch(1);
		CountDownLatch done = new CountDownLatch(threads);
		try {
			for (int t = 0; t < threads; t++) {
				pool.execute(() -> {
					try {
						start.await();
						for (int i = 0; i < perThread; i++) {
							buffer.append("s1", "u1", Arrays.asList((ChatMessage) UserMessage.from("消息")));
						}
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					} finally {
						done.countDown();
					}
				});
			}
			start.countDown();
			assertThat(done.await(10, TimeUnit.SECONDS)).isTrue();
		} finally {
			pool.shutdownNow();
		}

		assertThat(buffer.drain("s1")).as("并发追加不丢失").hasSize(threads * perThread);
	}

	@Test
	void concurrentAppendAndDrainNeverLoseTurns() throws Exception {
		FactTurnBuffer buffer = new FactTurnBuffer();
		int rounds = 2000;
		AtomicInteger appended = new AtomicInteger();
		AtomicInteger drained = new AtomicInteger();
		ExecutorService pool = Executors.newFixedThreadPool(2);
		CountDownLatch start = new CountDownLatch(1);
		CountDownLatch done = new CountDownLatch(2);
		try {
			pool.execute(() -> {
				try {
					start.await();
					for (int i = 0; i < rounds; i++) {
						appended.incrementAndGet();
						buffer.append("s1", "u1", Arrays.asList((ChatMessage) UserMessage.from("消息")));
					}
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				} finally {
					done.countDown();
				}
			});
			pool.execute(() -> {
				try {
					start.await();
					for (int i = 0; i < rounds; i++) {
						drained.addAndGet(buffer.drain("s1").size());
					}
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				} finally {
					done.countDown();
				}
			});
			start.countDown();
			assertThat(done.await(15, TimeUnit.SECONDS)).isTrue();
		} finally {
			pool.shutdownNow();
		}

		drained.addAndGet(buffer.drain("s1").size());
		assertThat(drained.get()).as("冲空总数 + 最终残留 = 追加总数（无静默丢轮）").isEqualTo(appended.get());
	}
}
