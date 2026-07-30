package com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.chat;

public class TinyEngineChatRequest {

	private String modelKey;
	private String message;

	public String getModelKey() {
		return modelKey;
	}

	public void setModelKey(String modelKey) {
		this.modelKey = modelKey;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}
}
