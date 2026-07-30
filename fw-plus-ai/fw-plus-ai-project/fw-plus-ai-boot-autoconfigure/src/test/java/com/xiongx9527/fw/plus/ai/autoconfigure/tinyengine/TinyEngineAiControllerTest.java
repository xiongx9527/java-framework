package com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine;

import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.chat.TinyEngineChatRequest;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.chat.TinyEngineChatResponse;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.chat.TinyEngineChatStreamChunk;
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

	private static final class RecordingChatService implements TinyEngineAiChatService {
		private final TinyEngineChatResponse response = new TinyEngineChatResponse("chat");
		private final List<TinyEngineChatStreamChunk> chunks = Arrays.asList(new TinyEngineChatStreamChunk("stream", true));
		private TinyEngineChatRequest lastChatRequest;
		private TinyEngineChatRequest lastStreamRequest;
		@Override public TinyEngineChatResponse chat(TinyEngineChatRequest request) { lastChatRequest = request; return response; }
		@Override public List<TinyEngineChatStreamChunk> stream(TinyEngineChatRequest request) { lastStreamRequest = request; return chunks; }
	}

	private static final class RecordingRagService implements TinyEngineRagService {
		private final TinyEngineRagSearchResult result = new TinyEngineRagSearchResult(Collections.emptyList());
		private TinyEngineRagRequest lastRequest;
		@Override public boolean isAvailable() { return true; }
		@Override public String getUnavailableReason() { return null; }
		@Override public TinyEngineRagSearchResult search(TinyEngineRagRequest request) { lastRequest = request; return result; }
	}
}
