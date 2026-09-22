package com.xiongx9527.fw.plus.ai.mcp.spi;

import java.util.Map;

/**
 * 资源内容
 *
 * @author xiongxiao
 * @date 2025-09-20
 */
public class McpResourceContent {
	private String uri;
	private String mimeType;
	private String text;
	private byte[] blob;

	public McpResourceContent() {
	}

	public McpResourceContent(String uri, String text) {
		this.uri = uri;
		this.text = text;
	}

	public static McpResourceContent text(String uri, String text) {
		return new McpResourceContent(uri, text);
	}

	public static McpResourceContent blob(String uri, String mimeType, byte[] blob) {
		McpResourceContent content = new McpResourceContent();
		content.uri = uri;
		content.mimeType = mimeType;
		content.blob = blob;
		return content;
	}

	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	public String getMimeType() {
		return mimeType;
	}

	public void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public byte[] getBlob() {
		return blob;
	}

	public void setBlob(byte[] blob) {
		this.blob = blob;
	}

	public Map<String, Object> toMap() {
		Map<String, Object> map = new java.util.LinkedHashMap<>();
		map.put("uri", uri);
		if (mimeType != null) map.put("mimeType", mimeType);
		if (text != null) map.put("text", text);
		if (blob != null) map.put("blob", java.util.Base64.getEncoder().encodeToString(blob));
		return map;
	}
}
