package com.xiongx9527.fw.plus.tinyengine.support.auth;

public interface TinyEngineAuthenticationProvider {

	TinyEngineLoginResult login(TinyEngineLoginRequest request);

	TinyEnginePrincipal resolve(TinyEngineCredential credential);

	void logout(TinyEngineCredential credential);
}
