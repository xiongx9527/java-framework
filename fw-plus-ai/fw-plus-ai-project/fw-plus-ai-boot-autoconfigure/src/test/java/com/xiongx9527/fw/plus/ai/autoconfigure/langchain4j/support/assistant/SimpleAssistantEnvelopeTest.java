package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.assistant;

import com.alibaba.fastjson2.JSONObject;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.envelope.BusinessContract;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.envelope.BusinessRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.envelope.ChatEnvelope;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.envelope.EnvelopeRuntime;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.envelope.EnvelopeSessionMemory;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.envelope.Langchain4jAssistantProperties;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 帧形态开关接管既有无 mapper 流式入口的验收测试。
 *
 * <p><b>本测试的可观测范围</b>：{@code SseEmitter} 在 {@code SimpleAssistant} 内部构造、没有公开注入点，
 * 因此<b>本测试不解析真实 SSE 帧</b>（真实帧的逐键逐值断言在样例侧 {@code EnvelopeChannelSameShapeTest}：
 * 反射读取 {@code ResponseBodyEmitter.earlySendAttempts}），只用三处可观测证据断言「帧内容的不变量」：</p>
 *
 * <ol>
 *   <li>信封对象本身：由 {@code EnvelopeRuntimeTest} / {@code EnvelopeAssistantTest} 覆盖；</li>
 *   <li>回调与 mapper：{@code onStarted} / {@code onDone} / {@code doneDataMapper}
 *       ——但它们只存在于<b>未被接管</b>的重载上，因此这条通道只用来验证「不接管」；</li>
 *   <li>对既有通道（{@link Assistant}）与模型（{@link ChatModel}）的交互：
 *       「分类是否发生」「是否存在 token 帧的来源（AiServices 流）」「模型被调用几次（是否重试）」
 *       「共享历史里写回了什么」。</li>
 * </ol>
 *
 * <p>据此，本测试对帧内容的断言只到「结构性不变量」这一层：结构化轮次没有 token 帧来源、
 * 且只调用一次模型；非结构化轮次有 token 帧来源且 done 由信封收敛；未接管的重载连分类都不发生。</p>
 */
class SimpleAssistantEnvelopeTest {

	private static final String MEMORY_ID = "s-1";

	private static final String ORDER_SCHEMA = "{"
			+ "\"type\":\"object\","
			+ "\"properties\":{\"orderId\":{\"type\":\"string\"}},"
			+ "\"required\":[\"orderId\"]}";

	private final ChatMemoryStore store = new InMemoryChatMemoryStore();

	private final ScriptedChatModel model = new ScriptedChatModel();

	private final Assistant provider = mock(Assistant.class);

	/** 底层 Assistant 签名是多模态 contents 形态：单条文本包装成 TextContent 列表参与 stub/verify */
	private static List<Content> contents(String text) {
		return Collections.singletonList(TextContent.from(text));
	}

	// -------------------------------------------------- 开关关闭：行为一字不变

	@Test
	void switchOffKeepsOriginalBehaviour() {
		when(provider.stream(MEMORY_ID, "", contents("讲个故事"))).thenReturn(fakeStream("杭州", "多云"));

		SseEmitter emitter = new SimpleAssistant(provider).streamToSse(MEMORY_ID, "讲个故事");

		assertThat(emitter).isNotNull();
		verify(provider).stream(MEMORY_ID, "", contents("讲个故事"));
		assertThat(model.requests).as("未装信封处理器时不做任何分类调用").isEmpty();
	}

	// -------------------------------------------------- 开关打开：既有无 mapper 入口被接管

	@Test
	void switchOnTakesOverNoMapperOverloadForChatRound() {
		when(provider.stream(MEMORY_ID, "", contents("随便聊聊"))).thenReturn(fakeStream("你好", "呀"));
		model.replies.addAll(Arrays.asList(classifiedAs("chat")));
		SimpleAssistant simple = simpleWith(envelope(order()));

		SseEmitter emitter = simple.streamToSse(MEMORY_ID, "随便聊聊");

		assertThat(emitter).isNotNull();
		// 非结构化轮次仍有 token 帧来源：既有通道被调用（正文逐字下发）
		verify(provider).stream(MEMORY_ID, "", contents("随便聊聊"));
		// 分类确实发生了（信封路径被走到）
		assertThat(model.requests).hasSize(1);
		// 闲聊轮次的记忆由既有通道写，信封层不重复写
		assertThat(store.getMessages(MEMORY_ID)).isEmpty();
	}

