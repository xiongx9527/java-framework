package com.xiongx9527.fw.plus.tinyengine.support.auth;

public interface TinyEngineAccountProvider {

	TinyEnginePrincipal register(TinyEngineRegistrationRequest request);

	void requestPasswordReset(TinyEnginePasswordResetRequest request);
}
