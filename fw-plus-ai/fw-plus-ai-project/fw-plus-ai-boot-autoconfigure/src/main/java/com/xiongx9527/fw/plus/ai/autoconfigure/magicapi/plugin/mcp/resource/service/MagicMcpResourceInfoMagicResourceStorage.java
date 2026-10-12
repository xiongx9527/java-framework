package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.resource.service;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.resource.model.MagicMcpResourceInfo;
import org.ssssssss.magicapi.core.model.MagicEntity;
import org.ssssssss.magicapi.core.resource.Resource;
import org.ssssssss.magicapi.core.service.MagicResourceService;
import org.ssssssss.magicapi.core.service.MagicResourceStorage;
import org.ssssssss.magicapi.utils.JsonUtils;

import java.util.Objects;

/**
 * {@code mcp-resource} 资源类型存储：JSON 定义 uri/mimeType/name，供 4.2 注册为 MCP resource。
 */
public class MagicMcpResourceInfoMagicResourceStorage implements MagicResourceStorage<MagicMcpResourceInfo> {

	private MagicResourceService magicResourceService;

	@Override
	public String folder() {
		return "mcp-resource";
	}

	@Override
	public String suffix() {
		return ".json";
	}

	@Override
	public Class<MagicMcpResourceInfo> magicClass() {
		return MagicMcpResourceInfo.class;
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
	public String buildMappingKey(MagicMcpResourceInfo info) {
		return String.format("%s-%s", info.getUri(), info.getUpdateTime());
	}

	@Override
	public void validate(MagicMcpResourceInfo entity) {
		validateRequired(entity.getUri(), "uri");
		validateRequired(entity.getName(), "name");
		validateUri(entity.getUri());
		validateMimeType(entity.getMimeType());
		validateUriUnique(entity.getUri(), entity.getId());
		if (entity.getId() != null && magicResourceService != null) {
			MagicMcpResourceInfo old = magicResourceService.file(entity.getId());
			if (old != null && !Objects.equals(old.getUri(), entity.getUri())) {
				throw new IllegalArgumentException("uri不可修改");
			}
		}
	}

	@Override
	public void setMagicResourceService(MagicResourceService magicResourceService) {
		this.magicResourceService = magicResourceService;
	}

	@Override
	public MagicMcpResourceInfo read(byte[] bytes) {
		return JsonUtils.readValue(bytes, MagicMcpResourceInfo.class);
	}

	@Override
	public byte[] write(MagicEntity entity) {
		return JsonUtils.toJsonBytes(entity);
	}

	@Override
	public MagicMcpResourceInfo readResource(Resource resource) {
		return read(resource.read());
	}

	private void validateRequired(String value, String field) {
		if (value == null || value.trim().length() == 0) {
			throw new IllegalArgumentException(field + "不能为空");
		}
	}

	private void validateUri(String uri) {
		if (uri.indexOf(' ') >= 0) {
			throw new IllegalArgumentException("uri不能包含空格");
		}
		if (!uri.startsWith("/") && !uri.contains("://")) {
			throw new IllegalArgumentException("uri需为绝对路径或带协议的地址");
		}
	}

	private void validateMimeType(String mimeType) {
		if (mimeType == null || mimeType.trim().length() == 0) {
			return;
		}
		if (!mimeType.matches("^[\\w.+-]+/[\\w.+-]+$")) {
			throw new IllegalArgumentException("mimeType格式非法，应形如 text/plain");
		}
	}

	private void validateUriUnique(String uri, String currentId) {
		if (uri == null || magicResourceService == null) {
			return;
		}
		boolean noneMatchUri = magicResourceService.listFiles(folder() + ":0").stream()
				.map(it -> (MagicMcpResourceInfo) it)
				.filter(it -> !Objects.equals(it.getId(), currentId))
				.noneMatch(it -> Objects.equals(it.getUri(), uri));
		if (!noneMatchUri) {
			throw new IllegalArgumentException(String.format("uri '%s' 已存在，请使用其他 uri", uri));
		}
	}
}
