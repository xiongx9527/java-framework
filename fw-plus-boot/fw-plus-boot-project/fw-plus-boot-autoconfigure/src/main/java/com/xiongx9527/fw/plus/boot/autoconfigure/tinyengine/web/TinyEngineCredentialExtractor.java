package com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.web;

import com.xiongx9527.fw.plus.tinyengine.support.auth.TinyEngineCredential;
import com.xiongx9527.fw.plus.tinyengine.support.auth.TinyEngineUnauthenticatedException;

public class TinyEngineCredentialExtractor {

	private static final String BEARER_PREFIX = "Bearer ";

	public TinyEngineCredential extract(String authorization, String tenantId) {
		if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
			throw new TinyEngineUnauthenticatedException("Missing Bearer token");
		}
		String token = authorization.substring(BEARER_PREFIX.length()).trim();
		if (token.length() == 0) {
			throw new TinyEngineUnauthenticatedException("Missing Bearer token");
		}
		return new TinyEngineCredential(token, tenantId);
	}
}
