package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.service.TokenStream;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

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

	/** done 事件的数据：完整回复文本；模型没有产出文本（如纯工具调用轮、response 为空）时退化为 [DONE]。 */
	@Test
	void doneDataFallsBackWhenResponseHasNoText() {
		assertThat(SimpleAssistant.doneData(null)).isEqualTo("[DONE]");
		assertThat(SimpleAssistant.doneData(Response.from(AiMessage.from("完整回复")))).isEqualTo("完整回复");
		assertThat(SimpleAssistant.doneData(Response.from(AiMessage.from("")))).isEqualTo("[DONE]");
	}

	/** 生成一个会在 start() 时依次触发 onNext/onComplete 的 TokenStream 替身 */
	private static TokenStream fakeStream(String... tokens) {
		return new TokenStream() {

			private final List<Consumer<String>> nextHandlers = new ArrayList<>();
			private Consumer<Response<AiMessage>> completeHandler;

			@Override
			public TokenStream onNext(Consumer<String> handler) {
				nextHandlers.add(handler);
				return this;
			}

			@Override
			public TokenStream onRetrieved(Consumer<List<dev.langchain4j.rag.content.Content>> handler) {
				return this;
			}

			@Override
			public TokenStream onComplete(Consumer<Response<AiMessage>> handler) {
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
					completeHandler.accept(Response.from(AiMessage.from(full.toString())));
				}
			}
		};
	}
}
