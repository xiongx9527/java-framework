package com.xiongx9527.fw.plus.tinyengine.support.context;

public class SystemTinyEngineUserContext implements TinyEngineUserContext {

	private static final TinyEngineUser SYSTEM_USER = new TinyEngineUser("system", "system", "system", "system", Integer.valueOf(0));

	@Override
	public TinyEngineUser getCurrentUser() {
		return SYSTEM_USER;
	}
}
