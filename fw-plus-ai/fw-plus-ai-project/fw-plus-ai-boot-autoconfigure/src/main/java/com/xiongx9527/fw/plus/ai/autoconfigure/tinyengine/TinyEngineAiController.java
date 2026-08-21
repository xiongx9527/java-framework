package com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine;

import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.chat.TinyEngineChatRequest;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.chat.TinyEngineChatResponse;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.chat.TinyEngineChatSseEncoder;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.official.TinyEngineUpstreamException;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.rag.TinyEngineRagRequest;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.rag.TinyEngineRagSearchResult;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.service.TinyEngineAiChatService;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.service.TinyEngineRagService;
import com.xiongx9527.fw.plus.tinyengine.common.model.Result;
import com.xiongx9527.fw.plus.tinyengine.support.auth.TinyEngineAccessDeniedException;
import com.xiongx9527.fw.plus.tinyengine.support.auth.TinyEngineUnauthenticatedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

	public TinyEngineChatResponse chat(@RequestBody TinyEngineChatRequest request) {
		return chatResponse(request);
	}

	@PostMapping({
			"${fw.plus.tiny-engine.web.base-path:}/app-center/api/ai/chat",
			"${fw.plus.tiny-engine.web.base-path:}/app-center/api/chat/completions"
	})
	public ResponseEntity<Object> chatCompletions(@RequestBody TinyEngineChatRequest request) {
		try {
			requireInvocationIdentifiers(request);
			if (Boolean.TRUE.equals(request.getStream())) {
				return ResponseEntity.ok().contentType(MediaType.TEXT_EVENT_STREAM)
						.body(TinyEngineChatSseEncoder.encode(chatService.stream(request)));
			}
			return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON)
					.body(TinyEngineChatSseEncoder.chatCompletion(chatService.chat(request), request.getModelName()));
		} catch (TinyEngineUpstreamException ex) {
			return failure(HttpStatus.BAD_GATEWAY, "502", upstreamMessage(ex));
		} catch (TinyEngineUnauthenticatedException ex) {
			return failure(HttpStatus.UNAUTHORIZED, "401", safeMessage(ex, "TinyEngine authentication is required"));
		} catch (TinyEngineAccessDeniedException ex) {
			return failure(HttpStatus.FORBIDDEN, "403", safeMessage(ex, "TinyEngine application permission denied"));
		} catch (IllegalArgumentException ex) {
			return failure(HttpStatus.BAD_REQUEST, "400", "TinyEngine AI model configuration is invalid");
		} catch (IllegalStateException ex) {
			return failure(HttpStatus.INTERNAL_SERVER_ERROR, "500", "TinyEngine AI model configuration is unavailable");
		} catch (RuntimeException ex) {
			return failure(HttpStatus.INTERNAL_SERVER_ERROR, "500", "TinyEngine AI request failed");
		}
	}

	@PostMapping({
			"${fw.plus.tiny-engine.web.base-path:}/app-center/api/encrypt-key"
	})
	public void encryptKey() {
		throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED,
				"TinyEngine AI encrypt key is not supported");
	}

	@PostMapping({
			"${fw.plus.tiny-engine.web.base-path:}/app-center/api/ai/search"
	})
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

	private ResponseEntity<Object> failure(HttpStatus status, String code, String message) {
		return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON)
				.body(Result.failed(code, message));
	}

	private String upstreamMessage(TinyEngineUpstreamException ex) {
		if (ex != null && (ex.getStatusCode() == 401 || ex.getStatusCode() == 403)) {
			return "TinyEngine AI model invocation failed";
		}
		return "TinyEngine AI upstream request failed";
	}

	private String safeMessage(RuntimeException exception, String fallback) {
		return exception == null || exception.getMessage() == null || exception.getMessage().trim().isEmpty()
				? fallback : exception.getMessage();
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
