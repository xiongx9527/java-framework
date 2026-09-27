package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.model;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.config.MagicApiMcpProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ssssssss.magicapi.core.model.MagicEntity;

/**
 * MCP server 实例定义：每份定义对应一个独立 server（独立 endpoint 与工具过滤）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MagicMcpServerInfo extends MagicEntity {

	/**
	 * 实例唯一 key。
	 */
	private String key;

	/**
	 * server 名称，缺省取 key。
	 */
	private String name;

	/**
	 * 传输协议：http-sse 或 streamable-http。
	 */
	private String transport = "http-sse";

	/**
	 * 服务端点，如 {@code /mcp/report}。
	 */
	private String endpoint;

	/**
	 * 是否启用；停用时 server 不注册路由。
	 */
	private boolean enabled = true;

	/**
	 * 该实例的工具过滤配置，缺省继承插件顶层配置。
	 */
	private MagicApiMcpProperties.Tools tools;
}
