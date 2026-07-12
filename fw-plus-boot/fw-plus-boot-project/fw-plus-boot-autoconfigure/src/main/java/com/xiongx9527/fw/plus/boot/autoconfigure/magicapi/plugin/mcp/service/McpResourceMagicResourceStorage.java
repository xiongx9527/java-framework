package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.service;

import com.alibaba.fastjson2.JSON;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.common.MagicBaseService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.model.McpResourceInfo;
import org.apache.commons.lang3.StringUtils;
import org.ssssssss.magicapi.core.service.AbstractPathMagicResourceStorage;

import java.net.URI;
import java.util.List;

public class McpResourceMagicResourceStorage extends AbstractPathMagicResourceStorage<McpResourceInfo> implements MagicBaseService {

	public static final String FOLDER = "mcp-resource";

	@Override
	public String folder() {
		return FOLDER;
	}

	@Override
	public Class<McpResourceInfo> magicClass() {
		return McpResourceInfo.class;
	}

	@Override
	public void validate(McpResourceInfo entity) {
		validateRequiredField(entity.getName(), "name");
		validateResourceUri(entity.getUri());
		validateMimeType(entity.getMimeType());
		validateJsonObject(entity.getAnnotationsJson(), "annotationsJson");
	}

	public List<McpResourceInfo> findAll() {
		return magicResourceService.files(folder());
	}

	@Override
	public String buildMappingKey(McpResourceInfo info) {
		return buildMappingKey(info, magicResourceService.getGroupPath(info.getGroupId()));
	}

	private void validateResourceUri(String uri) {
		validateRequiredField(uri, "uri");
		try {
			URI parsed = URI.create(uri);
			if (!"mcp".equals(parsed.getScheme()) || StringUtils.isBlank(parsed.getHost())) {
				throw new IllegalArgumentException();
			}
		} catch (Exception e) {
			throw new IllegalArgumentException("uri必须是有效的mcp://资源地址", e);
		}
	}

	private void validateMimeType(String mimeType) {
		if (StringUtils.isNotBlank(mimeType) && !mimeType.contains("/")) {
			throw new IllegalArgumentException("mimeType格式不正确");
		}
	}

	private void validateJsonObject(String value, String fieldName) {
		if (StringUtils.isNotBlank(value) && !(JSON.parse(value) instanceof com.alibaba.fastjson2.JSONObject)) {
			throw new IllegalArgumentException(fieldName + "不是有效的JSON对象");
		}
	}
}
