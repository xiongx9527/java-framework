package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.provider.sentinel;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.provider.key.MagicApiRequestKeyResolver;
import org.ssssssss.magicapi.core.context.RequestEntity;
import org.ssssssss.magicapi.core.interceptor.RequestInterceptor;
import org.ssssssss.magicapi.core.interceptor.ResultProvider;

public class MagicApiSentinelGuardRequestInterceptor implements RequestInterceptor {

	private final ResultProvider resultProvider;
	private final MagicApiRequestKeyResolver keyResolver;
	private final MagicApiSentinelResourceResolver resourceResolver;
	private final MagicApiSentinelRuleManager ruleManager;

	public MagicApiSentinelGuardRequestInterceptor(ResultProvider resultProvider,
												   MagicApiRequestKeyResolver keyResolver,
												   MagicApiSentinelResourceResolver resourceResolver,
												   MagicApiSentinelRuleManager ruleManager) {
		this.resultProvider = resultProvider;
		this.keyResolver = keyResolver;
		this.resourceResolver = resourceResolver;
		this.ruleManager = ruleManager;
	}

	@Override
	public Object preHandle(RequestEntity requestEntity) {
		return null;
	}
}
