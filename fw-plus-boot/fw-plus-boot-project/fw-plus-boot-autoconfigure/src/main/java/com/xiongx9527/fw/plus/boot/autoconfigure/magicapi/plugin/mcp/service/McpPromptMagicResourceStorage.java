package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.common.MagicBaseService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.model.McpPromptInfo;
import org.apache.commons.lang3.StringUtils;
import org.ssssssss.magicapi.core.service.AbstractPathMagicResourceStorage;

import java.util.List;

public class McpPromptMagicResourceStorage extends AbstractPathMagicResourceStorage<McpPromptInfo> implements MagicBaseService {

	public static final String FOLDER = "mcp-prompt";

	@Override
	public String folder() {
		return FOLDER;
	}

	@Override
	public Class<McpPromptInfo> magicClass() {
		return McpPromptInfo.class;
	}

	@Override
	public void validate(McpPromptInfo entity) {
		validateRequiredField(entity.getName(), "name");
		validateArguments(entity.getArgumentsJson());
		validateMessages(entity.getMessagesJson());
		validateJsonObject(entity.getAnnotationsJson(), "annotationsJson");
	}

	public List<McpPromptInfo> findAll() {
		return magicResourceService.files(folder());
	}

	@Override
	public String buildMappingKey(McpPromptInfo info) {
		return buildMappingKey(info, magicResourceService.getGroupPath(info.getGroupId()));
	}

	private void validateArguments(String value) {
		if (StringUtils.isBlank(value)) {
			return;
		}
		Object parsed = JSON.parse(value);
		if (!(parsed instanceof JSONArray)) {
			throw new IllegalArgumentException("argumentsJson不是有效的JSON数组");
		}
		for (Object item : (JSONArray) parsed) {
			if (!(item instanceof JSONObject) || StringUtils.isBlank(((JSONObject) item).getString("name"))) {
				throw new IllegalArgumentException("argumentsJson参数name不能为空");
			}
		}
	}

	private void validateMessages(String value) {
		if (StringUtils.isBlank(value)) {
			return;
		}
		Object parsed = JSON.parse(value);
		if (!(parsed instanceof JSONArray)) {
			throw new IllegalArgumentException("messagesJson不是有效的JSON数组");
		}
		for (Object item : (JSONArray) parsed) {
			if (!(item instanceof JSONObject)) {
				throw new IllegalArgumentException("messagesJson消息格式不正确");
			}
			JSONObject message = (JSONObject) item;
			if (StringUtils.isBlank(message.getString("role")) || message.get("content") == null) {
				throw new IllegalArgumentException("messagesJson消息role和content不能为空");
			}
		}
	}

	private void validateJsonObject(String value, String fieldName) {
		if (StringUtils.isNotBlank(value) && !(JSON.parse(value) instanceof JSONObject)) {
			throw new IllegalArgumentException(fieldName + "不是有效的JSON对象");
		}
	}
}
