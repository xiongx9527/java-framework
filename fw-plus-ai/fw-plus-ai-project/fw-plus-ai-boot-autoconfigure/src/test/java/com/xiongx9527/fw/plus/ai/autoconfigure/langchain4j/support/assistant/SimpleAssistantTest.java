package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.assistant;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.PartialThinking;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.service.TokenStream;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;

/**
 * Assistant 易用封装：context 默认空串、memoryId 默认 default，
 * SSE 推流的 start/token/done 事件顺序与数据。
 */
class SimpleAssistantTest {

	private final Assistant assistant = mock(Assistant.class);
	private final SimpleAssistant simple = new SimpleAssistant(assistant);

	@Test
	void chatWithSingleArgUsesDefaultMemoryIdAndEmptyContext() {
		org.mockito.Mockito.when(assistant.chat("default", "", "你好")).thenReturn("回复");

		assertThat(simple.chat("你好")).isEqualTo("回复");
		verify(assistant).chat("default", "", "你好");
	}

	@Test
	void chatWithMemoryIdUsesEmptyContext() {
		org.mockito.Mockito.when(assistant.chat("s-1", "", "你好")).thenReturn("回复");

		assertThat(simple.chat("s-1", "你好")).isEqualTo("回复");
		verify(assistant).chat("s-1", "", "你好");
	}

	@Test
	void streamDelegatesWithEmptyContext() {
		org.mockito.Mockito.when(assistant.stream("s-1", "", "讲个故事"))
				.thenReturn(mock(TokenStream.class));

		simple.stream("s-1", "讲个故事");

		verify(assistant).stream("s-1", "", "讲个故事");
	}

	/**
	 * SSE 推流：先发 start 事件回传 requestId（32 位），再逐段 token；
	 * 底层 assistant.stream 收到的 memoryId/context 已按缺省值填好。
	 */
	@Test
	void streamToSseDelegatesToAssistantStream() {
		AtomicReference<String> startedRequestId = new AtomicReference<>();
		org.mockito.Mockito.when(assistant.stream("s-1", "", "讲个故事"))
				.thenReturn(fakeStream("杭州", "多云"));

		SseEmitter emitter = simple.streamToSse("s-1", "讲个故事", startedRequestId::set);

		assertThat(emitter).isNotNull();
		assertThat(startedRequestId.get()).as("start 事件应回传 32 位 requestId").hasSize(32);
		verify(assistant).stream("s-1", "", "讲个故事");
	}

    @Test
    void shouldGenerateOnceWithSingleUserMessage() {
        RecordingChatModel model = new RecordingChatModel("杭州今天多云。");

        String reply = new SimpleAssistant(model).generate("杭州天气怎么样");

        assertThat(reply).isEqualTo("杭州今天多云。");
        assertThat(model.requests).hasSize(1);
        assertThat(model.requests.get(0)).containsExactly(UserMessage.from("杭州天气怎么样"));
    }

    @Test
    void shouldGenerateOnceWithSystemPrompt() {
        RecordingChatModel model = new RecordingChatModel("好的。");

        new SimpleAssistant(model).generate("你是助手", "你好");

        assertThat(model.requests.get(0)).containsExactly(
                SystemMessage.from("你是助手"), UserMessage.from("你好"));
    }

    @Test
    void shouldGenerateOnceWithCustomMessages() {
        RecordingChatModel model = new RecordingChatModel("好的。");

        new SimpleAssistant(model).generate(Arrays.<ChatMessage>asList(
                SystemMessage.from("你是助手"), UserMessage.from("上一句"), AiMessage.from("上一答"),
                UserMessage.from("这一句")));

        assertThat(model.requests.get(0)).hasSize(4);
    }

