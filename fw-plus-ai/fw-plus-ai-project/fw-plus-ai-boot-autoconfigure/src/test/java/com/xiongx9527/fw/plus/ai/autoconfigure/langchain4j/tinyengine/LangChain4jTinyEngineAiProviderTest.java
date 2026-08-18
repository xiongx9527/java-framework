package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.tinyengine;

import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.TinyEngineAiProperties;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.chat.TinyEngineChatMessage;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.chat.TinyEngineChatRequest;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.chat.TinyEngineChatResponse;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.chat.TinyEngineChatStreamChunk;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.provider.TinyEngineAiProvider;
import com.xiongx9527.fw.plus.ai.langchain4j.adapter.LangChain4jAiModel;
import com.xiongx9527.fw.plus.ai.langchain4j.adapter.LangChain4jModel;
import com.xiongx9527.fw.plus.ai.langchain4j.adapter.LangChain4jModelType;
import com.xiongx9527.fw.plus.ai.model.AiModel;
import com.xiongx9527.fw.plus.ai.model.AiModelDefinition;
import com.xiongx9527.fw.plus.ai.model.AiModelRegistry;
import com.xiongx9527.fw.plus.ai.model.AiModelType;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ResponseFormat;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class LangChain4jTinyEngineAiProviderTest {

	@Test
	public void shouldPreserveResponseFormatForChatModel() {
		OverridingChatModel chatModel = new OverridingChatModel();
		LangChain4jTinyEngineAiProvider provider = provider(aiModel("chat-default", AiModelType.CHAT,
				LangChain4jModelType.CHAT, chatModel));
		TinyEngineChatRequest request = request("chat-default", false);
		request.setResponseFormat("json_object");

		TinyEngineChatResponse response = provider.chat(request);

		assertEquals("legacy chat answer", response.getContent());
		assertEquals("JSON", chatModel.request.responseFormat().type().name());
	}

	@Test
	public void shouldInvokeChatModelThroughModernApi() {
		GenerateOnlyChatModel chatModel = new GenerateOnlyChatModel();
		LangChain4jTinyEngineAiProvider provider = provider(aiModel("chat-default", AiModelType.CHAT,
				LangChain4jModelType.CHAT, chatModel));

		TinyEngineChatResponse response = provider.chat(request("chat-default", false));

		assertEquals("legacy generate answer", response.getContent());
		assertEquals("hello", text(chatModel.messages.get(1)));
	}

	@Test
	public void shouldInvokePlainChatModel() {
		PlainChatModel chatModel = new PlainChatModel();
		LangChain4jTinyEngineAiProvider provider = provider(aiModel("chat-default", AiModelType.CHAT,
				LangChain4jModelType.CHAT, chatModel));

		TinyEngineChatResponse response = provider.chat(request("chat-default", false));

		assertEquals("plain chat answer", response.getContent());
		assertEquals("hello", text(chatModel.messages.get(1)));
	}

	@Test
	public void shouldInvokePlainStreamingChatModel() {
		PlainStreamingChatModel streamingModel = new PlainStreamingChatModel();
		LangChain4jTinyEngineAiProvider provider = provider(aiModel("stream-default", AiModelType.STREAMING_CHAT,
				LangChain4jModelType.STREAMING_CHAT, streamingModel));

		List<TinyEngineChatStreamChunk> chunks = provider.stream(request("stream-default", true));

		assertEquals("hello", text(streamingModel.messages.get(1)));
		assertEquals("plain stream answer", chunks.get(1).getContent());
		assertEquals(true, chunks.get(1).isCompleted());
	}

	@Test
	public void shouldInvokeNativeChatModelWithMappedMessagesAndParameters() {
		RecordingChatModel chatModel = new RecordingChatModel();
		LangChain4jTinyEngineAiProvider provider = provider(aiModel("chat-default", AiModelType.CHAT,
				LangChain4jModelType.CHAT, chatModel));
		TinyEngineChatRequest request = request("chat-default", false);
		request.setResponseFormat("json_object");
		request.setMaxTokens(Integer.valueOf(128));
		request.setPresencePenalty(Double.valueOf(0.5D));
		request.setFrequencyPenalty(Double.valueOf(0.2D));
		request.setEnableThinking(Boolean.TRUE);
		request.setEnableSearch(Boolean.FALSE);

		TinyEngineChatResponse response = provider.chat(request);

		assertEquals("chat answer", response.getContent());
		assertEquals("system prompt", text(chatModel.messages.get(0)));
		assertEquals("hello", text(chatModel.messages.get(1)));
		assertEquals("JSON", chatModel.request.responseFormat().type().name());
		assertEquals(Integer.valueOf(128), chatModel.request.maxOutputTokens());
		assertEquals(Double.valueOf(0.5D), chatModel.request.presencePenalty());
		assertEquals(Double.valueOf(0.2D), chatModel.request.frequencyPenalty());
		assertEquals("json_object", response.getMetadata().get("response_format"));
		assertEquals(Integer.valueOf(128), response.getMetadata().get("max_tokens"));
		assertEquals(Double.valueOf(0.5D), response.getMetadata().get("presence_penalty"));
		assertEquals(Boolean.TRUE, response.getMetadata().get("enable_thinking"));
	}

	@Test
	public void shouldInvokeNativeStreamingModelAndReturnSseConvertibleChunks() {
		RecordingNativeStreamingChatModel streamingModel = new RecordingNativeStreamingChatModel();
		LangChain4jTinyEngineAiProvider provider = provider(aiModel("stream-default", AiModelType.STREAMING_CHAT,
				LangChain4jModelType.STREAMING_CHAT, streamingModel));
		TinyEngineChatRequest request = request("stream-default", true);
		request.setResponseFormat("json_object");
		request.setMaxTokens(Integer.valueOf(128));
		request.setPresencePenalty(Double.valueOf(0.5D));
		request.setFrequencyPenalty(Double.valueOf(0.2D));

		List<TinyEngineChatStreamChunk> chunks = provider.stream(request);

		assertEquals("hello", text(streamingModel.messages.get(1)));
		assertEquals(ResponseFormat.JSON, streamingModel.request.responseFormat());
		assertEquals(Integer.valueOf(128), streamingModel.request.maxOutputTokens());
		assertEquals(Double.valueOf(0.5D), streamingModel.request.presencePenalty());
		assertEquals(Double.valueOf(0.2D), streamingModel.request.frequencyPenalty());
		assertEquals(3, chunks.size());
		assertEquals("he", chunks.get(0).getContent());
		assertEquals("llo", chunks.get(1).getContent());
		assertEquals(true, chunks.get(2).isCompleted());
		assertEquals("stream answer", chunks.get(2).getContent());
	}

	@Test
	public void shouldInvokeStreamingChatModelThroughModernApi() {
		RecordingStreamingChatModel streamingModel = new RecordingStreamingChatModel();
		LangChain4jTinyEngineAiProvider provider = provider(aiModel("stream-default", AiModelType.STREAMING_CHAT,
				LangChain4jModelType.STREAMING_CHAT, streamingModel));

		List<TinyEngineChatStreamChunk> chunks = provider.stream(request("stream-default", true));

		assertEquals("hello", text(streamingModel.messages.get(1)));
		assertEquals("stream answer", chunks.get(2).getContent());
		assertEquals(true, chunks.get(2).isCompleted());
	}

	@Test
	public void shouldPropagateNativeStreamingErrors() {
		LangChain4jTinyEngineAiProvider provider = provider(aiModel("stream-default", AiModelType.STREAMING_CHAT,
				LangChain4jModelType.STREAMING_CHAT, new ErrorStreamingChatModel()));

		IllegalStateException exception = assertThrows(IllegalStateException.class,
				() -> provider.stream(request("stream-default", true)));

		assertEquals("stream failure", exception.getMessage());
	}

	@Test
	public void shouldRejectUnsupportedToolMappingExplicitly() {
		LangChain4jTinyEngineAiProvider provider = provider(aiModel("chat-default", AiModelType.CHAT,
				LangChain4jModelType.CHAT, new RecordingChatModel()));
		TinyEngineChatRequest request = request("chat-default", false);
		request.setTools(Arrays.<java.util.Map<String, Object>>asList(new HashMap<String, Object>()));

		IllegalStateException exception = assertThrows(IllegalStateException.class, () -> provider.chat(request));

		assertEquals("TinyEngine AI provider 'langchain4j' does not support tool mapping yet", exception.getMessage());
	}

	private TinyEngineChatRequest request(String modelKey, boolean stream) {
		TinyEngineChatRequest request = new TinyEngineChatRequest();
		request.setModelKey(modelKey);
		request.setStream(Boolean.valueOf(stream));
		List<TinyEngineChatMessage> messages = new ArrayList<TinyEngineChatMessage>();
		messages.add(new TinyEngineChatMessage("system", "system prompt"));
		messages.add(new TinyEngineChatMessage("user", "hello"));
		request.setMessages(messages);
		return request;
	}

	private LangChain4jTinyEngineAiProvider provider(AiModel model) {
		TinyEngineAiProperties properties = new TinyEngineAiProperties();
		properties.getModel().setChatKey("chat-default");
		properties.getModel().setStreamingChatKey("stream-default");
		return new LangChain4jTinyEngineAiProvider(new SingleModelRegistry(model), properties);
	}

	private static String text(ChatMessage message) {
		if (message instanceof SystemMessage) {
			return ((SystemMessage) message).text();
		}
		if (message instanceof UserMessage) {
			return ((UserMessage) message).singleText();
		}
		if (message instanceof AiMessage) {
			return ((AiMessage) message).text();
		}
		throw new IllegalArgumentException("Unsupported message type: " + message.type());
	}

	private AiModel aiModel(String key, AiModelType type, LangChain4jModelType nativeType, Object nativeModel) {
		AiModelDefinition definition = new AiModelDefinition();
		definition.setKey(key);
		definition.setFramework("langchain4j");
		definition.setProvider("test");
		definition.setType(type);
		definition.setEnabled(true);
		return new LangChain4jAiModel(definition, new LangChain4jModel("test", nativeType, nativeModel));
	}

	private static final class SingleModelRegistry implements AiModelRegistry {
		private final AiModel model;
		private SingleModelRegistry(AiModel model) { this.model = model; }
		@Override public AiModel get(String key) { return model != null && model.getKey().equals(key) ? model : null; }
		@Override public AiModel getDefault() { return model; }
		@Override public String getDefaultKey() { return model == null ? null : model.getKey(); }
		@Override public boolean contains(String key) { return model != null && model.getKey().equals(key); }
		@Override public Set<String> keys() { return model == null ? Collections.<String>emptySet() : Collections.singleton(model.getKey()); }
	}

	private static final class RecordingChatModel implements ChatModel {
		private List<ChatMessage> messages = Collections.emptyList();
		private ChatRequest request;
		@Override public ChatResponse chat(ChatRequest request) {
			this.request = request;
			this.messages = request.messages();
			return ChatResponse.builder().aiMessage(AiMessage.from("chat answer")).build();
		}
	}

	private static final class RecordingStreamingChatModel implements StreamingChatModel {
		private List<ChatMessage> messages = Collections.emptyList();
		@Override public void chat(ChatRequest request,
				dev.langchain4j.model.chat.response.StreamingChatResponseHandler handler) {
			this.messages = request.messages();
			handler.onPartialResponse("he");
			handler.onPartialResponse("llo");
			handler.onCompleteResponse(ChatResponse.builder().aiMessage(AiMessage.from("stream answer")).build());
		}
	}

	private static final class RecordingNativeStreamingChatModel implements StreamingChatModel {
		private List<ChatMessage> messages = Collections.emptyList();
		private ChatRequest request;

		@Override
		public void chat(ChatRequest request, dev.langchain4j.model.chat.response.StreamingChatResponseHandler handler) {
			this.request = request;
			this.messages = request.messages();
			handler.onPartialResponse("he");
			handler.onPartialResponse("llo");
			handler.onCompleteResponse(ChatResponse.builder().aiMessage(AiMessage.from("stream answer")).build());
		}
	}

	private static final class ErrorStreamingChatModel implements StreamingChatModel {
		@Override
		public void chat(ChatRequest request, dev.langchain4j.model.chat.response.StreamingChatResponseHandler handler) {
			handler.onError(new IllegalStateException("stream failure"));
		}
	}

	private static final class GenerateOnlyChatModel implements ChatModel {
		private List<ChatMessage> messages = Collections.emptyList();

		@Override
		public ChatResponse chat(ChatRequest request) {
			this.messages = request.messages();
			return ChatResponse.builder().aiMessage(AiMessage.from("legacy generate answer")).build();
		}
	}

	private static final class OverridingChatModel extends BaseOverridingChatModel {
	}

	private static class BaseOverridingChatModel implements ChatModel {
		protected ChatRequest request;


		@Override
		public ChatResponse chat(ChatRequest request) {
			this.request = request;
			return ChatResponse.builder().aiMessage(AiMessage.from("legacy chat answer")).build();
		}
	}

	private static final class PlainChatModel implements ChatModel {
		private List<ChatMessage> messages = Collections.emptyList();

		@Override
		public ChatResponse chat(ChatRequest request) {
			this.messages = request.messages();
			return ChatResponse.builder().aiMessage(AiMessage.from("plain chat answer")).build();
		}
	}

	private static final class PlainStreamingChatModel implements StreamingChatModel {
		private List<ChatMessage> messages = Collections.emptyList();

		@Override
		public void chat(ChatRequest request, dev.langchain4j.model.chat.response.StreamingChatResponseHandler handler) {
			this.messages = request.messages();
			handler.onPartialResponse("plain ");
			handler.onCompleteResponse(ChatResponse.builder().aiMessage(AiMessage.from("plain stream answer")).build());
		}
	}
}
