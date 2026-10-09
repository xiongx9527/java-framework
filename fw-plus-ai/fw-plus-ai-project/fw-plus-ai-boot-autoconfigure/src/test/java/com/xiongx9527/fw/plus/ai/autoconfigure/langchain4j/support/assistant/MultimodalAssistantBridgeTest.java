package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.assistant;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.rag.AugmentationRequest;
import dev.langchain4j.rag.AugmentationResult;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.service.TokenStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 多模态桥接端到端：纯文本轮原样转发内层（String/Map context 归一化）、
 * 多模态轮自拼消息直调模型 + 记忆写文本占位、非法参数拒绝、流式与 RAG。
 *
 * <p>memory 用有状态假实现（add 落进同一列表、messages 返回该列表），
 * 断言与真实 {@code MessageWindowChatMemory} 的「先入记忆再取历史」语义一致。</p>
 */
class MultimodalAssistantBridgeTest {

	private final TextAssistant inner = mock(TextAssistant.class);
	private final ChatMemory memory = mock(ChatMemory.class);
	private final ChatMemoryProvider memoryProvider = mock(ChatMemoryProvider.class);
	private final ChatModel chatModel = mock(ChatModel.class);
	private final StreamingChatModel streamingModel = mock(StreamingChatModel.class);
	private final RetrievalAugmentor retrievalAugmentor = mock(RetrievalAugmentor.class);

	/** 有状态记忆：add 追加、messages 返回同一列表（模拟真实记忆的读写闭环） */
	private final List<ChatMessage> stored = new ArrayList<>();

	@BeforeEach
	void setUp() {
		doAnswer(invocation -> {
			stored.add(invocation.getArgument(0));
			return null;
		}).when(memory).add(any(ChatMessage.class));
		when(memory.messages()).thenReturn(stored);
		when(memoryProvider.get(any())).thenReturn(memory);
	}

	// -------------------------------------------------- 纯文本轮：转发内层

	@Test
	void allTextForwardsVerbatimWithStringContext() {
		when(inner.chat("m", "背景", "问题")).thenReturn("答");

		assertThat(bridge().chat("m", "背景", text("问题"))).isEqualTo("答");

		verify(inner).chat("m", "背景", "问题");
	}

	@Test
	void allTextConvertsMapContextToJson() {
		Map<String, Object> context = new LinkedHashMap<>();
		context.put("reportId", "R-1");

		bridge().chat("m", context, text("问题"));

		verify(inner).chat("m", "{\"reportId\":\"R-1\"}", "问题");
	}

	@Test
	void nullContextBecomesEmptyStringAndStreamForwardsToo() {
		TokenStream expected = mock(TokenStream.class);
		when(inner.chat("m", "", "问题")).thenReturn("答");
		when(inner.stream("m", "", "问题")).thenReturn(expected);

		assertThat(bridge().chat("m", null, text("问题"))).isEqualTo("答");
		assertThat(bridge().stream("m", null, text("问题"))).isSameAs(expected);

		verify(inner).chat("m", "", "问题");
		verify(inner).stream("m", "", "问题");
	}

	// -------------------------------------------------- 参数拒绝

	@Test
	void invalidContextTypeRejected() {
		MultimodalAssistantBridge bridge = bridge();

		assertThatThrownBy(() -> bridge.chat("m", 42, text("问题")))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("String 或 Map");
		assertThatThrownBy(() -> bridge.stream("m", 42, text("问题")))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("String 或 Map");
	}

