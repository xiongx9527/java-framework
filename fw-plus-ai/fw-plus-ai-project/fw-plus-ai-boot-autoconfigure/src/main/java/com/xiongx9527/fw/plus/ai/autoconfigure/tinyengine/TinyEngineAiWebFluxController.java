package com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine;

import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.chat.TinyEngineChatRequest;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.chat.TinyEngineChatResponse;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.chat.TinyEngineChatSseEncoder;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.official.TinyEngineUpstreamException;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.rag.TinyEngineRagRequest;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.rag.TinyEngineRagSearchResult;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.service.TinyEngineAiChatService;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.service.TinyEngineRagService;
import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.webflux.TinyEngineReactiveExecutor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@RestController
public class TinyEngineAiWebFluxController {

	private final TinyEngineAiChatService chatService;
	private final TinyEngineRagService ragService;
	private final TinyEngineReactiveExecutor executor;

	public TinyEngineAiWebFluxController(TinyEngineAiChatService chatService, TinyEngineRagService ragService,
			TinyEngineReactiveExecutor executor) {
		this.chatService = chatService;
		this.ragService = ragService;
		this.executor = executor;
	}

	public Mono<TinyEngineChatResponse> chat(@RequestBody TinyEngineChatRequest request) {
		return executor.submit(() -> {
			requireInvocationIdentifiers(request);
			return chatService.chat(request);
		}).onErrorMap(TinyEngineUpstreamException.class, this::modelInvocationFailure);
	}

	@PostMapping({
			"${fw.plus.tiny-engine.web.base-path:}/app-center/api/ai/chat",
			"${fw.plus.tiny-engine.web.base-path:}/app-center/api/chat/completions"
	})
	public Mono<ResponseEntity<byte[]>> chatCompletions(@RequestBody TinyEngineChatRequest request) {
		return executor.submit(() -> {
			requireInvocationIdentifiers(request);
			if (Boolean.TRUE.equals(request.getStream())) {
				return ResponseEntity.ok().contentType(MediaType.TEXT_EVENT_STREAM)
						.body(TinyEngineChatSseEncoder.encodeBytes(chatService.stream(request)));
			}
			return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON)
					.body(TinyEngineChatSseEncoder.encodeJson(
							TinyEngineChatSseEncoder.chatCompletion(chatService.chat(request), request.getModelName())));
		}).onErrorMap(TinyEngineUpstreamException.class, this::modelInvocationFailure);
	}

	@PostMapping({
			"${fw.plus.tiny-engine.web.base-path:}/app-center/api/encrypt-key"
	})
	public Mono<Void> encryptKey() {
		return Mono.error(new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED,
				"TinyEngine AI encrypt key is not supported"));
	}

	@PostMapping({
			"${fw.plus.tiny-engine.web.base-path:}/app-center/api/ai/search"
	})
	public Mono<TinyEngineRagSearchResult> search(@RequestBody TinyEngineRagRequest request) {
		return executor.submit(() -> ragService.search(request));
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
