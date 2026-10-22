package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.facts;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 任务 2.4：异步调度器的拒绝语义——有界队列满时丢弃（{@code submit} 返回 false），绝不阻塞调用方。
 */
class FactWriteSchedulerTest {

	@Test
	void submitRunsTask() throws Exception {
		CountDownLatch ran = new CountDownLatch(1);

		assertThat(FactWriteScheduler.submit(ran::countDown)).isTrue();
		assertThat(ran.await(5, TimeUnit.SECONDS)).isTrue();
	}

	@Test
	void queueFullRejectsInsteadOfBlocking() throws Exception {
		CountDownLatch occupying = new CountDownLatch(1);
		CountDownLatch release = new CountDownLatch(1);
		assertThat(FactWriteScheduler.submit(() -> {
			occupying.countDown();
			try {
				release.await(5, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		})).isTrue();
		assertThat(occupying.await(5, TimeUnit.SECONDS)).as("占位任务已进入执行线程").isTrue();
		try {
			for (int i = 0; i < FactWriteScheduler.QUEUE_CAPACITY; i++) {
				assertThat(FactWriteScheduler.submit(() -> {
					// 空任务：仅用于占满队列
				})).as("队列第 %d 个任务应入队", i + 1).isTrue();
			}
			assertThat(FactWriteScheduler.submit(() -> {
			})).as("队列满：拒绝而不阻塞").isFalse();
		} finally {
			release.countDown();
		}
		// 消化队列，避免影响同 JVM 后续用例
		long deadline = System.currentTimeMillis() + 10_000L;
		while (FactWriteScheduler.queueSize() > 0 && System.currentTimeMillis() < deadline) {
			Thread.sleep(10L);
		}
		assertThat(FactWriteScheduler.queueSize()).isZero();
	}
}
