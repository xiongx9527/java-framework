package com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.official;

import java.nio.charset.StandardCharsets;

public class TinyEngineUpstreamException extends IllegalStateException {

	private final int statusCode;
	private final String errorCode;

	public TinyEngineUpstreamException(int statusCode, String message) {
		this(statusCode, "UPSTREAM_ERROR", message);
	}

	public TinyEngineUpstreamException(int statusCode, String errorCode, String message) {
		super(message);
		this.statusCode = statusCode;
		this.errorCode = errorCode;
	}

	public int getStatusCode() {
		return statusCode;
	}

	public byte[] toSseError() {
		String frame = "data: {\"error\":{\"code\":\"" + errorCode + "\",\"message\":\""
				+ escape(getMessage()) + "\"}}\n\ndata: [DONE]\n\n";
		return frame.getBytes(StandardCharsets.UTF_8);
	}

	private String escape(String value) {
		String text = value == null ? "TinyEngine AI upstream request failed" : value;
		return text.replace("\\", "\\\\").replace("\"", "\\\"")
				.replace("\n", "\\n").replace("\r", "\\r");
	}
}
