package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.agenticscript.resource;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.agenticscript.model.Langchain4jAgenticScriptInfo;
import org.ssssssss.magicapi.core.service.AbstractPathMagicResourceStorage;

/**
 * {@code langchain4j-agentic-script} 资源类型存储：路径型 {@code .ms} 资源，
 * 每份脚本即一个 agentic 编排节点/工具（type 字段区分形态）。
 */
public class Langchain4jAgenticScriptMagicResourceStorage
		extends AbstractPathMagicResourceStorage<Langchain4jAgenticScriptInfo> {

	@Override
	public String folder() {
		return "langchain4j-agentic-script";
	}

	@Override
	public Class<Langchain4jAgenticScriptInfo> magicClass() {
		return Langchain4jAgenticScriptInfo.class;
	}

	@Override
	public String buildMappingKey(Langchain4jAgenticScriptInfo info) {
		return buildMappingKey(info, magicResourceService.getGroupPath(info.getGroupId()));
	}
}
