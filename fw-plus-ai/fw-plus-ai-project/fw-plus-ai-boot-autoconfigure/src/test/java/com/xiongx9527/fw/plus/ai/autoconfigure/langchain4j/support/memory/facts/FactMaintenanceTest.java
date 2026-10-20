package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.facts;

import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.context.ContextRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageJsonCodec;
import dev.langchain4j.data.message.GsonChatMessageJsonCodec;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.UserMessage;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 任务 2.3：三种写入模式（per-turn / batched / session-end）的触发时机、作用域守卫与调度。
 *
 * <p>全部以内联（{@code extractAsync=false}）驱动以获得确定性；异步路径单独一条。</p>
 */
class FactMaintenanceTest {

	private static final String MEM_ID = "t1:crm:bot:u1";

	private static final ChatMessageJsonCodec CODEC = new GsonChatMessageJsonCodec();

	@Test
	void writeModeFromConfigMapsAndDefaults() {
		assertThat(FactWriteMode.fromConfig("per-turn")).isEqualTo(FactWriteMode.PER_TURN);
		assertThat(FactWriteMode.fromConfig("batched")).isEqualTo(FactWriteMode.BATCHED);
		assertThat(FactWriteMode.fromConfig("session-end")).isEqualTo(FactWriteMode.SESSION_END);
		assertThat(FactWriteMode.fromConfig(" SESSION-END ")).isEqualTo(FactWriteMode.SESSION_END);
		assertThat(FactWriteMode.fromConfig(null)).isEqualTo(FactWriteMode.PER_TURN);
		assertThat(FactWriteMode.fromConfig("bogus")).isEqualTo(FactWriteMode.PER_TURN);
	}

	// -------------------------------------------------- per-turn

	@Test
	void perTurnSubmitsEveryTurnInline() {
		RecordingExtractor extractor = new RecordingExtractor();
		FactMaintenance maintenance = new FactMaintenance(extractor, FactWriteMode.PER_TURN, 5, false,
				new FactTurnBuffer());

		maintenance.maintain(request(MEM_ID, "s1", "你好"), emptyMessages(), AiMessage.from("回复一"));
		maintenance.maintain(request(MEM_ID, "s1", "再来"), emptyMessages(), AiMessage.from("回复二"));

		assertThat(extractor.userIds).containsExactly("u1", "u1");
		assertThat(extractor.turns).hasSize(2);
		assertThat(jsonOf(extractor.turns.get(0))).isEqualTo(jsonOf(Arrays.asList(
				UserMessage.from("你好"), AiMessage.from("回复一"))));
		assertThat(jsonOf(extractor.turns.get(1))).isEqualTo(jsonOf(Arrays.asList(
				UserMessage.from("再来"), AiMessage.from("回复二"))));
	}

	@Test
	void perTurnSkipsToolCallIntermediateAiMessages() {
		RecordingExtractor extractor = new RecordingExtractor();
		FactMaintenance maintenance = new FactMaintenance(extractor, FactWriteMode.PER_TURN, 5, false,
				new FactTurnBuffer());
		AiMessage toolCall = AiMessage.from("先查一下", Arrays.asList(
				ToolExecutionRequest.builder().id("call-1").name("queryOrder").arguments("{}").build()));

		maintenance.maintain(request(MEM_ID, "s1", "帮我查订单"), emptyMessages(), toolCall);

		assertThat(extractor.turns).as("工具循环中间态不触发抽取").isEmpty();
	}

	// -------------------------------------------------- 作用域守卫

	@Test
	void skipsWhenNoExplicitUserSegment() {
		RecordingExtractor extractor = new RecordingExtractor();
		FactMaintenance maintenance = new FactMaintenance(extractor, FactWriteMode.PER_TURN, 5, false,
				new FactTurnBuffer());

		maintenance.maintain(request("t1:crm:bot", "s1", "你好"), emptyMessages(), AiMessage.from("回复"));

		assertThat(extractor.turns).as("无显式用户段跳过").isEmpty();
	}

	@Test
	void readTimeFallbackDoesNotExtract() {
		RecordingExtractor extractor = new RecordingExtractor();
		FactMaintenance maintenance = new FactMaintenance(extractor, FactWriteMode.PER_TURN, 5, false,
				new FactTurnBuffer());

		maintenance.maintain(request(MEM_ID, "s1", "你好"), emptyMessages(), null);

		assertThat(extractor.turns).as("读时兜底（无当轮消息）不触发抽取").isEmpty();
	}

	@Test
	void skipsNonAiTurnMessages() {
		RecordingExtractor extractor = new RecordingExtractor();
		FactMaintenance maintenance = new FactMaintenance(extractor, FactWriteMode.PER_TURN, 5, false,
				new FactTurnBuffer());

		maintenance.maintain(request(MEM_ID, "s1", "你好"), emptyMessages(), UserMessage.from("中途的用户消息"));

		assertThat(extractor.turns).as("非 AI 消息不触发抽取").isEmpty();
	}

	@Test
	void skipsWhenBothTurnMessagesBlank() {
		RecordingExtractor extractor = new RecordingExtractor();
		FactMaintenance maintenance = new FactMaintenance(extractor, FactWriteMode.PER_TURN, 5, false,
				new FactTurnBuffer());

		maintenance.maintain(request(MEM_ID, "s1", "   "), emptyMessages(), AiMessage.from("   "));

		assertThat(extractor.turns).as("本轮消息全空不触发抽取").isEmpty();
	}

