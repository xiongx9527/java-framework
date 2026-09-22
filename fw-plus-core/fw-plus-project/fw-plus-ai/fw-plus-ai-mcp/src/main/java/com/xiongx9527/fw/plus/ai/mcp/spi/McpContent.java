package com.xiongx9527.fw.plus.ai.mcp.spi;

import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * MCP 内容项
 *
 * <p>对齐 MCP 规范 content 类型：text、image、resource、resource_link（2025-11-25）。
 *
 * @author xiongxiao
 * @date 2025-09-20
 */
public class McpContent {
	public static final String TYPE_TEXT = "text";
	public static final String TYPE_IMAGE = "image";
	public static final String TYPE_RESOURCE = "resource";
	public static final String TYPE_RESOURCE_LINK = "resource_link";

	private String type;
	private String text;
	private String data; // base64-encoded image data
	private String mimeType; // for image
	private McpResourceContent resource; // for embedded resource
	private String uri; // for resource_link

	public McpContent() {
	}

	public static McpContent text(String text) {
		McpContent content = new McpContent();
		content.type = TYPE_TEXT;
		content.text = text;
		return content;
	}

	public static McpContent image(byte[] data, String mimeType) {
		McpContent content = new McpContent();
		content.type = TYPE_IMAGE;
		content.data = Base64.getEncoder().encodeToString(data);
		content.mimeType = mimeType;
		return content;
	}

	public static McpContent image(String base64Data, String mimeType) {
		McpContent content = new McpContent();
		content.type = TYPE_IMAGE;
		content.data = base64Data;
		content.mimeType = mimeType;
		return content;
	}

	public static McpContent resource(McpResourceContent resource) {
		McpContent content = new McpContent();
		content.type = TYPE_RESOURCE;
		content.resource = resource;
		return content;
	}

	public static McpContent resourceLink(String uri) {
		McpContent content = new McpContent();
		content.type = TYPE_RESOURCE_LINK;
		content.uri = uri;
		return content;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}

	public String getMimeType() {
		return mimeType;
	}

	public void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}

	public McpResourceContent getResource() {
		return resource;
	}

	public void setResource(McpResourceContent resource) {
		this.resource = resource;
	}

	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	public Map<String, Object> toMap() {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("type", type);
		if (text != null) {
			map.put("text", text);
		}
		if (data != null) {
			map.put("data", data);
		}
		if (mimeType != null) {
			map.put("mimeType", mimeType);
		}
		if (resource != null) {
			map.put("resource", resource.toMap());
		}
		if (uri != null) {
			map.put("uri", uri);
		}
		return map;
	}
}
