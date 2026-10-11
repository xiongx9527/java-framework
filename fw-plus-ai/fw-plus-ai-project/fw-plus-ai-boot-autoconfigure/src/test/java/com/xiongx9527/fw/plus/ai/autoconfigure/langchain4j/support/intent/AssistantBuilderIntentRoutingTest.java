package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.intent;

import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.assistant.AssistantBuilder;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.assistant.SimpleAssistant;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.service.tool.ToolExecutor;
import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.service.tool.ToolProviderResult;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link AssistantBuilder#intent} 一站式意图识别：分类器注入 SimpleAssistant（classifyIntent），
 * 意图路由接线到 contentRetriever（检索路由）与 toolProvider（工具白名单裁剪）。
 */
class AssistantBuilderIntentRoutingTest {

	private static final IntentClassifier<SampleIntent> ROUTES_TO_LOGISTICS =
			context -> IntentResult.of(SampleIntent.LOGISTICS, 1.0d, IntentSource.RULE);

	private static ContentRetriever tag(String tag) {
		return query -> Collections.singletonList(Content.from(tag));
	}

	private static List<String> texts(List<Content> contents) {
		return contents.stream()
				.map(content -> content.textSegment().text())
				.collect(Collectors.toList());
	}

	private static ToolProvider twoToolSource() {
		return request -> {
			ToolProviderResult.Builder builder = ToolProviderResult.builder();
			builder.add(spec("track_order"), executor());
			builder.add(spec("create_refund"), executor());
			return builder.build();
		};
	}

	private static ToolSpecification spec(String name) {
		return ToolSpecification.builder().name(name).description(name).build();
	}

	private static ToolExecutor executor() {
		return (request, memoryId) -> "ok";
	}

	@Test
	void wiresIntentRoutingToContentRetriever() {
		AssistantBuilder builder = AssistantBuilder.retrievalOnly();
		AssistantBuilder.IntentRouting<SampleIntent> intent = builder.intent(ROUTES_TO_LOGISTICS);
		intent.rag()
				.route(SampleIntent.LOGISTICS, tag("logistics"))
				.fallback(tag("fallback"));

		SimpleAssistant assistant = intent.build();

		assertThat(texts(assistant.retrieve("查物流"))).containsExactly("logistics");
		assertThat(assistant.hasIntentClassifier()).isTrue();
	}

	@Test
	void unmatchedIntentFallsBackThroughRouting() {
		IntentClassifier<SampleIntent> chitchat =
				context -> IntentResult.of(SampleIntent.CHITCHAT, 1.0d, IntentSource.RULE);
		AssistantBuilder.IntentRouting<SampleIntent> intent = AssistantBuilder.retrievalOnly().intent(chitchat);
		intent.rag()
				.route(SampleIntent.LOGISTICS, tag("logistics"))
				.fallback(tag("fallback"));

		SimpleAssistant assistant = intent.build();

		assertThat(texts(assistant.retrieve("随便聊聊"))).containsExactly("fallback");
	}

	@Test
	void routeNoneYieldsNoContentThroughRouting() {
		IntentClassifier<SampleIntent> chitchat =
				context -> IntentResult.of(SampleIntent.CHITCHAT, 1.0d, IntentSource.RULE);
		AssistantBuilder.IntentRouting<SampleIntent> intent = AssistantBuilder.retrievalOnly().intent(chitchat);
		intent.rag()
				.routeNone(SampleIntent.CHITCHAT)
				.fallback(tag("fallback"));

		SimpleAssistant assistant = intent.build();

		assertThat(assistant.retrieve("随便聊聊")).isEmpty();
	}

	@Test
	void exposesClassifierResultViaClassifyIntent() {
		AssistantBuilder.IntentRouting<SampleIntent> intent =
				AssistantBuilder.retrievalOnly().intent(ROUTES_TO_LOGISTICS);

		SimpleAssistant assistant = intent.done().build();

		assertThat(assistant.classifyIntent("查物流").intent()).isEqualTo(SampleIntent.LOGISTICS);
		assertThat(assistant.classifyIntent("查物流").matched()).isTrue();
		assertThat(assistant.intentClassifier()).isSameAs(ROUTES_TO_LOGISTICS);
	}

	@Test
	void classifyIntentWithoutClassifierThrows() {
		SimpleAssistant assistant = new SimpleAssistant((ChatModel) null);

		assertThat(assistant.hasIntentClassifier()).isFalse();
		assertThatThrownBy(() -> assistant.classifyIntent("查物流"))
				.isInstanceOf(IllegalStateException.class);
	}

	@Test
	void nullClassifierIsRejected() {
		IntentClassifier<SampleIntent> none = null;

		assertThatThrownBy(() -> new AssistantBuilder().intent(none))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void intentTrimsToolsExposedToModel() {
		RecordingChatModel model = new RecordingChatModel("好的");

		AssistantBuilder builder = new AssistantBuilder().chatModel(model);
		AssistantBuilder.IntentRouting<SampleIntent> intent = builder.intent(ROUTES_TO_LOGISTICS);
		intent.tools()
				.source(twoToolSource())
				.allow(SampleIntent.LOGISTICS, "track_order");

		SimpleAssistant assistant = intent.build();
		assistant.chat("查物流");

		ChatRequest request = model.requests.get(model.requests.size() - 1);
		List<ToolSpecification> specs = request.toolSpecifications();

		assertThat(specs.stream().map(ToolSpecification::name).collect(Collectors.toList()))
				.containsExactly("track_order");
	}

	/** 记录工具请求、按序回放回复的对话模型替身（等价于 Module 测试的替身，适配 langchain4j 1.22） */
	private static final class RecordingChatModel implements ChatModel {

		private final List<ChatRequest> requests = new ArrayList<>();
		private final List<String> replies;
		private int index;

		private RecordingChatModel(String... replies) {
			this.replies = new ArrayList<>(java.util.Arrays.asList(replies));
		}

		@Override
		public ChatResponse doChat(ChatRequest request) {
			requests.add(request);
			String reply = replies.get(Math.min(index, replies.size() - 1));
			index++;
			return ChatResponse.builder().aiMessage(AiMessage.from(reply)).build();
		}

		@Override
		public CompletableFuture<ChatResponse> doChatAsync(ChatRequest request) {
			return CompletableFuture.completedFuture(doChat(request));
		}
	}
}