	@Test
	void switchOnTakesOverSingleArgOverload() {
		when(provider.stream(SimpleAssistant.DEFAULT_MEMORY_ID, "", contents("讲个故事")))
				.thenReturn(fakeStream("杭州", "多云"));
		model.replies.addAll(Arrays.asList(classifiedAs("chat")));

		SseEmitter emitter = simpleWith(envelope(order())).streamToSse("讲个故事");

		assertThat(emitter).isNotNull();
		verify(provider).stream(SimpleAssistant.DEFAULT_MEMORY_ID, "", contents("讲个故事"));
		assertThat(model.requests).as("走信封路径：先分类过一次").hasSize(1);
	}

	@Test
	void switchOnTakesOverStartDataOverload() {
		when(provider.stream(MEMORY_ID, "", contents("随便聊聊"))).thenReturn(fakeStream("你好"));
		model.replies.addAll(Arrays.asList(classifiedAs("chat")));

		// startData 传了值：接管后 start 帧 data 由框架决定，调用方值不再使用
		// （帧在内部构造、外部看不到，只能断言「确实走了信封路径」：分类发生过。
		// 注意必须传非 String 的 startData，否则会命中 (memoryId, context, message) 重载）
		SseEmitter emitter = simpleWith(envelope(order()))
				.streamToSse(MEMORY_ID, "随便聊聊", JSONObject.parseObject("{\"recordId\":\"record-1\"}"));

		assertThat(emitter).isNotNull();
		verify(provider).stream(MEMORY_ID, "", contents("随便聊聊"));
		assertThat(model.requests).hasSize(1);
	}

	// -------------------------------------------------- 结构化轮次：不逐字下发、不重试、写回共享历史

	@Test
	void structuredRoundStreamsNoTokensAndDoesNotRetry() {
		// 模型首次给出非法结果；若走重试，第二个回复会被消费（请求数会变成 3）
		model.replies.addAll(Arrays.asList(classifiedAs("order"), "{\"orderId\":123}", "{\"orderId\":\"A2\"}"));
		SimpleAssistant simple = simpleWith(envelope(order()));

		SseEmitter emitter = simple.streamToSse(MEMORY_ID, "帮我查订单 A1");

		assertThat(emitter).isNotNull();
		// 没有 token 帧来源：结构化轮次根本不走 AiServices 流式通道
		verify(provider, never()).stream(anyString(), any(), anyList());
		// 分类 1 次 + 生成 1 次：流式通道不重试
		assertThat(model.requests).hasSize(2);
		// 失败轮次以 ok=false 信封收场，并把失败说明写回共享历史（不把校验错误原文写进历史）
		List<ChatMessage> messages = store.getMessages(MEMORY_ID);
		assertThat(messages).hasSize(2);
		assertThat(((AiMessage) messages.get(1)).text()).isEqualTo(ChatEnvelope.DEFAULT_FAILURE_TEXT);
	}

	@Test
	void structuredRoundWritesEnvelopeSummaryIntoSharedHistory() {
		model.replies.addAll(Arrays.asList(classifiedAs("order"), "{\"orderId\":\"A1\"}"));
		SimpleAssistant simple = simpleWith(envelope(order()));

		simple.streamToSse(MEMORY_ID, "帮我查订单 A1");

		verify(provider, never()).stream(anyString(), any(), anyList());
		assertThat(model.requests).hasSize(2);

		// 流式侧产出的信封（经共享历史这条侧信道观测）与非流式入口在同一轮次下同形
		ChatEnvelope expected = ChatEnvelope.structured("order",
				JSONObject.parseObject("{\"orderId\":\"A1\"}"));
		List<ChatMessage> messages = store.getMessages(MEMORY_ID);
		assertThat(messages).hasSize(2);
		assertThat(((AiMessage) messages.get(1)).text())
				.isEqualTo(EnvelopeSessionMemory.summarize(expected));
	}

	// -------------------------------------------------- 未被接管的重载：连分类都不发生

