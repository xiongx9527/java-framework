package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.agenticscript.web;

import com.alibaba.fastjson2.JSON;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.agenticscript.model.Langchain4jAgenticScriptInfo;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import org.apache.commons.lang3.StringUtils;
import org.bsc.langgraph4j.GraphInput;
import org.bsc.langgraph4j.RunnableConfig;
import org.bsc.langgraph4j.state.AgentState;
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
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * agentic 脚本节点执行测试：按资源 id 以 debug 方式执行脚本，注入测试参数与声明的
 * 输入变量（缺参补 null）后返回节点输出；type=graph 时构建并执行图（测试参数作为图输入），
 * 返回最终 state。
 */
public class Langchain4jAgenticScriptController extends MagicController implements MagicExceptionHandler {

	public Langchain4jAgenticScriptController(MagicConfiguration configuration) {
		super(configuration);
	}

	@PostMapping("/langchain4j/agentic-script/execute")
	@ResponseBody
	public JsonBean<Object> execute(String id, MagicHttpServletRequest request) {
		Langchain4jAgenticScriptInfo entity = MagicConfiguration.getMagicResourceService().file(id);
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

		Map<String, Object> params = parseParams(entity.getParams());

		// graph 形态：构建并执行图，测试参数作为图的执行输入，返回最终 state
		if ("graph".equals(entity.getType())) {
			try {
				return new JsonBean<>(executeGraph(entity, params));
			} finally {
				MagicLoggerContext.SESSION.remove();
			}
		}

		MagicScriptDebugContext magicScriptContext = null;
		try {
			magicScriptContext = debugRequest.createMagicScriptContext(configuration.getDebugTimeout());
			WebSocketSessionManager.addMagicScriptContext(sessionAndScriptId, magicScriptContext);
			magicScriptContext.setScriptName(MagicConfiguration.getMagicResourceService().getScriptName(entity));

			// 注入声明的输入变量（缺参补 null）与测试参数供脚本使用
			if (entity.getInputKeys() != null) {
				for (String key : entity.getInputKeys()) {
					if (key != null) {
						magicScriptContext.set(key, null);
					}
				}
			}
			for (Map.Entry<String, Object> entry : params.entrySet()) {
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

	/** 构建并执行图脚本：返回图的最终 state（图终止无输出时返回空 Map）。 */
	private Map<String, Object> executeGraph(Langchain4jAgenticScriptInfo entity, Map<String, Object> input) {
		org.bsc.langgraph4j.CompiledGraph<? extends AgentState> compiled = SpringUtils
				.getBean(com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.agenticscript.module.Langchain4jAgenticScriptModule.class)
				.compileGraph(entity);
		java.util.Optional<? extends AgentState> output = compiled
				.invoke(GraphInput.args(input == null ? Collections.emptyMap() : input), RunnableConfig.empty());
		Map<String, Object> result = new LinkedHashMap<>();
		if (output != null && output.isPresent() && output.get().data() != null) {
			result.putAll(output.get().data());
		}
		return result;
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
