package com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine;

import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.chat.TinyEngineChatRequest;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.chat.TinyEngineChatResponse;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.chat.TinyEngineChatStreamChunk;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.official.TinyEngineUpstreamException;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.rag.TinyEngineRagRequest;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.rag.TinyEngineRagSearchResult;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.service.TinyEngineAiChatService;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.service.TinyEngineRagService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class TinyEngineAiController {

	private final TinyEngineAiChatService chatService;
	private final TinyEngineRagService ragService;

	public TinyEngineAiController(TinyEngineAiChatService chatService, TinyEngineRagService ragService) {
		this.chatService = chatService;
		this.ragService = ragService;
	}

	@PostMapping("${fw.plus.tiny-engine.ai.web.base-path:/tiny-engine/ai}${fw.plus.tiny-engine.ai.web.chat-path:/chat}")
	public TinyEngineChatResponse chat(@RequestBody TinyEngineChatRequest request) {
		return chatResponse(request);
	}

	@PostMapping("${fw.plus.tiny-engine.ai.web.base-path:/tiny-engine/ai}${fw.plus.tiny-engine.ai.web.chat-completions-path:/chat/completions}")
	public Object chatCompletions(@RequestBody TinyEngineChatRequest request) {
		try {
			requireInvocationIdentifiers(request);
			if (Boolean.TRUE.equals(request.getStream())) {
				return chatService.stream(request);
			}
			return chatService.chat(request);
		} catch (TinyEngineUpstreamException ex) {
			throw modelInvocationFailure(ex);
		}
	}

	@PostMapping("${fw.plus.tiny-engine.ai.web.base-path:/tiny-engine/ai}${fw.plus.tiny-engine.ai.web.encrypt-key-path:/encrypt-key}")
	public void encryptKey() {
		throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED,
				"TinyEngine AI encrypt key is not supported");
	}

	@PostMapping("${fw.plus.tiny-engine.ai.web.base-path:/tiny-engine/ai}${fw.plus.tiny-engine.ai.web.search-path:/search}")
	public TinyEngineRagSearchResult search(@RequestBody TinyEngineRagRequest request) {
		return ragService.search(request);
	}

	private TinyEngineChatResponse chatResponse(TinyEngineChatRequest request) {
		try {
			requireInvocationIdentifiers(request);
			return chatService.chat(request);
		} catch (TinyEngineUpstreamException ex) {
			throw modelInvocationFailure(ex);
		}
	}

	private void requireInvocationIdentifiers(TinyEngineChatRequest request) {
		if (request == null || request.getAppId() == null || request.getServiceId() == null
				|| isBlank(request.getModelName())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "appId, serviceId and modelName are required");
		}
	}

	private ResponseStatusException modelInvocationFailure(TinyEngineUpstreamException ex) {
		if (ex.getStatusCode() == 401 || ex.getStatusCode() == 403) {
			return new ResponseStatusException(HttpStatus.BAD_GATEWAY, "TinyEngine AI model invocation failed");
		}
		return new ResponseStatusException(HttpStatus.BAD_GATEWAY, "TinyEngine AI upstream request failed");
	}

	private boolean isBlank(String value) {
		return value == null || value.trim().length() == 0;
	}
}