	@Test
	void nullOrEmptyContentsRejected() {
		MultimodalAssistantBridge bridge = bridge();

		assertThatThrownBy(() -> bridge.chat("m", null, null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("不能为空");
		assertThatThrownBy(() -> bridge.stream("m", null, Collections.<Content>emptyList()))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("不能为空");
	}

	// -------------------------------------------------- 多模态轮：直调模型 + 记忆占位

	@SuppressWarnings("unchecked")
	@Test
	void mediaRoundBypassesInnerAndWritesPlaceholderToMemory() {
		when(chatModel.chat(any(ChatRequest.class))).thenReturn(
				ChatResponse.builder().aiMessage(AiMessage.from("好看")).build());
		Map<String, Object> context = new LinkedHashMap<>();
		context.put("a", 1);

		String reply = bridge().chat("m", context, media());

		assertThat(reply).isEqualTo("好看");
		verifyNoInteractions(inner);

		ArgumentCaptor<ChatRequest> requestCaptor = ArgumentCaptor.forClass(ChatRequest.class);
		verify(chatModel).chat(requestCaptor.capture());
		List<ChatMessage> sent = requestCaptor.getValue().messages();
		assertThat(sent).hasSize(1);
		List<Content> sentContents = ((UserMessage) sent.get(0)).contents();
		assertThat(sentContents).hasSize(3);
		assertThat(((TextContent) sentContents.get(0)).text()).isEqualTo("{\"a\":1}");
		assertThat(((TextContent) sentContents.get(1)).text()).isEqualTo("看这张图");
		assertThat(sentContents.get(2)).isInstanceOf(ImageContent.class);

		// 记忆：占位在前、完整回复在后——原文（含图片）不入记忆
		assertThat(stored).hasSize(2);
		assertThat(((TextContent) ((UserMessage) stored.get(0)).contents().get(0)).text())
				.isEqualTo("看这张图\n[图片]");
		assertThat(stored.get(1)).isEqualTo(AiMessage.from("好看"));
	}

	@SuppressWarnings("unchecked")
	@Test
	void mediaRoundAddsSystemMessageToMemoryAndMessages() {
		when(chatModel.chat(any(ChatRequest.class))).thenReturn(
				ChatResponse.builder().aiMessage(AiMessage.from("答")).build());
		Function<Object, String> systemMessageProvider = memoryId -> "系统人设";

		new MultimodalAssistantBridge(inner, chatModel, streamingModel, memoryProvider,
				systemMessageProvider, null).chat("m", null, media());

		ArgumentCaptor<ChatRequest> requestCaptor = ArgumentCaptor.forClass(ChatRequest.class);
		verify(chatModel).chat(requestCaptor.capture());
		List<ChatMessage> sent = requestCaptor.getValue().messages();
		assertThat(sent).hasSize(2);
		assertThat(sent.get(0)).isEqualTo(SystemMessage.from("系统人设"));
		assertThat(sent.get(1)).isInstanceOf(UserMessage.class);
		// 记忆顺序：系统提示 → 占位 → 回复
		assertThat(stored).hasSize(3);
		assertThat(stored.get(0)).isEqualTo(SystemMessage.from("系统人设"));
	}

	@Test
	void mediaRoundWithoutChatModelThrows() {
		MultimodalAssistantBridge bridge = new MultimodalAssistantBridge(inner, null, streamingModel,
				memoryProvider, null, null);

		assertThatThrownBy(() -> bridge.chat("m", null, media()))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("chatModel");
	}

	@Test
	void mediaStreamDeliversTokensAndCompletesMemory() {
		doAnswer(invocation -> {
			StreamingChatResponseHandler handler = invocation.getArgument(1);
			handler.onPartialResponse("你");
			handler.onPartialResponse("好");
			handler.onCompleteResponse(ChatResponse.builder().aiMessage(AiMessage.from("你好")).build());
			return null;
		}).when(streamingModel).chat(any(ChatRequest.class), any(StreamingChatResponseHandler.class));
		List<String> tokens = new ArrayList<>();
		AtomicReference<String> done = new AtomicReference<>();

		bridge().stream("m", null, media())
				.onPartialResponse(tokens::add)
				.onCompleteResponse(response -> done.set(response.aiMessage().text()))
				.start();

		assertThat(tokens).containsExactly("你", "好");
		assertThat(done.get()).isEqualTo("你好");
		assertThat(stored).hasSize(2);
		assertThat(((TextContent) ((UserMessage) stored.get(0)).contents().get(0)).text())
				.isEqualTo("看这张图\n[图片]");
		assertThat(stored.get(1)).isEqualTo(AiMessage.from("你好"));
	}

	@SuppressWarnings("unchecked")
	@Test
	void mediaRoundWithRagInjectsRetrievedTextAsReference() {
		when(chatModel.chat(any(ChatRequest.class))).thenReturn(
				ChatResponse.builder().aiMessage(AiMessage.from("答")).build());
		when(retrievalAugmentor.augment(any(AugmentationRequest.class))).thenReturn(
				new AugmentationResult(UserMessage.from("看这张图"),
						Collections.singletonList(dev.langchain4j.rag.content.Content.from("检索资料段"))));

		new MultimodalAssistantBridge(inner, chatModel, streamingModel, memoryProvider,
				null, retrievalAugmentor).chat("m", null, media());

		ArgumentCaptor<ChatRequest> requestCaptor = ArgumentCaptor.forClass(ChatRequest.class);
		verify(chatModel).chat(requestCaptor.capture());
		List<Content> sentContents = ((UserMessage) requestCaptor.getValue().messages().get(0)).contents();
		assertThat(sentContents).hasSize(3);
		assertThat(((TextContent) sentContents.get(0)).text()).isEqualTo("检索资料段");
		assertThat(((TextContent) sentContents.get(1)).text()).isEqualTo("看这张图");
		assertThat(sentContents.get(2)).isInstanceOf(ImageContent.class);
	}

	// -------------------------------------------------- 工具方法

	private MultimodalAssistantBridge bridge() {
		return new MultimodalAssistantBridge(inner, chatModel, streamingModel, memoryProvider, null, null);
	}

	private static List<Content> text(String line) {
		return Collections.singletonList(TextContent.from(line));
	}

	/** 多模态样本：一段文本 + 一张图片（query 文本「看这张图」兼作 RAG 入参） */
	private static List<Content> media() {
		return Arrays.asList(
				TextContent.from("看这张图"),
				ImageContent.from(URI.create("http://example.com/img.png")));
	}
}
