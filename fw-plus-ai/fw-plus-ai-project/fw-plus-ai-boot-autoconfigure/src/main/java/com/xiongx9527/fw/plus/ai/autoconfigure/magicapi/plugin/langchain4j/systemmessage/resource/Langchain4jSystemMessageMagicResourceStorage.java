package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.systemmessage.resource;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.systemmessage.model.Langchain4jSystemMessageInfo;
import org.ssssssss.magicapi.core.service.AbstractPathMagicResourceStorage;

/**
 * {@code langchain4j-system-message} 资源类型存储：路径型 {@code .ms} 资源，每份脚本即一条系统提示词。
 */
public class Langchain4jSystemMessageMagicResourceStorage
		extends AbstractPathMagicResourceStorage<Langchain4jSystemMessageInfo> {

	@Override
	public String folder() {
		return "langchain4j-system-message";
	}

	@Override
	public Class<Langchain4jSystemMessageInfo> magicClass() {
		return Langchain4jSystemMessageInfo.class;
	}

	@Override
	public String buildMappingKey(Langchain4jSystemMessageInfo info) {
		return buildMappingKey(info, magicResourceService.getGroupPath(info.getGroupId()));
	}
}
