package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.provider;

import com.alibaba.fastjson2.JSON;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.model.McpResourceInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.service.McpResourceMagicResourceStorage;
import org.apache.commons.lang3.StringUtils;
import org.ssssssss.magicapi.core.service.MagicResourceService;

import java.util.*;
import java.util.function.Supplier;

public class MagicApiMcpResourceProvider implements McpResourceProvider {

	private final Supplier<List<McpResourceInfo>> resourceSupplier;

	public MagicApiMcpResourceProvider(MagicResourceService magicResourceService) {
		this(() -> magicResourceService.files(McpResourceMagicResourceStorage.FOLDER));
	}

	public MagicApiMcpResourceProvider(Supplier<List<McpResourceInfo>> resourceSupplier) {
		this.resourceSupplier = resourceSupplier;
	}

	@Override
	public McpPage<McpResourceDefinition> listResources(McpProviderContext context, McpPageRequest pageRequest) {
		List<McpResourceDefinition> definitions = new ArrayList<>();
		for (McpResourceInfo info : resources()) {
			definitions.add(toDefinition(info));
		}
		return McpPage.of(definitions, null);
	}

	@Override
	public McpResourceReadResult readResource(McpProviderContext context, McpResourceReadRequest request) {
		if (request == null || request.getUri() == null) {
			return null;
		}
		for (McpResourceInfo info : resources()) {
			if (request.getUri().equals(info.getUri())) {
				McpResourceReadResult result = new McpResourceReadResult();
				Map<String, Object> content = new LinkedHashMap<>();
				content.put("uri", info.getUri());
				if (StringUtils.isNotBlank(info.getMimeType())) {
					content.put("mimeType", info.getMimeType());
				}
				content.put("text", info.getScript() == null ? "" : info.getScript());
				result.setContents(Collections.singletonList(content));
				return result;
			}
		}
		return null;
	}

	@Override
	public McpPage<McpResourceTemplate> listTemplates(McpProviderContext context, McpPageRequest pageRequest) {
		return McpPage.of(Collections.<McpResourceTemplate>emptyList(), null);
	}

	private McpResourceDefinition toDefinition(McpResourceInfo info) {
		McpResourceDefinition definition = new McpResourceDefinition();
		definition.setUri(info.getUri());
		definition.setName(info.getName());
		definition.setDescription(info.getDescription());
		definition.setMimeType(info.getMimeType());
		definition.setAnnotations(parseObject(info.getAnnotationsJson()));
		return definition;
	}

	private List<McpResourceInfo> resources() {
		List<McpResourceInfo> resources = resourceSupplier.get();
		return resources == null ? Collections.<McpResourceInfo>emptyList() : resources;
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> parseObject(String json) {
		if (StringUtils.isBlank(json)) {
			return null;
		}
		return JSON.parseObject(json, Map.class);
	}
}