	// -------------------------------------------------- batched

	@Test
	void batchedBuffersUntilThresholdThenSubmitsOneBatch() {
		RecordingExtractor extractor = new RecordingExtractor();
		FactMaintenance maintenance = new FactMaintenance(extractor, FactWriteMode.BATCHED, 2, false,
				new FactTurnBuffer());

		maintenance.maintain(request(MEM_ID, "s1", "第一轮"), emptyMessages(), AiMessage.from("回复一"));
		assertThat(extractor.turns).as("未达阈值不抽取").isEmpty();

		maintenance.maintain(request(MEM_ID, "s1", "第二轮"), emptyMessages(), AiMessage.from("回复二"));

		assertThat(extractor.userIds).containsExactly("u1");
		assertThat(extractor.turns).hasSize(1);
		assertThat(jsonOf(extractor.turns.get(0))).isEqualTo(jsonOf(Arrays.asList(
				UserMessage.from("第一轮"), AiMessage.from("回复一"),
				UserMessage.from("第二轮"), AiMessage.from("回复二"))));
	}

	@Test
	void batchedFlushesPreviousSessionOnSessionSwitch() {
		RecordingExtractor extractor = new RecordingExtractor();
		FactMaintenance maintenance = new FactMaintenance(extractor, FactWriteMode.BATCHED, 5, false,
				new FactTurnBuffer());

		maintenance.maintain(request(MEM_ID, "s1", "第一轮"), emptyMessages(), AiMessage.from("回复一"));
		assertThat(extractor.turns).isEmpty();

		maintenance.maintain(request(MEM_ID, "s2", "换会话"), emptyMessages(), AiMessage.from("回复二"));

		assertThat(extractor.turns).as("会话切换冲空旧会话").hasSize(1);
		assertThat(jsonOf(extractor.turns.get(0))).isEqualTo(jsonOf(Arrays.asList(
				UserMessage.from("第一轮"), AiMessage.from("回复一"))));
	}

	// -------------------------------------------------- session-end

	@Test
	void sessionEndWritesOnlyWhenSessionEnds() {
		RecordingExtractor extractor = new RecordingExtractor();
		FactMaintenance maintenance = new FactMaintenance(extractor, FactWriteMode.SESSION_END, 5, false,
				new FactTurnBuffer());

		maintenance.maintain(request(MEM_ID, "s1", "第一轮"), emptyMessages(), AiMessage.from("回复一"));
		maintenance.maintain(request(MEM_ID, "s1", "第二轮"), emptyMessages(), AiMessage.from("回复二"));
		assertThat(extractor.turns).as("会话进行中不写入").isEmpty();

		maintenance.maintain(request(MEM_ID, "s2", "新会话"), emptyMessages(), AiMessage.from("回复三"));

		assertThat(extractor.turns).as("会话结束集中写入上一会话").hasSize(1);
		assertThat(jsonOf(extractor.turns.get(0))).isEqualTo(jsonOf(Arrays.asList(
				UserMessage.from("第一轮"), AiMessage.from("回复一"),
				UserMessage.from("第二轮"), AiMessage.from("回复二"))));
	}

	@Test
	void explicitFlushDrainsSessionBuffer() {
		RecordingExtractor extractor = new RecordingExtractor();
		FactMaintenance maintenance = new FactMaintenance(extractor, FactWriteMode.SESSION_END, 5, false,
				new FactTurnBuffer());

		maintenance.maintain(request(MEM_ID, "s1", "第一轮"), emptyMessages(), AiMessage.from("回复一"));
		maintenance.flush("s1");

		assertThat(extractor.turns).hasSize(1);

		maintenance.flush("s1");
		assertThat(extractor.turns).as("冲空幂等").hasSize(1);
	}

	// -------------------------------------------------- 异步调度

	@Test
	void asyncModeSubmitsInBackground() throws Exception {
		RecordingExtractor extractor = new RecordingExtractor();
		FactMaintenance maintenance = new FactMaintenance(extractor, FactWriteMode.PER_TURN, 5, true,
				new FactTurnBuffer());

		maintenance.maintain(request(MEM_ID, "s1", "你好"), emptyMessages(), AiMessage.from("回复"));

		assertThat(extractor.latch.await(5, TimeUnit.SECONDS)).isTrue();
		assertThat(extractor.userIds).containsExactly("u1");
	}

	// -------------------------------------------------- 基础设施

	private static List<ChatMessage> emptyMessages() {
		return Collections.emptyList();
	}

	private static ContextRequest request(String memId, String sessionKey, String latestUserMessage) {
		return new ContextRequest(memId, sessionKey, "u1", latestUserMessage);
	}

	private static String jsonOf(List<ChatMessage> messages) {
		return CODEC.messagesToJson(messages);
	}

	/** 进程内记录桩：记录抽取调用；latch 供异步模式等待。 */
	private static final class RecordingExtractor implements FactExtractor {

		final List<String> userIds = new CopyOnWriteArrayList<String>();

		final List<List<ChatMessage>> turns = new CopyOnWriteArrayList<List<ChatMessage>>();

		final CountDownLatch latch = new CountDownLatch(1);

		@Override
		public void extract(String userId, List<ChatMessage> turnMessages) {
			userIds.add(userId);
			turns.add(new ArrayList<ChatMessage>(turnMessages));
			latch.countDown();
		}
	}
}
