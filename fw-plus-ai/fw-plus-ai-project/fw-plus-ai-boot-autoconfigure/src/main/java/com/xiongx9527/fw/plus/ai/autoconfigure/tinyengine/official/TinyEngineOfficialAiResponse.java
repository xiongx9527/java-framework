package com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.official;

import org.springframework.http.HttpHeaders;

public class TinyEngineOfficialAiResponse {

	private final int statusCode;
	private final HttpHeaders headers;
	private final byte[] body;

	public TinyEngineOfficialAiResponse(int statusCode, HttpHeaders headers, byte[] body) {
		this.statusCode = statusCode;
		this.headers = headers == null ? new HttpHeaders() : headers;
		this.body = body == null ? new byte[0] : body;
	}

	public int getStatusCode() {
		return statusCode;
	}

	public int getClientStatusCode() {
		return statusCode == 401 || statusCode == 403 ? 502 : statusCode;
	}

	public HttpHeaders getHeaders() {
		return headers;
	}

	public byte[] getBody() {
		return body;
	}
}
