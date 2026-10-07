package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.web;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.config.MagicApiMcpProperties;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.model.MagicDynamicMcpServer;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.model.MagicMcpServerInfo;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.service.MagicMcpServerInfoMagicResourceStorage;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.service.MagicMcpServerMagicDynamicRegistry;
import com.xiongx9527.fw.plus.auth.core.InstanceAuthRegistry;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.ListUtils;
import org.ssssssss.magicapi.core.config.MagicConfiguration;
import org.ssssssss.magicapi.core.model.JsonBean;
import org.ssssssss.magicapi.core.web.MagicController;
import org.ssssssss.magicapi.core.web.MagicExceptionHandler;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * MCP server 实例控制台接口：查看运行实例、连接校验与删除前校验。
 *
 * <p>{@code MagicDynamicMcpServer} / {@code MagicMcpServerMagicDynamicRegistry} 带
 * {@code @ConditionalOnBean(McpRuntimeFactory.class)}，未装配 MCP server runtime 时（样例
 * 或不引入 mcp runtime 依赖的场景）这两个 Bean 不存在。控制台端点要能用（不抛
 * {@code NoSuchBeanDefinitionException}），故一律经 {@link #dynamicDataSource()} 取 Bean，
 * 缺失时返回空列表/提示，而不是 500。</p>
 */
@Slf4j
public class MagicMcpServerController extends MagicController implements MagicExceptionHandler {

	public static final String TRANSPORT_SSE = "http-sse";

	public static final String TRANSPORT_STREAMABLE_HTTP = "streamable-http";

	public MagicMcpServerController(MagicConfiguration configuration) {
		super(configuration);
	}

	/**
	 * 获取 MCP server 动态数据源；未装配（Bean 缺失）时返回 {@code null}。
	 */
	private MagicDynamicMcpServer dynamicDataSource() {
		Map<String, MagicDynamicMcpServer> beans = SpringUtils.getBeansOfType(MagicDynamicMcpServer.class);
		if (beans.isEmpty()) {
			log.warn("MagicDynamicMcpServer bean 未装配，MCP server 控制台能力不可用（可忽略：未启用 MCP server runtime）");
			return null;
		}
		return beans.values().iterator().next();
	}

	/**
	 * 获取 MCP server 动态注册表；未装配（Bean 缺失）时返回 {@code null}。
	 */
	private MagicMcpServerMagicDynamicRegistry dynamicRegistry() {
		Map<String, MagicMcpServerMagicDynamicRegistry> beans =
				SpringUtils.getBeansOfType(MagicMcpServerMagicDynamicRegistry.class);
		if (beans.isEmpty()) {
			log.warn("MagicMcpServerMagicDynamicRegistry bean 未装配，MCP server 控制台能力不可用（可忽略：未启用 MCP server runtime）");
			return null;
		}
		return beans.values().iterator().next();
	}

	/**
	 * 全部运行中的 MCP server 实例；runtime 未装配时返回空列表。
	 */
	@RequestMapping("/mcp/server/list")
	@ResponseBody
	public JsonBean<List<Map<String, Object>>> list() {
		MagicDynamicMcpServer dataSource = dynamicDataSource();
		if (dataSource == null) {
			return new JsonBean<>(1, "MCP server runtime 未装配，返回空实例列表", Collections.emptyList());
		}
		return new JsonBean<>(dataSource.serverSummaries());
	}

	/**
	 * 保存前校验：key、endpoint、transport 合法。
	 */
	@RequestMapping("/mcp/server/test")
	@ResponseBody
	public JsonBean<Object> test(@RequestBody MagicMcpServerInfo info) {
		MagicMcpServerInfoMagicResourceStorage storage =
				SpringUtils.getBean(MagicMcpServerInfoMagicResourceStorage.class);
		try {
			storage.validate(info);
		} catch (IllegalArgumentException e) {
			return new JsonBean<>(e.getMessage());
		}
		return new JsonBean<>("ok");
	}

	/**
	 * 数据源连接测试：按控制台表单临时构建 MCP server 实例探测配置可用性
	 * （不落库、不注册、不影响运行中实例）。与「保存前校验」{@code /mcp/server/test}
	 * （仅静态字段校验）不同，本接口会真实装配 runtime 并立即关闭 transport 释放。
	 */
	@RequestMapping("/mcp/datasource/test")
	@ResponseBody
	public JsonBean<Object> datasourceTest(@RequestBody MagicMcpServerInfo info) {
		MagicMcpServerInfoMagicResourceStorage storage =
				SpringUtils.getBean(MagicMcpServerInfoMagicResourceStorage.class);
		try {
			storage.validate(info);
		} catch (IllegalArgumentException e) {
			return new JsonBean<>(e.getMessage());
		}
		MagicDynamicMcpServer dataSource = dynamicDataSource();
		if (dataSource == null) {
			return new JsonBean<>("MCP server runtime 未装配，无法测试连接（未启用 MCP server runtime）");
		}
		return new JsonBean<>(dataSource.testConnection(info));
	}

	/**
	 * 删除前校验：实例已停用时才允许删除。
	 */
	@RequestMapping("/mcp/server/doDelete")
	@ResponseBody
	public JsonBean<Object> doDelete(@RequestBody MagicMcpServerInfo info) {
		if (info.isEnabled()) {
			return new JsonBean<>("该 MCP server「" + info.getKey() + "」已启用，请先停用后再删除");
		}
		return new JsonBean<>("ok");
	}

	/**
	 * 启停 MCP server 实例：停用时销毁实例，启用时按当前资源定义重建。
	 */
	@RequestMapping("/mcp/server/changeConnected")
	@ResponseBody
	public JsonBean<Object> changeConnected(@RequestBody MagicMcpServerInfo info) {
		MagicDynamicMcpServer dataSource = dynamicDataSource();
		if (dataSource == null) {
			return new JsonBean<>("MCP server runtime 未装配，无法启停实例（未启用 MCP server runtime）");
		}
		if (!info.isEnabled()) {
			dataSource.destroyClientByKey(info.getKey());
			return new JsonBean<>("ok");
		}
		if (!dataSource.createClient(info)) {
			return new JsonBean<>("MCP server「" + info.getKey() + "」启动失败，请检查配置");
		}
		return new JsonBean<>("ok");
	}

	/**
	 * 刷新全部实例的工具快照。
	 */
	@RequestMapping("/mcp/server/refresh")
	@ResponseBody
	public JsonBean<Object> refresh() {
		MagicDynamicMcpServer dataSource = dynamicDataSource();
		if (dataSource == null) {
			return new JsonBean<>("MCP server runtime 未装配，无法刷新工具（未启用 MCP server runtime）");
		}
		dataSource.refreshTools();
		return new JsonBean<>("ok");
	}

	/**
	 * 全部已定义（含未启用）的 MCP server 实例。
	 */
	@RequestMapping("/mcp/server/defined")
	@ResponseBody
	public JsonBean<Object> defined() {
		MagicMcpServerMagicDynamicRegistry registry = dynamicRegistry();
		if (registry == null) {
			return new JsonBean<>(1, "MCP server runtime 未装配，返回空定义列表", Collections.emptyList());
		}
		return new JsonBean<>(registry.mappings());
	}

	/**
	 * 默认实例定义：来自插件配置项。
	 */
	@RequestMapping("/mcp/server/defaults")
	@ResponseBody
	public JsonBean<Object> defaults() {
		MagicApiMcpProperties properties = SpringUtils.getBean(MagicApiMcpProperties.class);
		return new JsonBean<>(ListUtils.emptyIfNull(properties.getServers()));
	}

	/**
	 * 当前 classpath 可用的认证 provider 清单：供控制台表单渲染可选项
	 * （与 Starter 的 InstanceAuthRegistry.availableProviders 同一份清单，缺失模块不可选）。
	 */
	@RequestMapping("/mcp/server/authProviders")
	@ResponseBody
	public JsonBean<Object> authProviders() {
		return new JsonBean<>(InstanceAuthRegistry.availableProviders(
				InstanceAuthRegistry.class.getClassLoader()));
	}
}