	@Test
	void doneDataMapperOverloadIsNotTakenOver() {
		when(provider.stream(MEMORY_ID, "", contents("讲个故事"))).thenReturn(fakeStream("杭州", "多云"));
		AtomicReference<String> doneText = new AtomicReference<>();

		SseEmitter emitter = simpleWith(envelope(order()))
				.streamToSse(MEMORY_ID, "讲个故事", "record-1", text -> {
					doneText.set(text);
					return "record-1:" + text;
				});

		assertThat(emitter).isNotNull();
		assertThat(doneText.get()).as("调用方的 doneDataMapper 仍收到模型原文，而非信封").isEqualTo("杭州多云");
		assertThat(model.requests).as("未被接管：不应发生分类调用").isEmpty();
	}

	@Test
	void callbackOverloadsAreNotTakenOver() {
		when(provider.stream(MEMORY_ID, "", contents("讲个故事"))).thenReturn(fakeStream("杭州", "多云"));
		AtomicReference<String> startedRequestId = new AtomicReference<>();
		List<String> done = new ArrayList<>();
		Consumer<String> onDone = done::add;

		SseEmitter emitter = simpleWith(envelope(order()))
				.streamToSse(MEMORY_ID, "讲个故事", startedRequestId::set);
		simpleWith(envelope(order())).streamToSse(MEMORY_ID, "讲个故事", null, onDone);

		assertThat(emitter).isNotNull();
		assertThat(startedRequestId.get()).as("start 事件仍回传 32 位 requestId").hasSize(32);
		assertThat(done).containsExactly("杭州多云");
		assertThat(model.requests).as("未被接管：不应发生分类调用").isEmpty();
	}

	@Test
	void contextOverloadIsNotTakenOver() {
		// 自带 context（系统提示定制）的入口不在接管白名单内：调用方的 context 原样传给既有通道
		when(provider.stream(MEMORY_ID, "你是一个诗人", contents("讲个故事"))).thenReturn(fakeStream("杭州", "多云"));

		SseEmitter emitter = simpleWith(envelope(order())).streamToSse(MEMORY_ID, "你是一个诗人", "讲个故事");

		assertThat(emitter).isNotNull();
		verify(provider).stream(MEMORY_ID, "你是一个诗人", contents("讲个故事"));
		assertThat(model.requests).as("未被接管：不应发生分类调用").isEmpty();
	}

	@Test
	void streamToSseWithFilterIsNotTakenOver() {
		StreamingChatModel streamingModel = new FakeStreamingChatModel("杭州", "多云");
		SimpleAssistant simple = new SimpleAssistant(provider, null, streamingModel, null, null, null)
				.filterableRetriever((message, condition) ->
						Collections.singletonList(dev.langchain4j.rag.content.Content.from("参考资料")));
		AtomicReference<String> doneText = new AtomicReference<>();

		SseEmitter emitter = simple.envelopeRuntime(envelope(order()))
				.streamToSseWithFilter(MEMORY_ID, "{\"city\":\"杭州\"}", "讲个故事", null, text -> {
					doneText.set(text);
					return text;
				}, null, null);

		assertThat(emitter).isNotNull();
		assertThat(doneText.get()).isEqualTo("杭州多云");
		assertThat(model.requests).as("未被接管：不应发生分类调用").isEmpty();
	}

	@Test
	void generateTextToSseIsNotTakenOver() {
		dev.langchain4j.model.language.StreamingLanguageModel streamingLanguageModel =
				(prompt, handler) -> {
					handler.onNext("北京当前 ");
					handler.onComplete(Response.from("北京当前 32℃"));
				};
		List<String> done = new ArrayList<>();
		Consumer<String> onDone = done::add;

		SseEmitter emitter = new SimpleAssistant(streamingLanguageModel)
				.envelopeRuntime(envelope(order()))
				.generateTextToSse("北京天气", null, onDone);

		assertThat(emitter).isNotNull();
		assertThat(done).containsExactly("北京当前 32℃");
		assertThat(model.requests).as("未被接管：不应发生分类调用").isEmpty();
	}

	// -------------------------------------------------- 装配工具

	private SimpleAssistant simpleWith(EnvelopeRuntime runtime) {
		return new SimpleAssistant(provider).envelopeRuntime(runtime);
	}

