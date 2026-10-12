package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.model;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.config.MagicApiMcpProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ssssssss.magicapi.core.model.MagicEntity;

/**
 * MCP server 实例定义：每份定义对应一个独立 server（独立 endpoint 与工具过滤）。
 *
 * <p>名称字段复用父类 {@link MagicEntity#name}，勿声明同名字段遮蔽父类。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MagicMcpServerInfo extends MagicEntity {

	/**
	 * 实例唯一 key。
	 */
	private String key;

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

	/**
	 * 认证 provider 与自定义实现选择；不包含认证凭据。
	 */
	private MagicApiMcpProperties.Auth auth;

	/**
	 * 资源树列表节点：补齐 key/transport/endpoint/enabled。
	 *
	 * <p>server 名称复用父类 {@link MagicEntity#name}，勿再声明同名字段遮蔽（Lombok {@code @Data}
	 * 会生成覆盖父类的 getName/setName，JSON 反序列化写入子类字段而父类字段恒为 null，
	 * {@code simple()} 拷出的节点 name 为 null，控制台资源树搜索过滤 {@code S.name.indexOf(...)}
	 * 会抛错、面板不再渲染）。</p>
	 */
	@Override
	public MagicEntity simple() {
		MagicMcpServerInfo info = new MagicMcpServerInfo();
		super.simple(info);
		info.setKey(this.key);
		info.setTransport(this.transport);
		info.setEndpoint(this.endpoint);
		info.setEnabled(this.enabled);
		return info;
	}
}
