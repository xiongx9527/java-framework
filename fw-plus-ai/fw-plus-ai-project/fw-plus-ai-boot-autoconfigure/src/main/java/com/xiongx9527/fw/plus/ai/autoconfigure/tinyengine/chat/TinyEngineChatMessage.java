package com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.chat;

public class TinyEngineChatMessage {

	private String role;
	private String content;

	public TinyEngineChatMessage() {
	}

	public TinyEngineChatMessage(String role, String content) {
		this.role = role;
		this.content = content;
	}

	public String getRole() {
		return role;
	}

	public void setRole(String role) {
		this.role = role;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}
}
