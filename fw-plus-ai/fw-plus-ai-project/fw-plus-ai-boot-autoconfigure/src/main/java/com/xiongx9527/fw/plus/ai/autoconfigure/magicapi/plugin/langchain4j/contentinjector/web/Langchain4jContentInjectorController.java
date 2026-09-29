package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.contentinjector.web;

import com.alibaba.fastjson2.JSON;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.contentinjector.model.Langchain4jContentInjectorInfo;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.contentinjector.module.Langchain4jContentInjectorModule;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.ssssssss.magicapi.core.config.MagicConfiguration;
import org.ssssssss.magicapi.core.config.WebSocketSessionManager;
import org.ssssssss.magicapi.core.logging.MagicLoggerContext;
import org.ssssssss.magicapi.core.model.DebugRequest;
import org.ssssssss.magicapi.core.model.JsonBean;
import org.ssssssss.magicapi.core.servlet.MagicHttpServletRequest;
import org.ssssssss.magicapi.core.web.MagicController;
import org.ssssssss.magicapi.core.web.MagicExceptionHandler;
import org.ssssssss.script.MagicScriptDebugContext;

import java.util.Collections;
import java.util.Map;

/**
 * 内容注入器执行测试：按资源 id 以 debug 方式执行脚本（支持控制台断点与日志），
 * 注入检索结果模板变量与测试参数后返回注入结果。
 *
 * <p>脚本可直接使用：{@code contents}（默认空列表）、{@code texts}（默认空列表）、
 * {@code chatMessage} / {@code query}（默认 null）；资源上的测试参数（{@code params}）后注入，
 * 可覆盖它们，例如 {@code {"query":"杭州天气怎么样","texts":["西湖很适合傍晚散步"]}}。</p>
 */
public class Langchain4jContentInjectorController extends MagicController implements MagicExceptionHandler {

	private final Langchain4jContentInjectorModule module;

	public Langchain4jContentInjectorController(MagicConfiguration configuration,
			Langchain4jContentInjectorModule module) {
		super(configuration);
		this.module = module;
	}

	@PostMapping("/langchain4j/content-injector/execute")
	@ResponseBody
	public JsonBean<Object> execute(String id, MagicHttpServletRequest request) {
		Langchain4jContentInjectorInfo entity = MagicConfiguration.getMagicResourceService().file(id);
		notNull(entity, FILE_NOT_FOUND);

		String script = entity.getScript();
		if (script == null || script.isEmpty()) {
			throw new IllegalArgumentException("Script content is empty or null.");
		}

		DebugRequest debugRequest = DebugRequest.create(request);
		String clientId = debugRequest.getRequestedClientId();
		String scriptId = debugRequest.getRequestedScriptId();

		if (clientId == null || scriptId == null) {
			throw new IllegalArgumentException("Client ID or Script ID is missing in the debug request.");
		}

		String sessionAndScriptId = clientId + scriptId;
		MagicLoggerContext.SESSION.set(clientId);

		MagicScriptDebugContext magicScriptContext = null;
		try {
			magicScriptContext = debugRequest.createMagicScriptContext(configuration.getDebugTimeout());
			WebSocketSessionManager.addMagicScriptContext(sessionAndScriptId, magicScriptContext);

			// 与注入器执行同一套上下文组装：contents/texts/chatMessage/query + 测试参数
			Object result = module.execute(entity, Collections.emptyList(), null,
					parseParams(entity.getParams()), magicScriptContext);
			return new JsonBean<>(result);
		} finally {
			if (magicScriptContext != null) {
				WebSocketSessionManager.removeMagicScriptContext(sessionAndScriptId);
			}
			MagicLoggerContext.SESSION.remove();
		}
	}

	/** 解析测试参数（JSON 对象字符串）为 Map；为空或非法时返回空 Map。 */
	private Map<String, Object> parseParams(String params) {
		if (StringUtils.isBlank(params)) {
			return Collections.emptyMap();
		}
		try {
			Map<String, Object> map = JSON.parseObject(params, Map.class);
			return map == null ? Collections.emptyMap() : map;
		} catch (Exception e) {
			throw new IllegalArgumentException("测试参数不是有效的 JSON 对象: " + params, e);
		}
	}
}
