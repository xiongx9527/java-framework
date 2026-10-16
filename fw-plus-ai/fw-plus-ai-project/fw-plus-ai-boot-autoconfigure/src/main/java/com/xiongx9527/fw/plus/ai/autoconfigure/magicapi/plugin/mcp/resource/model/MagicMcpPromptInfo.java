package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.resource.model;

import lombok.Data;
import org.ssssssss.magicapi.core.model.MagicEntity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 提示词定义。名称复用父类 {@link MagicEntity#name}，勿声明同名字段遮蔽（否则 simple() 节点
 * name 恒为 null，控制台资源树渲染会中断）。
 */
@Data
public class MagicMcpPromptInfo extends MagicEntity {

	private String description;

	private boolean enabled = true;

	private List<MagicMcpPromptMessage.Argument> arguments = new ArrayList<MagicMcpPromptMessage.Argument>();

	private List<MagicMcpPromptMessage> messages = new ArrayList<MagicMcpPromptMessage>();

	/**
	 * 补全候选：key 为参数名，value 为候选值；注册为 MCP {@code completion/complete}。
	 */
	private Map<String, List<String>> completions = new LinkedHashMap<>();
}
