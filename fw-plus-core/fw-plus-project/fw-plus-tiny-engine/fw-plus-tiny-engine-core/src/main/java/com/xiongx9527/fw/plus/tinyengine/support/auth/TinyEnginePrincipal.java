package com.xiongx9527.fw.plus.tinyengine.support.auth;

import com.xiongx9527.fw.plus.tinyengine.support.context.TinyEngineUser;

public class TinyEnginePrincipal extends TinyEngineUser {

	public TinyEnginePrincipal(String userId, String userName, String tenantId, String renterId, Integer platformId) {
		super(userId, userName, tenantId, renterId, platformId);
	}
}
