package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.systemmessage.module;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.systemmessage.model.Langchain4jSystemMessageInfo;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.systemmessage.registry.Langchain4jSystemMessageMagicDynamicRegistry;
import org.ssssssss.magicapi.core.annotation.MagicModule;
import org.ssssssss.magicapi.core.service.MagicResourceService;
import org.ssssssss.magicapi.utils.ScriptManager;
import org.ssssssss.script.MagicScriptContext;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * 系统提示词脚本模块：把 {@code langchain4j-system-message} 资源暴露为
 * {@code Function<Object, String>}，供 {@code AiServices.systemMessageProvider(...)} 装配。
 *
 * <p>用法：{@code langchain4jSystemMessageProvider.get('/system/assistant')} 返回
 * {@code Function<Object, String>}（{@code memoryId -> 系统提示词文本}）。</p>
 */
@MagicModule("langchain4jSystemMessageProvider")
public class Langchain4jSystemMessageProviderModule {

	private final Langchain4jSystemMessageMagicDynamicRegistry registry;

	private final MagicResourceService magicResourceService;

	public Langchain4jSystemMessageProviderModule(Langchain4jSystemMessageMagicDynamicRegistry registry,
			MagicResourceService magicResourceService) {
		this.registry = registry;
		this.magicResourceService = magicResourceService;
	}

	public String systemMessage(Object memoryId, String path, Map<String, Object> params) {
		Map<String, Object> newParams = new HashMap<>();
		if (null != params) {
			newParams.putAll(params);
		}
		newParams.put("memoryId", memoryId);
		Langchain4jSystemMessageInfo info = find(path);

		MagicScriptContext magicScriptContext = new MagicScriptContext();
		magicScriptContext.setScriptName(info.getPath());
		for (Map.Entry<String, Object> entry : newParams.entrySet()) {
			magicScriptContext.set(entry.getKey(), entry.getValue());
		}
		return (String)ScriptManager.executeScript(info.getScript(), magicScriptContext);
	}

	private Langchain4jSystemMessageInfo find(String path) {
		if (path == null || path.trim().isEmpty()) {
			throw new IllegalArgumentException("系统提示词路径不能为空");
		}
		String normalized = path.startsWith("/") ? path : "/" + path;
		Langchain4jSystemMessageInfo info = registry.getMapping(normalized);
		if (info == null) {
			info = magicResourceService.file(normalized);
		}
		return info;
	}

}
