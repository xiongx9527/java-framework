package com.xiongx9527.fw.plus.tinyengine.support.auth;

public interface TinyEnginePermissionEvaluator {

	void check(TinyEnginePrincipal principal, String permission);
}
