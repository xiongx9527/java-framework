package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.toolprovider.resource;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.toolprovider.model.Langchain4jToolProviderInfo;
import org.ssssssss.magicapi.core.model.MagicEntity;
import org.ssssssss.magicapi.core.resource.Resource;
import org.ssssssss.magicapi.core.service.MagicResourceService;
import org.ssssssss.magicapi.core.service.MagicResourceStorage;
import org.ssssssss.magicapi.utils.JsonUtils;

import java.util.Objects;

/**
 * {@code langchain4j-tool-provider} 资源类型存储：每份 JSON 定义新增一个 ToolProvider 实例。
 */
public class Langchain4jToolProviderMagicResourceStorage implements MagicResourceStorage<Langchain4jToolProviderInfo> {

	private MagicResourceService magicResourceService;

	@Override
	public String folder() {
		return "langchain4j-tool-provider";
	}

	@Override
	public String suffix() {
		return ".json";
	}

	@Override
	public Class<Langchain4jToolProviderInfo> magicClass() {
		return Langchain4jToolProviderInfo.class;
	}

	@Override
	public boolean requirePath() {
		return false;
	}

	@Override
	public boolean requiredScript() {
		return false;
	}

	@Override
	public boolean allowRoot() {
		return true;
	}

	@Override
	public String buildMappingKey(Langchain4jToolProviderInfo info) {
		return String.format("%s-%s", info.getKey(), info.getUpdateTime());
	}

	@Override
	public void validate(Langchain4jToolProviderInfo entity) {
		validateRequired(entity.getKey(), "key");
		if (!entity.getKey().matches("^[\\w.-]+$")) {
			throw new IllegalArgumentException("key只能包含字母、数字、下划线、点与中划线");
		}
		validateKeyUnique(entity.getKey(), entity.getId());
	}

	@Override
	public void setMagicResourceService(MagicResourceService magicResourceService) {
		this.magicResourceService = magicResourceService;
	}

	@Override
	public Langchain4jToolProviderInfo read(byte[] bytes) {
		return JsonUtils.readValue(bytes, Langchain4jToolProviderInfo.class);
	}

	@Override
	public byte[] write(MagicEntity entity) {
		return JsonUtils.toJsonBytes(entity);
	}

	@Override
	public Langchain4jToolProviderInfo readResource(Resource resource) {
		return read(resource.read());
	}

	private void validateRequired(String value, String field) {
		if (value == null || value.trim().isEmpty()) {
			throw new IllegalArgumentException(field + "不能为空");
		}
	}

	private void validateKeyUnique(String key, String currentId) {
		if (key == null || magicResourceService == null) {
			return;
		}
		boolean noneMatchKey = magicResourceService.listFiles(folder() + ":0").stream()
				.map(it -> (Langchain4jToolProviderInfo) it)
				.filter(it -> !Objects.equals(it.getId(), currentId))
				.noneMatch(it -> Objects.equals(it.getKey(), key));
		if (!noneMatchKey) {
			throw new IllegalArgumentException(String.format("key '%s' 已存在，请使用其他 key", key));
		}
	}
}
