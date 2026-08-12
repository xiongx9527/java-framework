package com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine;

import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.chat.TinyEngineChatRequest;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.chat.TinyEngineChatResponse;
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
import reactor.core.publisher.Mono;

@RestController
public class TinyEngineAiWebFluxController {

	private final TinyEngineAiChatService chatService;
	private final TinyEngineRagService ragService;

	public TinyEngineAiWebFluxController(TinyEngineAiChatService chatService, TinyEngineRagService ragService) {
		this.chatService = chatService;
		this.ragService = ragService;
	}

	@PostMapping("${fw.plus.tiny-engine.ai.web.base-path:/tiny-engine/ai}${fw.plus.tiny-engine.ai.web.chat-path:/chat}")
	public Mono<TinyEngineChatResponse> chat(@RequestBody TinyEngineChatRequest request) {
		return Mono.fromCallable(() -> {
			requireInvocationIdentifiers(request);
			return chatService.chat(request);
		}).onErrorMap(TinyEngineUpstreamException.class, this::modelInvocationFailure);
	}

	@PostMapping("${fw.plus.tiny-engine.ai.web.base-path:/tiny-engine/ai}${fw.plus.tiny-engine.ai.web.chat-completions-path:/chat/completions}")
	public Mono<Object> chatCompletions(@RequestBody TinyEngineChatRequest request) {
		return Mono.fromCallable(() -> {
			requireInvocationIdentifiers(request);
			if (Boolean.TRUE.equals(request.getStream())) {
				return chatService.stream(request);
			}
			return chatService.chat(request);
		}).onErrorMap(TinyEngineUpstreamException.class, this::modelInvocationFailure);
	}

	@PostMapping("${fw.plus.tiny-engine.ai.web.base-path:/tiny-engine/ai}${fw.plus.tiny-engine.ai.web.encrypt-key-path:/encrypt-key}")
	public Mono<Void> encryptKey() {
		return Mono.error(new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED,
				"TinyEngine AI encrypt key is not supported"));
	}

	@PostMapping("${fw.plus.tiny-engine.ai.web.base-path:/tiny-engine/ai}${fw.plus.tiny-engine.ai.web.search-path:/search}")
	public Mono<TinyEngineRagSearchResult> search(@RequestBody TinyEngineRagRequest request) {
		return Mono.fromCallable(() -> ragService.search(request));
	}

	private void requireInvocationIdentifiers(TinyEngineChatRequest request) {
		if (request == null || request.getAppId() == null || request.getServiceId() == null
				|| isBlank(request.getModelName())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "appId, serviceId and modelName are required");
		}
	}

	private ResponseStatusException modelInvocationFailure(TinyEngineUpstreamException ex) {
		return new ResponseStatusException(HttpStatus.BAD_GATEWAY,
				ex.getStatusCode() == 401 || ex.getStatusCode() == 403
						? "TinyEngine AI model invocation failed" : "TinyEngine AI upstream request failed");
	}

	private boolean isBlank(String value) {
		return value == null || value.trim().length() == 0;
	}
}
