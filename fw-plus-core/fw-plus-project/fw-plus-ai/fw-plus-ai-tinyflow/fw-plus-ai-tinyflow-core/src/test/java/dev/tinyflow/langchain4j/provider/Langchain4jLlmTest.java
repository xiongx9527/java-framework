package dev.tinyflow.langchain4j.provider;

import com.xiongx9527.fw.plus.ai.tinyflow.TinyflowException;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
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
		CapturingChatLanguageModel model = new CapturingChatLanguageModel(Response.from(AiMessage.from("ok")));
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
		Langchain4jLlm llm = new Langchain4jLlm(new ThrowingChatLanguageModel(modelFailure));

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
				new CapturingChatLanguageModel(Response.from(AiMessage.from("unused"))));

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
		Langchain4jLlm llm = new Langchain4jLlm(new CapturingChatLanguageModel(null));

		assertThrows(TinyflowException.class,
				() -> llm.chat(messageInfo(null, "question", null), new Llm.ChatOptions(), null, null));
	}

	@Test
	public void shouldRejectNullAiMessage() {
		Response<AiMessage> response = new Response<AiMessage>(AiMessage.from("unused")) {
			@Override
			public AiMessage content() {
				return null;
			}
		};
		Langchain4jLlm llm = new Langchain4jLlm(new CapturingChatLanguageModel(response));

		assertThrows(TinyflowException.class,
				() -> llm.chat(messageInfo(null, "question", null), new Llm.ChatOptions(), null, null));
	}

	@Test
	public void shouldRejectEmptyAiMessageText() {
		Langchain4jLlm llm = new Langchain4jLlm(
				new CapturingChatLanguageModel(Response.from(AiMessage.from(""))));

		assertThrows(TinyflowException.class,
				() -> llm.chat(messageInfo(null, "question", null), new Llm.ChatOptions(), null, null));
	}

	@Test
	public void shouldRejectBlankAiMessageText() {
		Langchain4jLlm llm = new Langchain4jLlm(
				new CapturingChatLanguageModel(Response.from(AiMessage.from(" \t"))));

		assertThrows(TinyflowException.class,
				() -> llm.chat(messageInfo(null, "question", null), new Llm.ChatOptions(), null, null));
	}

	private static Llm.MessageInfo messageInfo(String systemMessage, String message, String image) {
		Llm.MessageInfo messageInfo = new Llm.MessageInfo();
		messageInfo.setSystemMessage(systemMessage);
		messageInfo.setMessage(message);
		messageInfo.setImages(image == null ? Collections.<String>emptyList() : Collections.singletonList(image));
		return messageInfo;
	}

	private static final class CapturingChatLanguageModel implements ChatLanguageModel {
		private final Response<AiMessage> response;
		private List<ChatMessage> messages;

		private CapturingChatLanguageModel(Response<AiMessage> response) {
			this.response = response;
		}

		@Override
		public Response<AiMessage> generate(List<ChatMessage> messages) {
			this.messages = new ArrayList<ChatMessage>(messages);
			return response;
		}
	}

	private static final class ThrowingChatLanguageModel implements ChatLanguageModel {
		private final RuntimeException failure;

		private ThrowingChatLanguageModel(RuntimeException failure) {
			this.failure = failure;
		}

		@Override
		public Response<AiMessage> generate(List<ChatMessage> messages) {
			throw failure;
		}
	}
}
