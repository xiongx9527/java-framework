package com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine;

import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.chat.TinyEngineChatRequest;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.chat.TinyEngineChatResponse;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.chat.TinyEngineChatStreamChunk;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.official.TinyEngineUpstreamException;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.rag.TinyEngineRagRequest;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.rag.TinyEngineRagSearchResult;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.service.TinyEngineAiChatService;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.service.TinyEngineRagService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TinyEngineAiControllerTest {

	@Test
	public void shouldForwardChatStreamAndSearchRequests() {
		RecordingChatService chatService = new RecordingChatService();
		RecordingRagService ragService = new RecordingRagService();
		TinyEngineAiController controller = new TinyEngineAiController(chatService, ragService);
		TinyEngineChatRequest chatRequest = new TinyEngineChatRequest();
		TinyEngineChatRequest streamRequest = new TinyEngineChatRequest();
		identify(chatRequest);
		identify(streamRequest);
		streamRequest.setStream(true);
		TinyEngineRagRequest ragRequest = new TinyEngineRagRequest();

		assertSame(chatService.response, controller.chat(chatRequest));
		assertSame(chatService.response, controller.chatCompletions(chatRequest));
		assertSame(chatService.chunks, controller.chatCompletions(streamRequest));
		assertSame(ragService.result, controller.search(ragRequest));
		assertSame(chatRequest, chatService.lastChatRequest);
		assertSame(streamRequest, chatService.lastStreamRequest);
		assertSame(ragRequest, ragService.lastRequest);
	}

	@Test
	public void shouldRejectUnsupportedEncryptKeyRequest() {
		TinyEngineAiController controller = new TinyEngineAiController(new RecordingChatService(), new RecordingRagService());

		ResponseStatusException exception = assertThrows(ResponseStatusException.class, controller::encryptKey);

		assertEquals(HttpStatus.NOT_IMPLEMENTED, exception.getStatus());
		assertEquals("TinyEngine AI encrypt key is not supported", exception.getReason());
	}

	@Test
	public void shouldConvertUpstreamAuthorizationFailureToModelInvocationError() {
		TinyEngineAiController controller = new TinyEngineAiController(new FailingChatService(), new RecordingRagService());

		ResponseStatusException exception = assertThrows(ResponseStatusException.class,
				() -> controller.chat(identifiedRequest()));

		assertEquals(HttpStatus.BAD_GATEWAY, exception.getStatus());
		assertEquals("TinyEngine AI model invocation failed", exception.getReason());
	}

	@Test
	public void shouldRejectChatRequestWithoutInvocationIdentifiers() {
		TinyEngineAiController controller = new TinyEngineAiController(new RecordingChatService(), new RecordingRagService());

		ResponseStatusException exception = assertThrows(ResponseStatusException.class,
				() -> controller.chat(new TinyEngineChatRequest()));

		assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
		assertEquals("appId, serviceId and modelName are required", exception.getReason());
	}

	private static final class RecordingChatService implements TinyEngineAiChatService {
		private final TinyEngineChatResponse response = new TinyEngineChatResponse("chat");
		private final List<TinyEngineChatStreamChunk> chunks = Arrays.asList(new TinyEngineChatStreamChunk("stream", true));
		private TinyEngineChatRequest lastChatRequest;
		private TinyEngineChatRequest lastStreamRequest;
		@Override public TinyEngineChatResponse chat(TinyEngineChatRequest request) { lastChatRequest = request; return response; }
		@Override public List<TinyEngineChatStreamChunk> stream(TinyEngineChatRequest request) { lastStreamRequest = request; return chunks; }
	}

	private TinyEngineChatRequest identifiedRequest() {
		TinyEngineChatRequest request = new TinyEngineChatRequest();
		identify(request);
		return request;
	}

	private void identify(TinyEngineChatRequest request) {
		request.setAppId(Integer.valueOf(2));
		request.setServiceId(Long.valueOf(10));
		request.setModelName("test-model");
	}

	private static final class RecordingRagService implements TinyEngineRagService {
		private final TinyEngineRagSearchResult result = new TinyEngineRagSearchResult(Collections.emptyList());
		private TinyEngineRagRequest lastRequest;
		@Override public boolean isAvailable() { return true; }
		@Override public String getUnavailableReason() { return null; }
		@Override public TinyEngineRagSearchResult search(TinyEngineRagRequest request) { lastRequest = request; return result; }
	}

	private static final class FailingChatService implements TinyEngineAiChatService {
		@Override public TinyEngineChatResponse chat(TinyEngineChatRequest request) {
			throw new TinyEngineUpstreamException(401, "INVALID_API_KEY", "Bearer server-api-key");
		}
		@Override public List<TinyEngineChatStreamChunk> stream(TinyEngineChatRequest request) { return Collections.emptyList(); }
	}
}
