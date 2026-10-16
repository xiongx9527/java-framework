package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.session;

import java.io.Serializable;

public class ExplicitSessionIdResolver implements SessionIdResolver, Serializable {

	private static final long serialVersionUID = 1L;

	private final String sessionId;

	public ExplicitSessionIdResolver(String sessionId) {
		this.sessionId = sessionId;
	}

	@Override
	public String resolve(SessionContext ctx) {
		return sessionId;
	}

}