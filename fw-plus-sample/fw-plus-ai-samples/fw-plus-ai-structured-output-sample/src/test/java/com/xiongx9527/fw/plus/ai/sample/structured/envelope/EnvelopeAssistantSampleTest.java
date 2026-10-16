package com.xiongx9527.fw.plus.ai.sample.structured.envelope;

import com.alibaba.fastjson2.JSONObject;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.envelope.BusinessRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.envelope.ChatEnvelope;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.envelope.EnvelopeRuntime;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.envelope.EnvelopeSessionMemory;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.envelope.Langchain4jAssistantProperties;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 信封样例的端到端自检：四条路径 + 帧协议，全程离线（不需要模型与 Key）。
 *
 * <p><b>可观测性说明</b>：本测试刻意从<b>来源</b>侧断言——「有没有 token 帧」看 token 帧唯一的来源
 * （流式模型有没有被调用、有没有回调分片），「start 帧 data」看它的来源
 * （{@link EnvelopeRuntime.Round#getStartData()}）。真实 SSE 帧本身可以在未初始化时从
 * {@code ResponseBodyEmitter} 的帧缓冲里反射读到，两条通道的信封同形断言见
 * {@link EnvelopeChannelSameShapeTest}。</p>
 */
class EnvelopeAssistantSampleTest {

	private static final String ORDER_MESSAGE = "帮我查订单 A1001 的付款状态";

	private final EnvelopeAssistantSample sample = new EnvelopeAssistantSample();

	// -------------------------------------------------- 路径 1：命中 + 结构化成功

	@Test
	void structuredSuccessPath() {
		String memoryId = "order-1";

		ChatEnvelope envelope = sample.entry().chat(memoryId, ORDER_MESSAGE);

		assertThat(envelope.isOk()).isTrue();
		assertThat(envelope.getBusiness()).isEqualTo(SampleBusinesses.ORDER);
		assertThat(envelope.getVersion()).isEqualTo("1");
		assertThat(envelope.getResult().getString("orderId")).isEqualTo("A1001");
		assertThat(envelope.getResult().getString("status")).isEqualTo("paid");
		assertThat(envelope.getText()).as("结构化成功轮次的 text 为空串（人话在 result 里）").isEmpty();
		assertThat(envelope.toJsonObject().keySet()).as("外层恰好 5 键").containsExactlyElementsOf(ChatEnvelope.keys());
		// 1 次分类 + 1 次生成：一次就通过校验，没有重试
		assertThat(sample.chatModel().requestCount()).isEqualTo(2);
		// 结构化轮次写回历史：本轮用户原文 + 结果摘要
		assertThat(textsOf(memoryId)).hasSize(2);
		assertThat(textsOf(memoryId).get(1)).isEqualTo(EnvelopeSessionMemory.summarize(envelope));
	}

	// -------------------------------------------------- 路径 2：命中但校验失败

	@Test
	void validationFailurePathRetriesThenFails() {
		String memoryId = "order-bad";

		ChatEnvelope envelope = sample.entry().chat(memoryId, "帮我查订单 BAD 的付款状态");

		assertThat(envelope.isOk()).isFalse();
		assertThat(envelope.getBusiness()).isEqualTo(SampleBusinesses.ORDER);
		assertThat(envelope.getResult()).isNull();
		assertThat(envelope.toJsonObject()).as("失败也保持 5 键（result 键在、值为 null）")
				.containsKey(ChatEnvelope.KEY_RESULT);
		assertThat(envelope.getText()).isEqualTo(ChatEnvelope.DEFAULT_FAILURE_TEXT);

		// 1 次分类 + 2 次生成：契约 max-attempts=2，第一次不合法后回喂重试，第二次仍不合法
		assertThat(sample.chatModel().requestCount()).isEqualTo(3);

		// 第二次尝试的上下文里只有「系统提示 + 本轮提示词」：重试期间没有任何历史写入，
		// 而上一轮的错误是通过提示词回喂的（不是通过会话记忆）
		List<ChatMessage> secondAttempt = sample.chatModel().requests().get(2);
		assertThat(secondAttempt).hasSize(2);
		assertThat(secondAttempt.get(0)).isInstanceOf(SystemMessage.class);
		assertThat(secondAttempt.get(1)).isInstanceOf(UserMessage.class);
		assertThat(((UserMessage) secondAttempt.get(1)).text())
				.contains("needMoreInfo=false 时必须给出 orderId");

		// 历史里写的是失败说明，而不是校验错误原文
		assertThat(textsOf(memoryId).get(1)).isEqualTo(ChatEnvelope.DEFAULT_FAILURE_TEXT);
	}

	// -------------------------------------------------- 路径 3：业务追问（两轮）

	@Test
	void followUpPathKeepsBusinessAcrossTurns() {
		String memoryId = "refund-1";

		ChatEnvelope first = sample.entry().chat(memoryId, "我想退款");
		assertThat(first.isOk()).isTrue();
		assertThat(first.getBusiness()).isEqualTo(SampleBusinesses.REFUND);
		assertThat(first.getVersion()).isEqualTo("2");
		assertThat(first.getResult().getBooleanValue("needMoreInfo")).as("追问就是一次成功的业务结果").isTrue();
		assertThat(first.getResult().getString("question")).isNotEmpty();

		ChatEnvelope second = sample.entry().chat(memoryId, "订单号 A1001，理由是收到货破损");
		assertThat(second.isOk()).isTrue();
		assertThat(second.getBusiness()).as("本轮消息里没有“退款”二字，靠历史里上一轮的内容判出业务")
				.isEqualTo(SampleBusinesses.REFUND);
		assertThat(second.getResult().getBooleanValue("needMoreInfo")).isFalse();
		assertThat(second.getResult().getString("orderId")).isEqualTo("A1001");
		assertThat(second.getResult().getString("reason")).isNotEmpty();

		// 第二轮生成时的上下文里带了第一轮写回的历史（摘要含业务名与结果）
		assertThat(sample.chatModel().lastRequestText()).contains("已处理业务 refund");
	}

	// -------------------------------------------------- 路径 4：未命中落闲聊

	@Test
	void unmatchedFallsBackToChat() {
		ChatEnvelope envelope = sample.entry().chat("chat-1", "今天天气怎么样");

		assertThat(envelope.isOk()).isTrue();
		assertThat(envelope.getBusiness()).isEqualTo(BusinessRegistry.FALLBACK);
		assertThat(envelope.getResult()).isNull();
		assertThat(envelope.toJsonObject()).containsKey(ChatEnvelope.KEY_RESULT);
		assertThat(envelope.getText()).as("闲聊轮次的 text 就是模型原文").isEqualTo(RecordingChatModel.CHAT_REPLY);
	}

	// -------------------------------------------------- 帧协议：结构化轮次没有 token 帧

	@Test
	void structuredRoundStreamsNoTokens() {
		String memoryId = "stream-order";

		EnvelopeRuntime.Round round = sample.runtime().decide(memoryId, ORDER_MESSAGE);
		assertThat(round.isStructured()).isTrue();

		JSONObject startData = round.getStartData();
		assertThat(startData.keySet()).containsExactlyInAnyOrder("requestId", "business", "tokens");
		assertThat(startData.getString("requestId")).hasSize(32);
		assertThat(startData.getString("business")).isEqualTo(SampleBusinesses.ORDER);
		assertThat(startData.getBooleanValue("tokens")).as("结构化轮次不逐字下发").isFalse();

		assertThat(sample.assistant().streamToSse(memoryId, ORDER_MESSAGE)).isNotNull();

		// token 帧唯一的来源一次都没被调用：这一轮没有任何 token 帧
		assertThat(sample.streamingModel().requestCount()).isZero();
		assertThat(sample.streamingModel().deliveredTokens()).isEmpty();
		// 结构化轮次照样写回了历史（信封经摘要落进共享历史）
		assertThat(textsOf(memoryId).get(1)).startsWith("已处理业务 " + SampleBusinesses.ORDER);
	}

	@Test
	void chatRoundStreamsTokens() {
		String memoryId = "stream-chat";

		EnvelopeRuntime.Round round = sample.runtime().decide(memoryId, "今天天气怎么样");
		assertThat(round.isStructured()).isFalse();
		assertThat(round.getBusiness()).isEqualTo(BusinessRegistry.FALLBACK);
		assertThat(round.getStartData().getBooleanValue("tokens")).as("闲聊轮次逐字下发").isTrue();

		assertThat(sample.assistant().streamToSse(memoryId, "今天天气怎么样")).isNotNull();

		// 闲聊轮次走既有 AiServices 流式通道：流式模型被调用一次并回调了分片
		assertThat(sample.streamingModel().requestCount()).isEqualTo(1);
		assertThat(sample.streamingModel().deliveredTokens()).isNotEmpty();
	}

	// -------------------------------------------------- 演示本身可跑

	@Test
	void mainRunsOffline() {
		// 五段演示都不依赖模型/Key，跑通即证明样例可用
		EnvelopeAssistantSample.main(new String[0]);
	}

	// -------------------------------------------------- 样例自身配置成对

	/**
	 * 样例自己的配置成对：既有通道按 {@link EnvelopeAssistantSample#MEMORY_WINDOW} 建记忆，信封层按
	 * {@code Session} 的默认窗口读历史/写回。<b>这里钉住的是两个默认值</b>——任一个改动即红灯，
	 * 默认值不会静默地各走一边；装配时的实际接线（构造函数里的 {@code memory(MEMORY_WINDOW)}）
	 * 不在本断言的覆盖范围内。
	 */
	@Test
	void memoryWindowStaysPairedWithEnvelopeSessionWindow() {
		assertThat(EnvelopeAssistantSample.MEMORY_WINDOW)
				.isEqualTo(Langchain4jAssistantProperties.Session.DEFAULT_WINDOW_SIZE);
	}

	// -------------------------------------------------- 工具

	/** 该会话历史里各条消息的文本（结构化轮次写回的内容就在这里） */
	private List<String> textsOf(String memoryId) {
		List<String> texts = new ArrayList<String>();
		for (ChatMessage message : sample.history(memoryId)) {
			if (message instanceof UserMessage) {
				texts.add(((UserMessage) message).text());
			} else if (message instanceof AiMessage) {
				texts.add(((AiMessage) message).text());
			}
		}
		return texts;
	}
}
