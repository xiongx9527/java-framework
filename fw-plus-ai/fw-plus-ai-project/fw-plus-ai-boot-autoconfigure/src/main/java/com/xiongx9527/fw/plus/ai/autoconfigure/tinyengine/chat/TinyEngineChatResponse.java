package com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.chat;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class TinyEngineChatResponse {

	private final String content;
	private final Map<String, Object> metadata;

	public TinyEngineChatResponse(String content) {
		this(content, Collections.<String, Object>emptyMap());
	}

	public TinyEngineChatResponse(String content, Map<String, Object> metadata) {
		this.content = content;
		this.metadata = metadata == null ? new LinkedHashMap<String, Object>() : new LinkedHashMap<String, Object>(metadata);
	}

	public String getContent() {
		return content;
	}

	public Map<String, Object> getMetadata() {
		return Collections.unmodifiableMap(metadata);
	}
}
