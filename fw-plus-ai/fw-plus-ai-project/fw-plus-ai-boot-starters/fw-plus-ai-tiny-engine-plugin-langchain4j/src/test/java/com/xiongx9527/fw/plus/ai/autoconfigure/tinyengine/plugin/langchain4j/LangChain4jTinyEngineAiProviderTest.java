package com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.plugin.langchain4j;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.TinyEngineAiProperties;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.chat.TinyEngineChatMessage;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.chat.TinyEngineChatRequest;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.chat.TinyEngineChatResponse;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.chat.TinyEngineChatStreamChunk;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.official.TinyEngineUpstreamException;
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
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class LangChain4jTinyEngineAiProviderTest {

	private HttpServer upstream;

	@AfterEach
	public void stopUpstream() {
		if (upstream != null) {
			upstream.stop(0);
		}
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
		assertEquals("system prompt", chatModel.messages.get(0).text());
		assertEquals("hello", chatModel.messages.get(1).text());
		assertEquals("JSON", chatModel.request.responseFormat().type().name());
		assertEquals("json_object", response.getMetadata().get("response_format"));
		assertEquals(Integer.valueOf(128), response.getMetadata().get("max_tokens"));
		assertEquals(Double.valueOf(0.5D), response.getMetadata().get("presence_penalty"));
		assertEquals(Boolean.TRUE, response.getMetadata().get("enable_thinking"));
	}

	@Test
	public void shouldInvokeNativeStreamingModelAndReturnSseConvertibleChunks() {
		RecordingStreamingChatModel streamingModel = new RecordingStreamingChatModel();
		LangChain4jTinyEngineAiProvider provider = provider(aiModel("stream-default", AiModelType.STREAMING_CHAT,
				LangChain4jModelType.STREAMING_CHAT, streamingModel));
		TinyEngineChatRequest request = request("stream-default", true);

		List<TinyEngineChatStreamChunk> chunks = provider.stream(request);

		assertEquals("hello", streamingModel.messages.get(1).text());
		assertEquals(3, chunks.size());
		assertEquals("he", chunks.get(0).getContent());
		assertEquals("llo", chunks.get(1).getContent());
		assertEquals(true, chunks.get(2).isCompleted());
		assertEquals("stream answer", chunks.get(2).getContent());
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

	@Test
	public void shouldInvokeOpenAiCompatibleEndpointWithResolvedServerCredentials() throws Exception {
		AtomicReference<String> authorization = new AtomicReference<String>();
		AtomicReference<String> requestBody = new AtomicReference<String>();
		upstream = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		upstream.createContext("/v1/chat/completions", exchange -> {
			authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
			requestBody.set(readString(exchange.getRequestBody()));
			byte[] body = ("{\"id\":\"chatcmpl-resolved\",\"object\":\"chat.completion\","
					+ "\"created\":1,\"model\":\"resolver-model\",\"choices\":[{\"index\":0,"
					+ "\"message\":{\"role\":\"assistant\",\"content\":\"resolved response\"},"
					+ "\"finish_reason\":\"stop\"}]}").getBytes("UTF-8");
			exchange.getResponseHeaders().set("Content-Type", "application/json");
			exchange.sendResponseHeaders(200, body.length);
			exchange.getResponseBody().write(body);
			exchange.close();
		});
		upstream.start();
		LangChain4jTinyEngineAiProvider provider = provider(aiModel("registry-model", AiModelType.CHAT,
				LangChain4jModelType.CHAT, new RecordingChatModel()));
		TinyEngineChatRequest request = request("registry-model", false);
		request.setProvider("openai");
		request.setBaseUrl("http://127.0.0.1:" + upstream.getAddress().getPort() + "/v1");
		request.setApiKey("resolver-key");
		request.setModel("resolver-model");
		Map<String, Object> modelSettings = new HashMap<String, Object>();
		modelSettings.put("temperature", Double.valueOf(0.2D));
		modelSettings.put("maxTokens", Integer.valueOf(64));
		request.setModelSettings(modelSettings);
		request.setMaxTokens(Integer.valueOf(96));

		TinyEngineChatResponse response = provider.chat(request);

		assertEquals("resolved response", response.getContent());
		assertEquals("Bearer resolver-key", authorization.get());
		JsonNode upstreamRequest = new ObjectMapper().readTree(requestBody.get());
		assertEquals(0.2D, upstreamRequest.path("temperature").doubleValue(), 0.0001D);
		assertEquals(96, upstreamRequest.path("max_tokens").intValue());
	}

	@Test
	public void shouldNormalizeOpenAiCompatibleChatAuthorizationFailures() throws Exception {
		LangChain4jTinyEngineAiProvider provider = provider(aiModel("registry-model", AiModelType.CHAT,
				LangChain4jModelType.CHAT, new RecordingChatModel()));

		for (int statusCode : new int[] {401, 403}) {
			startAuthorizationFailure(statusCode);

			TinyEngineUpstreamException exception = assertThrows(TinyEngineUpstreamException.class,
					() -> provider.chat(resolvedRequest("registry-model", false)));

			assertEquals(statusCode, exception.getStatusCode());
			assertEquals("data: {\"error\":{\"code\":\"INVALID_API_KEY\",\"message\":\"TinyEngine AI upstream credential rejected\"}}\n\ndata: [DONE]\n\n",
					new String(exception.toSseError(), "UTF-8"));
		}
	}

	@Test
	public void shouldNormalizeOpenAiCompatibleStreamingAuthorizationFailures() throws Exception {
		LangChain4jTinyEngineAiProvider provider = provider(aiModel("registry-model", AiModelType.STREAMING_CHAT,
				LangChain4jModelType.STREAMING_CHAT, new RecordingStreamingChatModel()));

		for (int statusCode : new int[] {401, 403}) {
			startAuthorizationFailure(statusCode);

			TinyEngineUpstreamException exception = assertThrows(TinyEngineUpstreamException.class,
					() -> provider.stream(resolvedRequest("registry-model", true)));

			assertEquals(statusCode, exception.getStatusCode());
			assertEquals("data: {\"error\":{\"code\":\"INVALID_API_KEY\",\"message\":\"TinyEngine AI upstream credential rejected\"}}\n\ndata: [DONE]\n\n",
					new String(exception.toSseError(), "UTF-8"));
		}
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

	private TinyEngineChatRequest resolvedRequest(String modelKey, boolean stream) {
		TinyEngineChatRequest request = request(modelKey, stream);
		request.setProvider("openai");
		request.setBaseUrl("http://127.0.0.1:" + upstream.getAddress().getPort() + "/v1");
		request.setApiKey("resolver-key");
		request.setModel("resolver-model");
		return request;
	}

	private void startAuthorizationFailure(int statusCode) throws IOException {
		if (upstream != null) {
			upstream.stop(0);
		}
		upstream = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		upstream.createContext("/v1/chat/completions", exchange -> {
			readAll(exchange.getRequestBody());
			byte[] body = "{\"error\":{\"message\":\"invalid key\"}}".getBytes("UTF-8");
			exchange.getResponseHeaders().set("Content-Type", "application/json");
			exchange.sendResponseHeaders(statusCode, body.length);
			exchange.getResponseBody().write(body);
			exchange.close();
		});
		upstream.start();
	}

	private void readAll(java.io.InputStream input) throws IOException {
		readString(input);
	}

	private String readString(java.io.InputStream input) throws IOException {
		java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
		byte[] buffer = new byte[256];
		int read;
		while ((read = input.read(buffer)) >= 0) {
			output.write(buffer, 0, read);
		}
		return new String(output.toByteArray(), "UTF-8");
	}

	private LangChain4jTinyEngineAiProvider provider(AiModel model) {
		TinyEngineAiProperties properties = new TinyEngineAiProperties();
		properties.getModel().setChatKey("chat-default");
		properties.getModel().setStreamingChatKey("stream-default");
		return new LangChain4jTinyEngineAiProvider(new SingleModelRegistry(model), properties);
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

	private static final class RecordingChatModel implements ChatLanguageModel {
		private List<ChatMessage> messages = Collections.emptyList();
		private ChatRequest request;
		@Override public Response<AiMessage> generate(List<ChatMessage> messages) {
			this.messages = messages;
			return Response.from(AiMessage.from("chat answer"));
		}
		@Override public ChatResponse chat(ChatRequest request) {
			this.request = request;
			this.messages = request.messages();
			return ChatResponse.builder().aiMessage(AiMessage.from("chat answer")).build();
		}
	}

	private static final class RecordingStreamingChatModel implements StreamingChatLanguageModel {
		private List<ChatMessage> messages = Collections.emptyList();
		@Override public void generate(List<ChatMessage> messages, StreamingResponseHandler<AiMessage> handler) {
			this.messages = messages;
			handler.onNext("he");
			handler.onNext("llo");
			handler.onComplete(Response.from(AiMessage.from("stream answer")));
		}
	}
}
