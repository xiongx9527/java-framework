package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.resource.service;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.resource.model.MagicMcpPromptInfo;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.resource.model.MagicMcpPromptMessage;
import org.ssssssss.magicapi.core.model.MagicEntity;
import org.ssssssss.magicapi.core.resource.Resource;
import org.ssssssss.magicapi.core.service.MagicResourceService;
import org.ssssssss.magicapi.core.service.MagicResourceStorage;
import org.ssssssss.magicapi.utils.JsonUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * {@code mcp-prompt} 资源类型存储：JSON 定义 name/arguments/messages，供 4.2 注册为 MCP prompt。
 */
public class MagicMcpPromptInfoMagicResourceStorage implements MagicResourceStorage<MagicMcpPromptInfo> {

	private static final Set<String> ROLES = new HashSet<String>() {
		{
			add("user");
			add("assistant");
		}
	};

	private MagicResourceService magicResourceService;

	@Override
	public String folder() {
		return "mcp-prompt";
	}

	@Override
	public String suffix() {
		return ".json";
	}

	@Override
	public Class<MagicMcpPromptInfo> magicClass() {
		return MagicMcpPromptInfo.class;
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
	public String buildMappingKey(MagicMcpPromptInfo info) {
		return String.format("%s-%s", info.getName(), info.getUpdateTime());
	}

	@Override
	public void validate(MagicMcpPromptInfo entity) {
		validateRequired(entity.getName(), "name");
		validateNameUnique(entity.getName(), entity.getId());
		validateArguments(entity.getArguments());
		validateMessages(entity.getMessages());
	}

	@Override
	public void setMagicResourceService(MagicResourceService magicResourceService) {
		this.magicResourceService = magicResourceService;
	}

	@Override
	public MagicMcpPromptInfo read(byte[] bytes) {
		return JsonUtils.readValue(bytes, MagicMcpPromptInfo.class);
	}

	@Override
	public byte[] write(MagicEntity entity) {
		return JsonUtils.toJsonBytes(entity);
	}

	@Override
	public MagicMcpPromptInfo readResource(Resource resource) {
		return read(resource.read());
	}

	private void validateRequired(String value, String field) {
		if (value == null || value.trim().length() == 0) {
			throw new IllegalArgumentException(field + "不能为空");
		}
	}

	private void validateArguments(List<MagicMcpPromptMessage.Argument> arguments) {
		if (arguments == null) {
			return;
		}
		Set<String> names = new HashSet<String>();
		for (MagicMcpPromptMessage.Argument argument : arguments) {
			if (argument == null) {
				continue;
			}
			validateRequired(argument.getName(), "arguments.name");
			if (!names.add(argument.getName())) {
				throw new IllegalArgumentException(String.format("参数名 '%s' 重复", argument.getName()));
			}
		}
	}

	private void validateMessages(List<MagicMcpPromptMessage> messages) {
		if (messages == null || messages.isEmpty()) {
			throw new IllegalArgumentException("messages不能为空");
		}
		for (MagicMcpPromptMessage message : messages) {
			if (message == null || message.getContent() == null || message.getContent().trim().length() == 0) {
				throw new IllegalArgumentException("messages.content不能为空");
			}
			if (message.getRole() == null || !ROLES.contains(message.getRole())) {
				throw new IllegalArgumentException("messages.role仅支持 user 或 assistant");
			}
		}
	}

	private void validateNameUnique(String name, String currentId) {
		if (name == null || magicResourceService == null) {
			return;
		}
		boolean noneMatchName = magicResourceService.listFiles(folder() + ":0").stream()
				.map(it -> (MagicMcpPromptInfo) it)
				.filter(it -> !Objects.equals(it.getId(), currentId))
				.noneMatch(it -> Objects.equals(it.getName(), name));
		if (!noneMatchName) {
			throw new IllegalArgumentException(String.format("提示词名称 '%s' 已存在，请使用其他名称", name));
		}
	}
}
