package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.resource.model;

import lombok.Data;
import org.ssssssss.magicapi.core.model.MagicEntity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
public class MagicMcpPromptInfo extends MagicEntity {

	private String name;

	private String description;

	private boolean enabled = true;

	private List<MagicMcpPromptMessage.Argument> arguments = new ArrayList<MagicMcpPromptMessage.Argument>();

	private List<MagicMcpPromptMessage> messages = new ArrayList<MagicMcpPromptMessage>();

	/**
	 * 补全候选：key 为参数名，value 为候选值；注册为 MCP {@code completion/complete}。
	 */
	private Map<String, List<String>> completions = new LinkedHashMap<>();
}
