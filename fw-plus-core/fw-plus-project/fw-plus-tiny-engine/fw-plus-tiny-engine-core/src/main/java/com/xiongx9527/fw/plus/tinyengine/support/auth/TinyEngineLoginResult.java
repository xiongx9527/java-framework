package com.xiongx9527.fw.plus.tinyengine.support.auth;

public class TinyEngineLoginResult {

	private final String accessToken;
	private final String tokenType;
	private final Long expiresIn;

	public TinyEngineLoginResult(String accessToken, String tokenType, Long expiresIn) {
		this.accessToken = accessToken;
		this.tokenType = tokenType;
		this.expiresIn = expiresIn;
	}

	public String getAccessToken() {
		return accessToken;
	}

	public String getTokenType() {
		return tokenType;
	}

	public Long getExpiresIn() {
		return expiresIn;
	}
}
