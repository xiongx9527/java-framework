package com.xiongx9527.fw.plus.ai.sample.structured.envelope;

import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.assistant.AssistantBuilder;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.assistant.SimpleAssistant;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.envelope.BusinessRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.envelope.ChatEnvelope;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.envelope.EnvelopeAssistant;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.envelope.EnvelopeRuntime;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.envelope.EnvelopeSessionMemory;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.envelope.Langchain4jAssistantProperties;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * 信封助手样例：<b>一个入口 + N 份业务契约</b>，外层永远是同一份 5 键 JSON。
 *
 * <p>入口只有两个参数：{@code memoryId} 与用户消息——业务不由调用方指定，只由本轮的分类结果决定
 * （分类输入含最近几条用户消息，所以“那第二单呢”这种追问也能判对业务）。</p>
 *
 * <p>组装三件事，全在构造函数里：</p>
 *
 * <ol>
 *   <li>{@link ChatMemoryStore}：一份会话历史，信封层与既有对话通道<b>共用同一个 store</b>
 *       （不然结构化轮次写的历史，闲聊轮次看不到）；</li>
 *   <li>{@link BusinessRegistry}：登记 {@link SampleBusinesses} 的两份契约；</li>
 *   <li>{@link EnvelopeRuntime}：分类与结构化生成都直连模型，不经 AiServices；
 *       会话窗口 {@link #MEMORY_WINDOW} 与既有通道的 {@code memory(...)} 保持一致。</li>
 * </ol>
 *
 * <p>{@link #main(String[])} <b>离线跑通四条路径</b>（不需要模型、不需要 Key）：命中并结构化成功、
 * 命中但校验失败、业务追问（两轮）、未命中落闲聊；另加一段帧协议的演示（结构化轮次没有 token 帧）。</p>
 *
 * <p>接真实模型：把构造函数里的两个假模型换成真模型（任何 langchain4j {@code ChatLanguageModel} /
 * {@code StreamingChatLanguageModel}）；Spring 场景里这些 Bean 由
 * {@code fw.plus.ai.langchain4j.assistant.envelope.enabled=true} 触发装配，不用手写这段组装。</p>
 */
public final class EnvelopeAssistantSample {

	/** 会话窗口：信封层读快照/写回 与 既有通道的记忆窗口要成对配置，否则两条通道看到的历史不一样 */
	public static final int MEMORY_WINDOW = 20;

	private final ChatMemoryStore memoryStore;

	private final RecordingChatModel chatModel;

	private final RecordingStreamingChatModel streamingModel;

	private final EnvelopeRuntime runtime;

	private final SimpleAssistant assistant;

	private final EnvelopeAssistant entry;

	public EnvelopeAssistantSample() {
		this(new RecordingChatModel(), new RecordingStreamingChatModel());
	}

	public EnvelopeAssistantSample(RecordingChatModel chatModel, RecordingStreamingChatModel streamingModel) {
		this.chatModel = chatModel;
		this.streamingModel = streamingModel;
		this.memoryStore = new InMemoryChatMemoryStore();
		this.runtime = new EnvelopeRuntime(chatModel, memoryStore, SampleBusinesses.registry(), properties());
		// Spring 场景下这一步由装配器自动完成（开关开启后，它会把 runtime 装进容器里每个 SimpleAssistant）
		this.assistant = new AssistantBuilder()
				.chatModel(chatModel)
				.streamingModel(streamingModel)
				.memory(MEMORY_WINDOW)
				.memoryStore(memoryStore)
				.build()
				.envelopeRuntime(runtime);
		this.entry = new EnvelopeAssistant(this.runtime, this.assistant);
	}

	/**
	 * 配置全用默认值：默认「结构化全局关」，但两份契约都写了 {@code structured(true)}，
	 * 所以这两份业务照样产出结构化结果（覆盖顺序：单次调用 &gt; 业务契约 &gt; 全局默认）。
	 */
	private static Langchain4jAssistantProperties properties() {
		Langchain4jAssistantProperties properties = new Langchain4jAssistantProperties();
		// 本样例手动安装，等价于把开关打开；Spring 场景下该开关决定是否装配
		properties.getEnvelope().setEnabled(true);
		return properties;
	}

	/** 通用入口（非流式）：一轮进、一个信封出，永不抛 */
	public EnvelopeAssistant entry() {
		return entry;
	}

	/** 信封编排件：分类 + 结构化生成 + 会话记忆 */
	public EnvelopeRuntime runtime() {
		return runtime;
	}

	/** 既有对话通道（已装上信封处理器：无 mapper 的流式入口按帧协议输出） */
	public SimpleAssistant assistant() {
		return assistant;
	}

	/** 假非流式模型（记录过哪些请求） */
	public RecordingChatModel chatModel() {
		return chatModel;
	}

	/** 假流式模型（token 帧的唯一来源） */
	public RecordingStreamingChatModel streamingModel() {
		return streamingModel;
	}

	/** 该会话的历史（结构化轮次写回的内容可在测试里观察，见 {@link EnvelopeSessionMemory#summarize}） */
	public List<ChatMessage> history(String memoryId) {
		return memoryStore.getMessages(memoryId);
	}

	// -------------------------------------------------- 离线演示

	public static void main(String[] args) {
		demoStructuredSuccess();
		demoValidationFailure();
		demoFollowUp();
		demoFallbackChat();
		demoStreamingFrames();
	}

	/** 路径 1：命中业务 + 结构化成功 —— 一轮请求出 5 键信封，业务结果在 result 里 */
	private static void demoStructuredSuccess() {
		EnvelopeAssistantSample sample = new EnvelopeAssistantSample();
		String memoryId = "demo-order";
		String message = "帮我查订单 A1001 的付款状态";

		System.out.println("=== 路径 1：命中业务 + 结构化成功 ===");
		System.out.println("用户：" + message);

		ChatEnvelope envelope = sample.entry().chat(memoryId, message);

		System.out.println("信封：" + envelope.toJsonString());
		System.out.println("业务结果取字段：orderId=" + envelope.getResult().getString("orderId")
				+ "，status=" + envelope.getResult().getString("status"));
		System.out.println("模型请求：" + sample.chatModel().requestCount() + " 次（1 次分类 + 1 次生成）");
		System.out.println("写回历史：" + EnvelopeSessionMemory.summarize(envelope));
		System.out.println();
	}

	/** 路径 2：命中但校验失败 —— 错误回喂重试仍不合法，最终 ok=false，历史里只留失败说明 */
	private static void demoValidationFailure() {
		EnvelopeAssistantSample sample = new EnvelopeAssistantSample();
		String memoryId = "demo-order-bad";
		String message = "帮我查订单 BAD 的付款状态";

		System.out.println("=== 路径 2：命中但校验失败（契约 max-attempts=2） ===");
		System.out.println("用户：" + message);

		ChatEnvelope envelope = sample.entry().chat(memoryId, message);

		System.out.println("信封：" + envelope.toJsonString());
		System.out.println("result 键仍在？" + envelope.toJsonObject().containsKey(ChatEnvelope.KEY_RESULT)
				+ "（值为 null：成功失败同形）");
		System.out.println("模型请求：" + sample.chatModel().requestCount()
				+ " 次（1 次分类 + 2 次生成：第一次不合法，回喂重试后仍不合法）");
		System.out.println("写回历史：" + EnvelopeSessionMemory.summarize(envelope) + "（校验错误原文不进历史）");
		System.out.println();
	}

	/** 路径 3：业务追问 —— 追问就是一次成功的业务结果；第二轮靠历史仍判为同一业务 */
	private static void demoFollowUp() {
		EnvelopeAssistantSample sample = new EnvelopeAssistantSample();
		String memoryId = "demo-refund";

		System.out.println("=== 路径 3：业务追问（同一会话两轮） ===");

		ChatEnvelope first = sample.entry().chat(memoryId, "我想退款");
		System.out.println("第 1 轮用户：我想退款");
		System.out.println("信封：" + first.toJsonString());
		System.out.println("追问信息在 result 内部：question=" + first.getResult().getString("question"));

		ChatEnvelope second = sample.entry().chat(memoryId, "订单号 A1001，理由是收到货破损");
		System.out.println("第 2 轮用户：订单号 A1001，理由是收到货破损");
		System.out.println("信封：" + second.toJsonString());
		System.out.println("第 2 轮业务仍是 " + second.getBusiness() + "（本轮消息里没有“退款”二字，"
				+ "靠的是历史里上一轮的原文与结果摘要）");
		System.out.println();
	}

	/** 路径 4：未命中任何业务 —— 落兜底 chat，正文走既有 AiServices 通道（这一轮的记忆由它写） */
	private static void demoFallbackChat() {
		EnvelopeAssistantSample sample = new EnvelopeAssistantSample();
		String memoryId = "demo-chat";
		String message = "今天天气怎么样";

		System.out.println("=== 路径 4：未命中落闲聊 ===");
		System.out.println("用户：" + message);

		ChatEnvelope envelope = sample.entry().chat(memoryId, message);

		System.out.println("信封：" + envelope.toJsonString());
		System.out.println("business=" + envelope.getBusiness() + "（兜底名），result=null 但键在；"
				+ "text 就是模型原文吗？" + RecordingChatModel.CHAT_REPLY.equals(envelope.getText()));
		System.out.println();
	}

	/**
	 * 帧协议演示：两条通道共用同一个 runtime，所以 start 帧 data 里的 {@code tokens} 就是
	 * 「这一轮会不会逐字下发」；结构化轮次一次性出 JSON，因此没有任何 token 帧。
	 *
	 * <p>这里打印的是 start 帧 data 与 token 帧的<b>来源</b>（本轮判定结果、流式模型的实际回调），
	 * 二者合起来足以判定帧协议。真实 SSE 帧（{@link SseEmitter}）本身可以在未初始化时从
	 * {@code ResponseBodyEmitter} 的帧缓冲里反射读到，逐帧比对见 {@code EnvelopeChannelSameShapeTest}。</p>
	 *
	 * <p>注意 {@code requestId} 是<b>每次判定</b>新生成的：下面打印的是本次判定的产物，与
	 * {@code streamToSse} 内部那次判定不是同一个 id（真实帧里的事件 id 与 start 帧 data 里的 requestId
	 * 是一致的，有测试断言）。</p>
	 */
	private static void demoStreamingFrames() {
		EnvelopeAssistantSample sample = new EnvelopeAssistantSample();

		System.out.println("=== 帧协议：结构化轮次不发 token 帧 ===");
		String structuredId = "demo-stream-order";
		String structuredMessage = "帮我查订单 A1001 的付款状态";
		EnvelopeRuntime.Round structuredRound = sample.runtime().decide(structuredId, structuredMessage);
		sample.assistant().streamToSse(structuredId, structuredMessage);

		System.out.println("结构化轮次的 start 帧 data（本轮判定的产物）：" + structuredRound.getStartData());
		System.out.println("本轮已发过的 token：" + sample.streamingModel().deliveredTokens()
				+ "（结构化轮次一次性出 JSON，所以这里是空的）");

		String chatId = "demo-stream-chat";
		String chatMessage = "今天天气怎么样";
		EnvelopeRuntime.Round chatRound = sample.runtime().decide(chatId, chatMessage);
		sample.assistant().streamToSse(chatId, chatMessage);

		System.out.println("闲聊轮次的 start 帧 data（本轮判定的产物）：" + chatRound.getStartData());
		System.out.println("闲聊轮次开始后，流式模型被调用 " + sample.streamingModel().requestCount()
				+ " 次，逐字下发的 token：" + sample.streamingModel().deliveredTokens());
		System.out.println();
	}
}
