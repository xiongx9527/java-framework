package com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.provider;

import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.chat.TinyEngineChatRequest;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.chat.TinyEngineChatResponse;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.chat.TinyEngineChatStreamChunk;

import java.util.List;

public interface TinyEngineAiProvider {

	String getKey();

	boolean supports(TinyEngineAiCapability capability);

	TinyEngineChatResponse chat(TinyEngineChatRequest request);

	List<TinyEngineChatStreamChunk> stream(TinyEngineChatRequest request);
}
