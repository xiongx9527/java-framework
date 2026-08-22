package dev.tinyflow.langchain4j.provider;

import com.xiongx9527.fw.plus.ai.tinyflow.TinyflowException;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.tinyflow.core.llm.Llm;
import org.junit.Test;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class Langchain4jLlmTest {

	@Test
	public void shouldGenerateWithSystemTextAndImageMessages() {
		CapturingChatModel model = new CapturingChatModel(response("ok"));
		Langchain4jLlm llm = new Langchain4jLlm(model);

		String answer = llm.chat(messageInfo("system", "question", "https://example.test/a.png"),
				new Llm.ChatOptions(), null, null);

		assertEquals("ok", answer);
		assertEquals(2, model.messages.size());
		assertTrue(model.messages.get(0) instanceof SystemMessage);
		assertEquals("system", ((SystemMessage) model.messages.get(0)).text());
		assertTrue(model.messages.get(1) instanceof UserMessage);
		UserMessage userMessage = (UserMessage) model.messages.get(1);
		assertEquals(2, userMessage.contents().size());
		assertTrue(userMessage.contents().get(0) instanceof TextContent);
		assertEquals("question", ((TextContent) userMessage.contents().get(0)).text());
		assertTrue(userMessage.contents().get(1) instanceof ImageContent);
		assertEquals(URI.create("https://example.test/a.png"),
				((ImageContent) userMessage.contents().get(1)).image().url());
	}

	@Test
	public void shouldWrapModelException() {
		String sensitiveValue = "secret-value";
		IllegalStateException modelFailure = new IllegalStateException("model failed with token=" + sensitiveValue);
		Langchain4jLlm llm = new Langchain4jLlm(new ThrowingChatModel(modelFailure));

		TinyflowException failure = assertThrows(TinyflowException.class,
				() -> llm.chat(messageInfo(null, "question", null), new Llm.ChatOptions(), null, null));

		assertSame(modelFailure, failure.getCause());
		assertEquals("LangChain4j model invocation failed", failure.getMessage());
		assertFalse(failure.getMessage().contains(sensitiveValue));
	}

	@Test
	public void shouldRedactSensitiveInvalidImageUriFromMappingFailure() {
		String sensitiveValue = "secret-value";
		Langchain4jLlm llm = new Langchain4jLlm(
				new CapturingChatModel(response("unused")));

		TinyflowException failure = assertThrows(TinyflowException.class,
				() -> llm.chat(messageInfo(null, "question",
						"https://example.test/a b.png?token=" + sensitiveValue),
						new Llm.ChatOptions(), null, null));

		assertNull("mapping failure must not retain the URI-bearing cause", failure.getCause());
		assertEquals("Failed to map Tinyflow message to LangChain4j", failure.getMessage());
		assertFalse(failure.getMessage().contains(sensitiveValue));
	}

	@Test
	public void shouldRejectNullResponse() {
		Langchain4jLlm llm = new Langchain4jLlm(new CapturingChatModel(null));

		assertThrows(TinyflowException.class,
				() -> llm.chat(messageInfo(null, "question", null), new Llm.ChatOptions(), null, null));
	}

	@Test
	public void shouldRejectNullAiMessage() {
		ChatResponse response = new ChatResponse(ChatResponse.builder().aiMessage(AiMessage.from("unused"))) {
			@Override
			public AiMessage aiMessage() {
				return null;
			}
		};
		Langchain4jLlm llm = new Langchain4jLlm(new CapturingChatModel(response));

		assertThrows(TinyflowException.class,
				() -> llm.chat(messageInfo(null, "question", null), new Llm.ChatOptions(), null, null));
	}

	@Test
	public void shouldRejectEmptyAiMessageText() {
		Langchain4jLlm llm = new Langchain4jLlm(
				new CapturingChatModel(response("")));

		assertThrows(TinyflowException.class,
				() -> llm.chat(messageInfo(null, "question", null), new Llm.ChatOptions(), null, null));
	}

	@Test
	public void shouldRejectBlankAiMessageText() {
		Langchain4jLlm llm = new Langchain4jLlm(
				new CapturingChatModel(response(" \t")));

		assertThrows(TinyflowException.class,
				() -> llm.chat(messageInfo(null, "question", null), new Llm.ChatOptions(), null, null));
	}

	private static ChatResponse response(String text) {
		return ChatResponse.builder().aiMessage(AiMessage.from(text)).build();
	}

	private static Llm.MessageInfo messageInfo(String systemMessage, String message, String image) {
		Llm.MessageInfo messageInfo = new Llm.MessageInfo();
		messageInfo.setSystemMessage(systemMessage);
		messageInfo.setMessage(message);
		messageInfo.setImages(image == null ? Collections.<String>emptyList() : Collections.singletonList(image));
		return messageInfo;
	}

	private static final class CapturingChatModel implements ChatModel {
		private final ChatResponse response;
		private List<ChatMessage> messages;

		private CapturingChatModel(ChatResponse response) {
			this.response = response;
		}

		@Override
		public ChatResponse chat(ChatRequest request) {
			this.messages = new ArrayList<ChatMessage>(request.messages());
			return response;
		}
	}

	private static final class ThrowingChatModel implements ChatModel {
		private final RuntimeException failure;

		private ThrowingChatModel(RuntimeException failure) {
			this.failure = failure;
		}

		@Override
		public ChatResponse chat(ChatRequest request) {
			throw failure;
		}
	}
}
