package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.systemmessage.web;

import com.alibaba.fastjson2.JSON;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.systemmessage.model.Langchain4jSystemMessageInfo;
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
import org.ssssssss.magicapi.utils.ScriptManager;
import org.ssssssss.script.MagicScriptDebugContext;

import java.util.Collections;
import java.util.Map;

/**
 * 系统提示词执行测试：按资源 id 以 debug 方式执行脚本，注入测试参数后返回系统提示词结果。
 */
public class Langchain4jSystemMessageController extends MagicController implements MagicExceptionHandler {

	public Langchain4jSystemMessageController(MagicConfiguration configuration) {
		super(configuration);
	}

	@PostMapping("/langchain4j/system-message/execute")
	@ResponseBody
	public JsonBean<Object> execute(String id, MagicHttpServletRequest request) {
		Langchain4jSystemMessageInfo entity = MagicConfiguration.getMagicResourceService().file(id);
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
			magicScriptContext.setScriptName(MagicConfiguration.getMagicResourceService().getScriptName(entity));

			// 注入 memoryId 与测试参数供脚本使用
			magicScriptContext.set("memoryId", null);
			for (Map.Entry<String, Object> entry : parseParams(entity.getParams()).entrySet()) {
				magicScriptContext.set(entry.getKey(), entry.getValue());
			}

			Object result = ScriptManager.executeScript(script, magicScriptContext);
			return new JsonBean<>(result);
		} finally {
			if (magicScriptContext != null) {
				WebSocketSessionManager.removeMagicScriptContext(sessionAndScriptId);
			}
			MagicLoggerContext.SESSION.remove();
		}
	}

	/**
	 * 解析测试参数（JSON 对象字符串）为 Map；为空或非法时返回空 Map。
	 */
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
