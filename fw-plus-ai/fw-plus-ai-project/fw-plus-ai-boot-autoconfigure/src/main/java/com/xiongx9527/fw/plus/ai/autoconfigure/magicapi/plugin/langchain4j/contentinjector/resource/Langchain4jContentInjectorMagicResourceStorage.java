package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.contentinjector.resource;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.contentinjector.model.Langchain4jContentInjectorInfo;
import org.ssssssss.magicapi.core.service.AbstractPathMagicResourceStorage;

/**
 * {@code langchain4j-content-injector} 资源类型存储：路径型 {@code .ms} 资源，每份脚本即一个内容注入器。
 */
public class Langchain4jContentInjectorMagicResourceStorage
		extends AbstractPathMagicResourceStorage<Langchain4jContentInjectorInfo> {

	@Override
	public String folder() {
		return "langchain4j-content-injector";
	}

	@Override
	public Class<Langchain4jContentInjectorInfo> magicClass() {
		return Langchain4jContentInjectorInfo.class;
	}

	@Override
	public String buildMappingKey(Langchain4jContentInjectorInfo info) {
		return buildMappingKey(info, magicResourceService.getGroupPath(info.getGroupId()));
	}
}