    /** 未配置 assistant 时，chat 系方法直接报错（直连请走 generate 系列） */
    @Test
    void chatFailsFastWithoutAssistant() {
        RecordingChatModel model = new RecordingChatModel("直连回复");

        assertThatThrownBy(() -> new SimpleAssistant(model).chat("你好"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("未配置 Assistant");
    }

    /** SSE 推流：start/done 事件 data 由调用方提供（doneDataMapper 收到完整回复文本） */
    @Test
    void streamToSseUsesCallerProvidedEventData() {
        AtomicReference<String> doneText = new AtomicReference<>();
        org.mockito.Mockito.when(assistant.stream("s-1", "", "讲个故事"))
                .thenReturn(fakeStream("杭州", "多云"));

        SseEmitter emitter = simple.streamToSse("s-1", "讲个故事", "record-1",
                text -> {
                    doneText.set(text);
                    return "record-1:" + text;
                });

        assertThat(emitter).isNotNull();
        assertThat(doneText.get()).as("doneDataMapper 应收到完整回复文本").isEqualTo("杭州多云");
    }

    /** SSE 推流：onDone 回调收到完整回复文本（finally 保证调用），可用于保存聊天记录 */
    @Test
    void streamToSseInvokesOnDoneWithFullText() {
        List<String> done = new ArrayList<>();
        Consumer<String> onDone = done::add;
        org.mockito.Mockito.when(assistant.stream("s-1", "", "讲个故事"))
                .thenReturn(fakeStream("杭州", "多云"));

        SseEmitter emitter = simple.streamToSse("s-1", "讲个故事", null, onDone);

        assertThat(emitter).isNotNull();
        assertThat(done).containsExactly("杭州多云");
    }

	/** done 事件的数据：完整回复文本；模型没有产出文本（如纯工具调用轮、response 为空）时退化为 [DONE]。 */
	@Test
	void doneDataFallsBackWhenResponseHasNoText() {
		assertThat(SimpleAssistant.doneData(null)).isEqualTo("[DONE]");
		assertThat(SimpleAssistant.doneData(AiMessage.from("完整回复"))).isEqualTo("完整回复");
		assertThat(SimpleAssistant.doneData(AiMessage.from(""))).isEqualTo("[DONE]");
	}

	@Test
	void streamToSseFinishesThinkingBeforeDoneWhenThereIsNoAnswerToken() throws Exception {
		org.mockito.Mockito.when(assistant.stream("s-1", "", "问题"))
				.thenReturn(fakeThinkingStream("思考内容"));

		SseEmitter emitter = simple.streamToSse("s-1", "问题");

		assertThat(eventNames(emitter)).containsExactly(
				SimpleAssistant.EVENT_START, SimpleAssistant.EVENT_THINKING,
				SimpleAssistant.EVENT_THINKING_DONE, SimpleAssistant.EVENT_DONE);
	}

	@Test
	void streamToSseFinishesThinkingBeforeToolCall() throws Exception {
		dev.langchain4j.agent.tool.ToolExecutionRequest request =
				dev.langchain4j.agent.tool.ToolExecutionRequest.builder()
						.name("search").arguments("{}").build();
		org.mockito.Mockito.when(assistant.stream("s-1", "", "问题"))
				.thenReturn(fakeThinkingStream("思考内容", request));

		SseEmitter emitter = simple.streamToSse("s-1", "问题");

		assertThat(eventNames(emitter)).containsExactly(
				SimpleAssistant.EVENT_START, SimpleAssistant.EVENT_THINKING,
				SimpleAssistant.EVENT_THINKING_DONE, SimpleAssistant.EVENT_TOOL_CALL,
				SimpleAssistant.EVENT_DONE);
	}

	@Test
	void directStreamingChatForwardsThinkingAndFinishesItBeforeAnswer() throws Exception {
		StreamingChatModel model = mock(StreamingChatModel.class);
		doAnswer(invocation -> {
			StreamingChatResponseHandler handler = invocation.getArgument(1);
			handler.onPartialThinking(new PartialThinking("思考内容"));
			handler.onPartialResponse("回答");
			handler.onCompleteResponse(ChatResponse.builder().aiMessage(AiMessage.from("回答")).build());
			return null;
		}).when(model).chat(anyString(), any(StreamingChatResponseHandler.class));

		SimpleAssistant direct = new SimpleAssistant(model)
				.filterableRetriever((message, condition) -> Collections.emptyList());
		SseEmitter emitter = direct.streamToSseWithFilter("s-1", null, "问题");

		assertThat(eventNames(emitter)).containsExactly(
				SimpleAssistant.EVENT_START, SimpleAssistant.EVENT_THINKING,
				SimpleAssistant.EVENT_THINKING_DONE, SimpleAssistant.EVENT_TOKEN, SimpleAssistant.EVENT_DONE);
	}

	@Test
	void directStreamingChatWithoutThinkingDoesNotEmitThinkingEvents() throws Exception {
		StreamingChatModel model = mock(StreamingChatModel.class);
		doAnswer(invocation -> {
			StreamingChatResponseHandler handler = invocation.getArgument(1);
			handler.onPartialResponse("回答");
			handler.onCompleteResponse(ChatResponse.builder().aiMessage(AiMessage.from("回答")).build());
			return null;
		}).when(model).chat(anyString(), any(StreamingChatResponseHandler.class));

		SimpleAssistant direct = new SimpleAssistant(model)
				.filterableRetriever((message, condition) -> Collections.emptyList());
		SseEmitter emitter = direct.streamToSseWithFilter("s-1", null, "问题");

		assertThat(eventNames(emitter)).containsExactly(
				SimpleAssistant.EVENT_START, SimpleAssistant.EVENT_TOKEN, SimpleAssistant.EVENT_DONE);
	}

	private static List<String> eventNames(SseEmitter emitter) throws Exception {
		List<String> events = new ArrayList<>();
		Class<?> handlerType = Class.forName(
				"org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter$Handler");
		Object handler = Proxy.newProxyInstance(handlerType.getClassLoader(), new Class<?>[] { handlerType },
				(proxy, method, args) -> {
					if ("send".equals(method.getName()) && args[0] instanceof Set) {
						StringBuilder event = new StringBuilder();
						for (Object item : (Set<?>) args[0]) {
							Method getData = item.getClass().getMethod("getData");
							event.append(getData.invoke(item));
						}
						events.add(event.toString());
					}
					return null;
				});
		Method initialize = org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter.class
				.getDeclaredMethod("initialize", handlerType);
		initialize.setAccessible(true);
		initialize.invoke(emitter, handler);

		List<String> names = new ArrayList<>();
		for (String event : events) {
			int start = 0;
			while ((start = event.indexOf("event:", start)) >= 0) {
				int end = event.indexOf('\n', start);
				names.add(event.substring(start + "event:".length(), end));
				start = end + 1;
			}
		}
		return names;
	}

	/** 生成一个会在 start() 时依次触发 onNext/onComplete 的 TokenStream 替身 */
	private static TokenStream fakeStream(String... tokens) {
		return new FakeTokenStream(null, null, tokens);
	}

	private static TokenStream fakeThinkingStream(String thinking,
			dev.langchain4j.agent.tool.ToolExecutionRequest toolRequest, String... tokens) {
		return new FakeTokenStream(thinking, toolRequest, tokens);
	}

	private static TokenStream fakeThinkingStream(String thinking, String... tokens) {
		return fakeThinkingStream(thinking, null, tokens);
	}

	private static final class FakeTokenStream implements TokenStream {

		private final String thinking;
		private final dev.langchain4j.agent.tool.ToolExecutionRequest toolRequest;
		private final String[] tokens;
		private final List<Consumer<String>> nextHandlers = new ArrayList<>();
		private java.util.function.BiConsumer<PartialThinking,
				dev.langchain4j.model.chat.response.PartialThinkingContext> thinkingHandler;
		private Consumer<dev.langchain4j.service.tool.BeforeToolExecution> beforeToolHandler;
		private Consumer<dev.langchain4j.model.chat.response.ChatResponse> completeHandler;

		private FakeTokenStream(String thinking, dev.langchain4j.agent.tool.ToolExecutionRequest toolRequest,
				String... tokens) {
			this.thinking = thinking;
			this.toolRequest = toolRequest;
			this.tokens = tokens;
		}

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
				java.util.function.BiConsumer<PartialThinking,
						dev.langchain4j.model.chat.response.PartialThinkingContext> handler) {
			thinkingHandler = handler;
			return this;
		}

		@Override
		public TokenStream beforeToolExecution(
				Consumer<dev.langchain4j.service.tool.BeforeToolExecution> handler) {
			beforeToolHandler = handler;
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
		public TokenStream onCompleteResponse(
				Consumer<dev.langchain4j.model.chat.response.ChatResponse> handler) {
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
			if (thinkingHandler != null && thinking != null) {
				thinkingHandler.accept(new PartialThinking(thinking), null);
			}
			if (beforeToolHandler != null && toolRequest != null) {
				beforeToolHandler.accept(dev.langchain4j.service.tool.BeforeToolExecution.builder()
						.request(toolRequest)
						.invocationContext(mock(dev.langchain4j.invocation.InvocationContext.class))
						.build());
			}
			for (Consumer<String> handler : nextHandlers) {
				for (String token : tokens) {
					handler.accept(token);
				}
			}
			if (completeHandler != null) {
				StringBuilder full = new StringBuilder();
				for (String token : tokens) {
					full.append(token);
				}
				completeHandler.accept(dev.langchain4j.model.chat.response.ChatResponse.builder()
						.aiMessage(AiMessage.from(full.toString()))
						.build());
			}
		}
	}

	/** 记录每次请求、按序回放的对话模型替身 */
	private static final class RecordingChatModel implements ChatModel {

		private final List<List<ChatMessage>> requests = new ArrayList<>();
		private final List<String> replies;

		private RecordingChatModel(String... replies) {
			this.replies = Arrays.asList(replies);
		}

		@Override
		public dev.langchain4j.model.chat.response.ChatResponse doChat(
				dev.langchain4j.model.chat.request.ChatRequest request) {
			requests.add(new ArrayList<>(request.messages()));
			int index = Math.min(requests.size() - 1, replies.size() - 1);
			return dev.langchain4j.model.chat.response.ChatResponse.builder()
					.aiMessage(AiMessage.from(replies.get(index)))
					.build();
		}
	}
}