	private EnvelopeRuntime envelope(BusinessContract... contracts) {
		BusinessRegistry registry = new BusinessRegistry();
		for (BusinessContract contract : contracts) {
			registry.register(contract);
		}
		return new EnvelopeRuntime(model, store, registry, new Langchain4jAssistantProperties());
	}

	/** 命中即可产出结构化结果的业务契约 */
	private static BusinessContract order() {
		return BusinessContract.builder("order").schema(ORDER_SCHEMA).structured(true).build();
	}

	private static String classifiedAs(String business) {
		return "{\"business\":\"" + business + "\"}";
	}

	/** 生成一个会在 start() 时依次触发增量/完成回调的 TokenStream 替身 */
	private static TokenStream fakeStream(String... tokens) {
		return new TokenStream() {

			private final List<Consumer<String>> nextHandlers = new ArrayList<>();
			private Consumer<ChatResponse> completeHandler;

			@Override
			public TokenStream onPartialResponse(Consumer<String> handler) {
				nextHandlers.add(handler);
				return this;
			}

			@Override
			public TokenStream onPartialResponseWithContext(
					java.util.function.BiConsumer<dev.langchain4j.model.chat.response.PartialResponse,
							dev.langchain4j.model.chat.response.PartialResponseContext> handler) {
				return onPartialResponse(text -> handler.accept(
						new dev.langchain4j.model.chat.response.PartialResponse(text), null));
			}

			@Override
			public TokenStream onPartialThinkingWithContext(
					java.util.function.BiConsumer<dev.langchain4j.model.chat.response.PartialThinking,
							dev.langchain4j.model.chat.response.PartialThinkingContext> handler) {
				return this;
			}

			@Override
			public TokenStream beforeToolExecution(
					Consumer<dev.langchain4j.service.tool.BeforeToolExecution> handler) {
				return this;
			}

			@Override
			public TokenStream onRetrieved(Consumer<List<dev.langchain4j.rag.content.Content>> handler) {
				return this;
			}

			@Override
			public TokenStream onToolExecuted(Consumer<dev.langchain4j.service.tool.ToolExecution> handler) {
				return this;
			}

			@Override
			public TokenStream onCompleteResponse(Consumer<ChatResponse> handler) {
				completeHandler = handler;
				return this;
			}

			@Override
			public TokenStream onError(Consumer<Throwable> handler) {
				return this;
			}

			@Override
			public TokenStream ignoreErrors() {
				return this;
			}

			@Override
			public void start() {
				for (Consumer<String> handler : nextHandlers) {
					handler.accept(tokens[0]);
				}
				if (completeHandler != null) {
					StringBuilder full = new StringBuilder();
					for (String token : tokens) {
						full.append(token);
					}
					completeHandler.accept(ChatResponse.builder()
							.aiMessage(AiMessage.from(full.toString()))
							.build());
				}
			}
		};
	}

	/** 记录每次请求、按序回放的对话模型替身（脚本用尽后返回空串，便于用请求数识别多余调用） */
	private static final class ScriptedChatModel implements ChatModel {

		private final List<List<ChatMessage>> requests = new ArrayList<>();
		private final Deque<String> replies = new ArrayDeque<>();

		@Override
		public ChatResponse doChat(ChatRequest request) {
			requests.add(new ArrayList<>(request.messages()));
			return ChatResponse.builder()
					.aiMessage(AiMessage.from(replies.isEmpty() ? "" : replies.poll()))
					.build();
		}
	}

	/** 直连流式模型替身：一次 chat 内逐段回调后完成（供 streamToSseWithFilter 用） */
	private static final class FakeStreamingChatModel implements StreamingChatModel {

		private final String[] tokens;

		private FakeStreamingChatModel(String... tokens) {
			this.tokens = tokens;
		}

		@Override
		public void chat(ChatRequest request, StreamingChatResponseHandler handler) {
			StringBuilder full = new StringBuilder();
			for (String token : tokens) {
				handler.onPartialResponse(token);
				full.append(token);
			}
			handler.onCompleteResponse(ChatResponse.builder()
					.aiMessage(AiMessage.from(full.toString()))
					.build());
		}
	}
}
