package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.provider.repeat;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.enums.Options;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.lock.Lock;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.provider.ParamConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.ssssssss.magicapi.core.config.Constants;
import org.ssssssss.magicapi.core.context.RequestContext;
import org.ssssssss.magicapi.core.context.RequestEntity;
import org.ssssssss.magicapi.core.interceptor.RequestInterceptor;
import org.ssssssss.magicapi.core.interceptor.ResultProvider;
import org.ssssssss.magicapi.core.model.ApiInfo;
import org.ssssssss.magicapi.core.model.JsonBean;
import org.ssssssss.magicapi.core.servlet.MagicHttpServletRequest;
import org.ssssssss.magicapi.core.servlet.MagicHttpServletResponse;
import org.ssssssss.script.MagicScriptContext;

@Slf4j
public class RepeatSubmitRequestInterceptor implements RequestInterceptor {

	static final String LOCK_CONTEXT_KEY = "__fw_plus_repeat_submit_lock_info";

	private final Lock lock;
	private final ResultProvider resultProvider;
	private final RepeatSubmitKeyResolver keyResolver;

	public RepeatSubmitRequestInterceptor(Lock lock, ResultProvider resultProvider, RepeatSubmitKeyResolver keyResolver) {
		this.lock = lock;
		this.resultProvider = resultProvider;
		this.keyResolver = keyResolver;
	}

	@Override
	public Object preHandle(RequestEntity requestEntity) {
		if (!ParamConfig.isEnabled(requestEntity.getApiInfo(), Options.AVOID_REPEAT_SUBMIT_ENABLE.getValue())) {
			return null;
		}
		MagicScriptContext context = ensureMagicScriptContext(requestEntity);
		long timeout = parseTimeout(requestEntity);
		String key;
		try {
			key = keyResolver.resolve(requestEntity.getApiInfo(), requestEntity);
		} catch (Exception e) {
			return failure(requestEntity, e.getMessage());
		}
		try {
			Object lockInfo = lock.lock(key, timeout, timeout);
			context.set(LOCK_CONTEXT_KEY, lockInfo);
			return null;
		} catch (Exception e) {
			return failure(requestEntity, repeatMessage(requestEntity));
		}
	}

	@Override
	public Object preHandle(ApiInfo info, MagicScriptContext context,
							MagicHttpServletRequest request, MagicHttpServletResponse response) {
		return preHandle(requestEntity(info, context, request, response));
	}

	@Override
	public void afterCompletion(RequestEntity requestEntity, Object returnValue, Throwable throwable) {
		MagicScriptContext context = requestEntity == null ? null : requestEntity.getMagicScriptContext();
		Object lockInfo = context == null ? null : context.get(LOCK_CONTEXT_KEY);
		if (lockInfo == null) {
			return;
		}
		try {
			lock.release(lockInfo);
		} catch (Exception e) {
			log.warn("release magic-api repeat submit lock failed", e);
		} finally {
			context.set(LOCK_CONTEXT_KEY, null);
		}
	}

	@Override
	public void afterCompletion(ApiInfo info, MagicScriptContext context, Object returnValue,
								MagicHttpServletRequest request, MagicHttpServletResponse response,
								Throwable throwable) {
		afterCompletion(requestEntity(info, context, request, response), returnValue, throwable);
	}

	private RequestEntity requestEntity(ApiInfo info, MagicScriptContext context,
										MagicHttpServletRequest request, MagicHttpServletResponse response) {
		RequestEntity entity = RequestContext.getRequestEntity();
		if (entity != null) {
			if (entity.getMagicScriptContext() == null) {
				entity.setMagicScriptContext(context);
			}
			return entity;
		}
		return RequestEntity.create()
				.info(info)
				.request(request)
				.response(response)
				.setMagicScriptContext(context);
	}

	private MagicScriptContext ensureMagicScriptContext(RequestEntity requestEntity) {
		MagicScriptContext context = requestEntity.getMagicScriptContext();
		if (context == null) {
			context = new MagicScriptContext();
			requestEntity.setMagicScriptContext(context);
		}
		return context;
	}

	private long parseTimeout(RequestEntity requestEntity) {
		String value = ParamConfig.get(requestEntity.getApiInfo(), Options.AVOID_REPEAT_SUBMIT_TIMEOUT.getValue());
		try {
			return Long.parseLong(StringUtils.defaultIfBlank(value, Options.AVOID_REPEAT_SUBMIT_TIMEOUT.getDefaultValue()));
		} catch (Exception e) {
			log.warn("invalid avoid_repeat_submit_timeout={}, use default={}", value, Options.AVOID_REPEAT_SUBMIT_TIMEOUT.getDefaultValue());
			return Long.parseLong(Options.AVOID_REPEAT_SUBMIT_TIMEOUT.getDefaultValue());
		}
	}

	private String repeatMessage(RequestEntity requestEntity) {
		return StringUtils.defaultIfBlank(
				ParamConfig.get(requestEntity.getApiInfo(), Options.AVOID_REPEAT_SUBMIT_MSG.getValue()),
				Options.AVOID_REPEAT_SUBMIT_MSG.getDefaultValue());
	}

	private Object failure(RequestEntity requestEntity, String message) {
		Object result = resultProvider.buildResult(requestEntity, Constants.RESPONSE_CODE_INVALID, message, null);
		if (result instanceof JsonBean) {
			((JsonBean<?>) result).setSuccess(false);
		}
		return result;
	}
}
