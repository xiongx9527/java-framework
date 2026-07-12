package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.model;

import lombok.Data;
import org.ssssssss.magicapi.core.model.MagicEntity;
import org.ssssssss.magicapi.core.model.PathMagicEntity;

@Data
public class McpPromptInfo extends PathMagicEntity {

	private String description;

	private String argumentsJson;

	private String messagesJson;

	private String annotationsJson;

	@Override
	public MagicEntity simple() {
		McpPromptInfo info = new McpPromptInfo();
		super.simple(info);
		copyMcpFields(info);
		return info;
	}

	@Override
	public McpPromptInfo copy() {
		McpPromptInfo info = new McpPromptInfo();
		super.copyTo(info);
		copyMcpFields(info);
		return info;
	}

	private void copyMcpFields(McpPromptInfo info) {
		info.setDescription(this.description);
		info.setArgumentsJson(this.argumentsJson);
		info.setMessagesJson(this.messagesJson);
		info.setAnnotationsJson(this.annotationsJson);
	}
}
