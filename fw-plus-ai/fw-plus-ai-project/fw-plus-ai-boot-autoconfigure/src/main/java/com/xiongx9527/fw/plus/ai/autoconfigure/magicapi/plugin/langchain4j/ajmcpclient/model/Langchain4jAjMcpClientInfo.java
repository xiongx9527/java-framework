package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.ajmcpclient.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ssssssss.magicapi.core.model.MagicEntity;

import java.util.List;
import java.util.Map;

/**
 * {@code langchain4j-aj-mcp-client} 数据源资源定义：基于 aj-mcp（Java 8 兼容 SDK）连接一个
 * 外部 MCP Server，把它的工具暴露成 LangChain4j {@code ToolProvider}，脚本用
 * {@code langchain4jAjMcpClient['key']} 取用后挂到 {@code AssistantBuilder.toolProvider(...)}。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class Langchain4jAjMcpClientInfo extends MagicEntity {

	/** 传输类型：http-sse（旧版 HTTP+SSE，默认） / streamable-http / stdio（本地子进程） */
	private String transport;

	/** 实例唯一 key，脚本按 key 取 ToolProvider */
	private String key;

	/** 展示名，缺省取 key */
	private String name;

	/** 是否启用；停用时销毁实例且不注册（下线而非删除） */
	private boolean enabled = true;

	/** http 系传输：MCP Server 地址（http-sse 为 SSE 端点，streamable-http 为消息端点） */
	private String url;

	/** stdio 传输：启动命令（如 ["java","-jar","mcp-server.jar"]） */
	private List<String> command;

	/** stdio 传输：子进程环境变量 */
	private Map<String, String> environment;

	/** http 系传输：自定义请求头，认证用（如 Authorization: Bearer xxx） */
	private Map<String, String> customHeaders;

	/** 认证失败（如 api-key 过期，401/403 等）时的自动重试次数，缺省 0（不重试） */
	private Integer authRetryCount;
}
