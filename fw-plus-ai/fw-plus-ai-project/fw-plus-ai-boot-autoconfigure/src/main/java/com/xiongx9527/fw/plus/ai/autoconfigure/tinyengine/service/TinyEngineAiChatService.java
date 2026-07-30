package com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.service;

import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.chat.TinyEngineChatRequest;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.chat.TinyEngineChatResponse;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.chat.TinyEngineChatStreamChunk;

import java.util.List;

public interface TinyEngineAiChatService {

	default boolean isAvailable() {
		return true;
	}

	default String getUnavailableReason() {
		return null;
	}

	TinyEngineChatResponse chat(TinyEngineChatRequest request);

	List<TinyEngineChatStreamChunk> stream(TinyEngineChatRequest request);
}
