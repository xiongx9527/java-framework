package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.mcpclient.resource;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.mcpclient.model.Langchain4jMcpClientInfo;
import org.ssssssss.magicapi.core.model.MagicEntity;
import org.ssssssss.magicapi.core.resource.Resource;
import org.ssssssss.magicapi.core.service.MagicResourceService;
import org.ssssssss.magicapi.core.service.MagicResourceStorage;
import org.ssssssss.magicapi.utils.JsonUtils;

import java.util.Objects;

/**
 * {@code langchain4j-mcp-client} 资源类型存储：每份 JSON 定义一个外部 MCP Server 连接。
 */
public class Langchain4jMcpClientMagicResourceStorage implements MagicResourceStorage<Langchain4jMcpClientInfo> {

	private MagicResourceService magicResourceService;

	@Override
	public String folder() {
		return "langchain4j-mcp-client";
	}

	@Override
	public String suffix() {
		return ".json";
	}

	@Override
	public Class<Langchain4jMcpClientInfo> magicClass() {
		return Langchain4jMcpClientInfo.class;
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
	public String buildMappingKey(Langchain4jMcpClientInfo info) {
		return String.format("%s-%s", info.getKey(), info.getUpdateTime());
	}

	@Override
	public void validate(Langchain4jMcpClientInfo entity) {
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
	public Langchain4jMcpClientInfo read(byte[] bytes) {
		return JsonUtils.readValue(bytes, Langchain4jMcpClientInfo.class);
	}

	@Override
	public byte[] write(MagicEntity entity) {
		return JsonUtils.toJsonBytes(entity);
	}

	@Override
	public Langchain4jMcpClientInfo readResource(Resource resource) {
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
				.map(it -> (Langchain4jMcpClientInfo) it)
				.filter(it -> !Objects.equals(it.getId(), currentId))
				.noneMatch(it -> Objects.equals(it.getKey(), key));
		if (!noneMatchKey) {
			throw new IllegalArgumentException(String.format("key '%s' 已存在，请使用其他 key", key));
		}
	}
}
