package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.mcpclient.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ssssssss.magicapi.core.model.MagicEntity;

import java.util.List;
import java.util.Map;

/**
 * {@code langchain4j-mcp-client} 数据源资源定义：连接一个外部 MCP Server，
 * 把它的工具暴露成一个 LangChain4j {@code ToolProvider}，脚本用
 * {@code langchain4jMcpClient['key']} 取用后挂到 {@code AssistantBuilder.toolProvider(...)}。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class Langchain4jMcpClientInfo extends MagicEntity {

	/** 传输类型：http（Streamable HTTP，默认） / stdio（本地子进程） / docker（容器内子进程） */
	private String transport;

	/** 实例唯一 key，脚本按 key 取 ToolProvider */
	private String key;

	/** 展示名，缺省取 key */
	private String name;

	/** 是否启用；停用时销毁实例且不注册（下线而非删除） */
	private boolean enabled = true;

	/** http 传输：MCP Server 地址（如 http://host:3000/mcp） */
	private String url;

	/** http 传输：自定义请求头，认证用（如 Authorization: Bearer xxx） */
	private Map<String, String> customHeaders;

	/**
	 * http 传输：动态请求头函数（magic-api function 名），每次请求调用一次拿最新头，
	 * 适合先调 token 接口换 api-key 的场景；返回的 Map 覆盖 {@link #customHeaders} 同名 key，
	 * 函数调用失败时回落 {@link #customHeaders}。
	 */
	private String customHeadersFunction;

	/** stdio/docker 传输：启动命令（如 ["npx","-y","mcp-server-xx"]） */
	private List<String> command;

	/** stdio/docker 传输：子进程环境变量 */
	private Map<String, String> environment;

	/** docker 传输：镜像名 */
	private String image;

	/** 客户端名（MCP initialize 时上报），缺省 magic-langchain4j */
	private String clientName;

	/** 仅暴露这些工具（缺省全部） */
	private List<String> filterToolNames;

	/** 单个 MCP Server 失败是否让整次工具装配失败；缺省 false（跳过该路） */
	private Boolean failIfOneServerFails;
}
