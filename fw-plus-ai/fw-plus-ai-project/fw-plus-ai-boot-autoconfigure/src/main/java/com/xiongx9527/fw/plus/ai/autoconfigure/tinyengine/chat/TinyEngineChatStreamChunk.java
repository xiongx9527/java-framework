package com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.chat;

public class TinyEngineChatStreamChunk {

	private final String content;
	private final boolean completed;

	public TinyEngineChatStreamChunk(String content, boolean completed) {
		this.content = content;
		this.completed = completed;
	}

	public String getContent() {
		return content;
	}

	public boolean isCompleted() {
		return completed;
	}
}
