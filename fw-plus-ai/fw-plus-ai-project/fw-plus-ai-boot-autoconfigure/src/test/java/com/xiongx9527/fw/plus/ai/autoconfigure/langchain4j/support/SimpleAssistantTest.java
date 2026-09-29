package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.service.TokenStream;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

	/** 一次性文本补全：LanguageModel 直接生成文本 */
	@Test
	void shouldGenerateTextWithLanguageModel() {
		dev.langchain4j.model.language.LanguageModel languageModel =
				prompt -> dev.langchain4j.model.output.Response.from("补全文本");

		assertThat(new SimpleAssistant(languageModel).generateText("写一段简介")).isEqualTo("补全文本");
	}

	/** 流式文本补全：onToken 逐段回调，阻塞等待完成后拼接完整文本 */
	@Test
	void shouldGenerateTextWithStreamingLanguageModel() {
		List<String> streamed = new ArrayList<>();
		dev.langchain4j.model.language.StreamingLanguageModel streamingModel =
				(prompt, handler) -> {
					handler.onNext("北京当前 ");
					handler.onNext("32℃");
					handler.onComplete(dev.langchain4j.model.output.Response.from("北京当前 32℃"));
				};

		String full = new SimpleAssistant(streamingModel).generateText("北京天气", streamed::add);

		assertThat(full).isEqualTo("北京当前 32℃");
		assertThat(streamed).containsExactly("北京当前 ", "32℃");
	}

	/** 流式文本补全出错：错误向外抛出 */
	@Test
	void shouldPropagateStreamingGenerateTextError() {
		dev.langchain4j.model.language.StreamingLanguageModel failingModel =
				(prompt, handler) -> handler.onError(new IllegalStateException("模型不可用"));

		assertThatThrownBy(() -> new SimpleAssistant(failingModel).generateText("提示", null))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("流式文本补全失败");
	}

	/** 流式文本补全的 SSE：start（requestId）→ token 逐段 → done（完整文本） */
	@Test
	void shouldGenerateTextToSseWithStartTokenAndDoneEvents() {
		AtomicReference<String> startedRequestId = new AtomicReference<>();
		dev.langchain4j.model.language.StreamingLanguageModel streamingModel =
				(prompt, handler) -> {
					handler.onNext("北京当前 ");
					handler.onComplete(dev.langchain4j.model.output.Response.from("北京当前 32℃"));
				};

		SseEmitter emitter = new SimpleAssistant(streamingModel)
				.generateTextToSse("北京天气", startedRequestId::set);

		assertThat(emitter).isNotNull();
		assertThat(startedRequestId.get()).as("start 事件应回传 32 位 requestId").hasSize(32);
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

    /** 流式文本补全 SSE：onDone 回调收到完整文本（finally 保证调用） */
    @Test
    void generateTextToSseInvokesOnDoneWithFullText() {
        dev.langchain4j.model.language.StreamingLanguageModel streamingModel =
                (prompt, handler) -> {
                    handler.onNext("北京当前 ");
                    handler.onComplete(dev.langchain4j.model.output.Response.from("北京当前 32℃"));
                };
        List<String> done = new ArrayList<>();
        Consumer<String> onDone = done::add;

        new SimpleAssistant(streamingModel).generateTextToSse("北京天气", null, onDone);

        assertThat(done).containsExactly("北京当前 32℃");
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

	/** 记录每次请求、按序回放的对话模型替身 */
	private static final class RecordingChatModel implements ChatLanguageModel {

		private final List<List<ChatMessage>> requests = new ArrayList<>();
		private final List<String> replies;

		private RecordingChatModel(String... replies) {
			this.replies = Arrays.asList(replies);
		}

		@Override
		public Response<AiMessage> generate(List<ChatMessage> messages) {
			requests.add(new ArrayList<>(messages));
			int index = Math.min(requests.size() - 1, replies.size() - 1);
			return Response.from(AiMessage.from(replies.get(index)));
		}
	}
}
