package com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine;

import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.chat.TinyEngineChatRequest;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.chat.TinyEngineChatResponse;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.chat.TinyEngineChatStreamChunk;
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
		return chatService.chat(request);
	}

	@PostMapping("${fw.plus.tiny-engine.ai.web.base-path:/tiny-engine/ai}${fw.plus.tiny-engine.ai.web.chat-completions-path:/chat/completions}")
	public Object chatCompletions(@RequestBody TinyEngineChatRequest request) {
		if (Boolean.TRUE.equals(request.getStream())) {
			return chatService.stream(request);
		}
		return chatService.chat(request);
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
}
