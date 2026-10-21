package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.provider.sentinel;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.EntryType;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.flow.FlowException;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowException;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.enums.Options;
import com.xiongx9527.fw.plus.common.i18n.I18nMessageResolver;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.provider.ParamConfig;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.provider.key.MagicApiRequestKeyResolver;
import lombok.extern.slf4j.Slf4j;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
public class MagicApiSentinelGuardRequestInterceptor implements RequestInterceptor {

	static final String ENTRY_CONTEXT_KEY = "__fw_plus_magicapi_sentinel_entries";

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
		ApiInfo info = requestEntity.getApiInfo();
		MagicApiSentinelOptions options = MagicApiSentinelOptions.from(info);
		String resource = resourceResolver.resolve(info);
		ruleManager.ensureRules(resource, options);
		if (!options.isFallbackEffective() && !options.isHotDataEffective()) {
			return null;
		}
		MagicScriptContext context = ensureMagicScriptContext(requestEntity);
		List<EntryHolder> entries = new ArrayList<>();
		String blockMessage = fallbackBlockMessage();
		try {
			if (options.isHotDataEffective()) {
				String hotKey = keyResolver.resolve(info, requestEntity,
						ParamConfig.get(info, Options.HOT_DATA_KEY.getValue()));
				blockMessage = hotDataBlockMessage();
				entries.add(EntryHolder.hotData(enterHotData(resource, hotKey), hotKey));
			} else if (options.isFallbackEffective()) {
				blockMessage = fallbackBlockMessage();
				entries.add(EntryHolder.fallback(enterFallback(resource)));
			}
			context.set(ENTRY_CONTEXT_KEY, entries);
			return null;
		} catch (BlockException e) {
			release(entries);
			context.set(ENTRY_CONTEXT_KEY, null);
			return failure(requestEntity, blockMessage(e, blockMessage));
		} catch (RuntimeException e) {
			release(entries);
			context.set(ENTRY_CONTEXT_KEY, null);
			throw e;
		}
	}

	protected Entry enterFallback(String resource) throws BlockException {
		return SphU.entry(resource, EntryType.IN);
	}

	protected Entry enterHotData(String resource, String hotKey) throws BlockException {
		return SphU.entry(resource, EntryType.IN, 1, hotKey);
	}

	private String blockMessage(BlockException exception, String fallback) {
		if (exception instanceof ParamFlowException) {
			return hotDataBlockMessage();
		}
		if (exception instanceof FlowException) {
			return fallbackBlockMessage();
		}
		return fallback;
	}

	private static String hotDataBlockMessage() {
		return I18nMessageResolver.resolveKey("fw.boot.sentinel.hotDataBlocked");
	}

	private static String fallbackBlockMessage() {
		return I18nMessageResolver.resolveKey("fw.boot.sentinel.fallbackBlocked");
	}

	@Override
	public Object preHandle(ApiInfo info, MagicScriptContext context,
							MagicHttpServletRequest request, MagicHttpServletResponse response) throws Exception {
		return preHandle(requestEntity(info, context, request, response));
	}

	@Override
	public void afterCompletion(RequestEntity requestEntity, Object returnValue, Throwable throwable) {
		MagicScriptContext context = requestEntity == null ? null : requestEntity.getMagicScriptContext();
		Object value = context == null ? null : context.get(ENTRY_CONTEXT_KEY);
		if (!(value instanceof List)) {
			return;
		}
		try {
			release((List<?>) value);
		} finally {
			context.set(ENTRY_CONTEXT_KEY, null);
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
				.parameters(Collections.emptyMap())
				.pathVariables(Collections.emptyMap())
				.setHeaders(Collections.emptyMap())
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

	private void release(List<?> entries) {
		for (int i = entries.size() - 1; i >= 0; i--) {
			Object value = entries.get(i);
			if (!(value instanceof EntryHolder)) {
				continue;
			}
			try {
				((EntryHolder) value).exit();
			} catch (Exception e) {
				log.warn("release magic-api sentinel entry failed", e);
			}
		}
	}

	private Object failure(RequestEntity requestEntity, String message) {
		Object result = resultProvider.buildResult(requestEntity, Constants.RESPONSE_CODE_INVALID, message, null);
		if (result instanceof JsonBean) {
			((JsonBean<?>) result).setSuccess(false);
		}
		return result;
	}

	private static class EntryHolder {

		private final Entry entry;
		private final Object[] args;

		private EntryHolder(Entry entry, Object... args) {
			this.entry = entry;
			this.args = args;
		}

		private static EntryHolder fallback(Entry entry) {
			return new EntryHolder(entry);
		}

		private static EntryHolder hotData(Entry entry, Object hotKey) {
			return new EntryHolder(entry, hotKey);
		}

		private void exit() {
			if (args == null || args.length == 0) {
				entry.exit();
				return;
			}
			entry.exit(1, args);
		}
	}
}
