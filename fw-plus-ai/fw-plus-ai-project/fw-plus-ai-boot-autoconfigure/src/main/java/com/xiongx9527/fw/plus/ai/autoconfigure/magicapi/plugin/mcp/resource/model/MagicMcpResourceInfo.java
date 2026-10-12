package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.resource.model;

import lombok.Data;
import org.ssssssss.magicapi.core.model.MagicEntity;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * MCP 资源定义。名称复用父类 {@link MagicEntity#name}，勿声明同名字段遮蔽（否则 simple() 节点
 * name 恒为 null，控制台资源树渲染会中断）。
 */
@Data
public class MagicMcpResourceInfo extends MagicEntity {

	private String uri;

	private String mimeType = "text/plain";

	private String description;

	/**
	 * 资源内容：{@code resources/read} 直接返回的文本，缺省为空串。
	 */
	private String content;

	/**
	 * RFC 6570 资源模板（如 {@code file:///{path}}）：非空时额外注册为 MCP resource template，
	 * {@code resources/read} 可按模板匹配读取并支持路径变量。
	 */
	private String template;

	/**
	 * 补全候选：key 为参数名（模板变量或提示词参数），value 为候选值。
	 */
	private java.util.Map<String, java.util.List<String>> completions = new java.util.LinkedHashMap<>();

	private boolean enabled = true;

	private Map<String, Object> metadata = new LinkedHashMap<String, Object>();
}
