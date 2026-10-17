package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.facts;

import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.context.ContextRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * 任务 2.4：抽取与写入的失败隔离——抽取服务抛异常时本轮对话不中断且仅记录失败。
 *
 * <p>内联与异步同语义：异常在 {@code FactMaintenance} 边界被吞掉并记日志；
 * 失败不熔断（后续轮次继续尝试），调度器线程不被失败任务拖垮。</p>
 */
class FactMaintenanceFailureTest {

	private static final String MEM_ID = "t1:crm:bot:u1";

	@Test
	void inlineExtractionFailureIsSwallowedAndNextTurnStillAttempts() {
		AtomicInteger attempts = new AtomicInteger();
		FactExtractor failing = (userId, turnMessages) -> {
			attempts.incrementAndGet();
			throw new IllegalStateException("mem0 不可用");
		};
		FactMaintenance maintenance = new FactMaintenance(failing, FactWriteMode.PER_TURN, 5, false,
				new FactTurnBuffer());

		assertThatCode(() -> maintain(maintenance, "回复一")).doesNotThrowAnyException();
		assertThatCode(() -> maintain(maintenance, "回复二")).doesNotThrowAnyException();

		assertThat(attempts.get()).as("失败不熔断，后续轮次继续尝试").isEqualTo(2);
	}

	@Test
	void asyncExtractionFailureIsRecordedWithoutPropagation() throws Exception {
		CountDownLatch failed = new CountDownLatch(1);
		FactExtractor failing = (userId, turnMessages) -> {
			failed.countDown();
			throw new IllegalStateException("mem0 不可用");
		};
		FactMaintenance maintenance = new FactMaintenance(failing, FactWriteMode.PER_TURN, 5, true,
				new FactTurnBuffer());

		assertThatCode(() -> maintain(maintenance, "回复")).doesNotThrowAnyException();
		assertThat(failed.await(5, TimeUnit.SECONDS)).isTrue();
	}

	@Test
	void asyncSchedulerSurvivesFailedTask() throws Exception {
		AtomicInteger attempts = new AtomicInteger();
		CountDownLatch first = new CountDownLatch(1);
		CountDownLatch second = new CountDownLatch(1);
		FactExtractor extractor = (userId, turnMessages) -> {
			if (attempts.incrementAndGet() == 1) {
				first.countDown();
				throw new IllegalStateException("mem0 不可用");
			}
			second.countDown();
		};
		FactMaintenance maintenance = new FactMaintenance(extractor, FactWriteMode.PER_TURN, 5, true,
				new FactTurnBuffer());

		maintain(maintenance, "回复一");
		assertThat(first.await(5, TimeUnit.SECONDS)).isTrue();

		maintain(maintenance, "回复二");
		assertThat(second.await(5, TimeUnit.SECONDS)).isTrue();
		assertThat(attempts.get()).isEqualTo(2);
	}

	@Test
	void inlineErrorIsAlsoIsolated() {
		FactExtractor failing = (userId, turnMessages) -> {
			throw new AssertionError("模拟 Error 级失败");
		};
		FactMaintenance maintenance = new FactMaintenance(failing, FactWriteMode.PER_TURN, 5, false,
				new FactTurnBuffer());

		assertThatCode(() -> maintain(maintenance, "回复")).doesNotThrowAnyException();
	}

	// -------------------------------------------------- 基础设施

	private static void maintain(FactMaintenance maintenance, String aiText) {
		maintenance.maintain(new ContextRequest(MEM_ID, "s1", "u1", "你好"),
				Collections.<ChatMessage>emptyList(), AiMessage.from(aiText));
	}
}
