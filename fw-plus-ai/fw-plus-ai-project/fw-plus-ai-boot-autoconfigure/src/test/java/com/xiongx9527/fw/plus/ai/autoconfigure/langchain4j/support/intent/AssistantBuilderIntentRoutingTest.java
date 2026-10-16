package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.intent;

import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.assistant.AssistantBuilder;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.assistant.SimpleAssistant;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.service.tool.ToolExecutor;
import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.service.tool.ToolProviderResult;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
		SimpleAssistant assistant = new SimpleAssistant((ChatLanguageModel) null);

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
	@SuppressWarnings("unchecked")
	void intentTrimsToolsExposedToModel() {
		ChatLanguageModel model = mock(ChatLanguageModel.class);
		when(model.generate(anyList(), anyList()))
				.thenReturn(Response.from(AiMessage.from("好的")));

		AssistantBuilder builder = new AssistantBuilder().chatModel(model);
		AssistantBuilder.IntentRouting<SampleIntent> intent = builder.intent(ROUTES_TO_LOGISTICS);
		intent.tools()
				.source(twoToolSource())
				.allow(SampleIntent.LOGISTICS, "track_order");

		SimpleAssistant assistant = intent.build();
		assistant.chat("查物流");

		ArgumentCaptor<List> specsCaptor = ArgumentCaptor.forClass(List.class);
		verify(model).generate(anyList(), specsCaptor.capture());
		List<ToolSpecification> specs = (List<ToolSpecification>) specsCaptor.getValue();

		assertThat(specs.stream().map(ToolSpecification::name).collect(Collectors.toList()))
				.containsExactly("track_order");
	}
}
