package com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.common;

import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.config.WsConfig;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.config.WsConfig.WsInstance;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * 命名实例配置校验，在自动配置装配路径上执行，不依赖宿主是否开启 {@code @EnableWebSocket}。
 *
 * <p>校验口径与 {@code fw-plus.ai.mcp.servers[]}（{@code McpRuntimeFactory#validateKeys} /
 * {@code validateEndpoints}）保持一致，出现以下情形均抛 {@link IllegalArgumentException} 使应用启动失败：</p>
 * <ul>
 *   <li>实例 {@code key} 缺失、空串或纯空白；</li>
 *   <li>实例 {@code key} 在 {@code servers} 内 trim 后重复；</li>
 *   <li>实例端点（{@code path} 或 {@code paths}，规范化后）与默认实例端点或另一实例端点重复；</li>
 *   <li>实例只配 {@code key}，未配任何端点。</li>
 * </ul>
 *
 * <p>端点重复会让后缀匹配中后声明的一方永远无法被握手命中，只配 key 的实例同样永远不会生效，
 * 二者都是「配了却不生效」的静默误导，因此按配置错误处理而不是放行。</p>
 *
 * <p>同一实例内部 {@code path} 与 {@code paths} 的重复按配置顺序去重，不算冲突。
 * appId 留空由 {@link WsInstanceResolver} 回落顶层 {@code fw.plus.ws.appId}，不在此处校验。</p>
 *
 * @author zzy
 */
public final class WsInstanceValidator {

	/**
	 * 默认实例在冲突信息中的标识：默认实例没有 key，由 {@code fw.plus.ws.websocket.*} 描述。
	 */
	private static final String DEFAULT_INSTANCE = "default";

	public WsInstanceValidator() {
	}

	/**
	 * 校验命名实例配置。
	 *
	 * @param config WS 地基配置，可为 {@code null}；未配置 {@code servers} 时直接通过
	 * @throws IllegalArgumentException 出现 key 或端点配置错误时
	 */
	public static void validate(WsConfig config) {
		if (config == null || config.getServers() == null) {
			return;
		}
		Map<String, String> endpoints = new LinkedHashMap<>();
		for (String endpoint : WsEndpointPathResolver.resolveRelativePaths(
				config.getWebsocket().getPath(), config.getWebsocket().getPaths())) {
			String normalized = normalizeEndpoint(endpoint);
			if (normalized != null) {
				endpoints.put(normalized, DEFAULT_INSTANCE);
			}
		}
		Map<String, Integer> keys = new LinkedHashMap<>();
		List<WsInstance> servers = config.getServers();
		for (int i = 0; i < servers.size(); i++) {
			WsInstance server = servers.get(i);
			String prefix = "fw.plus.ws.servers[" + i + "]";
			if (server == null) {
				throw new IllegalArgumentException(prefix + " must not be null");
			}
			validateKey(server, prefix, i, keys);
			String key = server.getKey().trim();
			List<String> instanceEndpoints = resolveInstanceEndpoints(server);
			if (instanceEndpoints.isEmpty()) {
				throw new IllegalArgumentException("ws server key '" + key + "' (" + prefix
						+ ") declares no endpoint; configure " + prefix + ".path or " + prefix + ".paths");
			}
			validateEndpoints(instanceEndpoints, key, prefix, endpoints);
		}
	}

	/**
	 * 校验单个实例的 key：必填非空，且 trim 后在 {@code servers} 内唯一。
	 */
	private static void validateKey(WsInstance server, String prefix, int index, Map<String, Integer> keys) {
		if (StringUtils.isBlank(server.getKey())) {
			throw new IllegalArgumentException(prefix + ".key must not be blank");
		}
		String key = server.getKey().trim();
		Integer previousIndex = keys.put(key, index);
		if (previousIndex != null) {
			throw new IllegalArgumentException("fw.plus.ws.servers[].key '" + key
					+ "' is declared more than once: " + prefix + ".key conflicts with fw.plus.ws.servers["
					+ previousIndex + "].key");
		}
	}

	/**
	 * 校验单个实例的端点：与默认实例或已校验实例重复时启动失败。
	 */
	private static void validateEndpoints(List<String> instanceEndpoints, String key, String prefix,
			Map<String, String> endpoints) {
		for (String endpoint : instanceEndpoints) {
			String owner = endpoints.put(endpoint, key);
			if (owner != null) {
				throw new IllegalArgumentException("ws endpoint conflict: server key '" + key + "' (" + prefix
						+ ") declares endpoint '" + endpoint + "', which is already declared by "
						+ describeOwner(owner));
			}
		}
	}

	/**
	 * 冲突方描述：默认实例没有 key，用配置键路径描述。
	 */
	private static String describeOwner(String owner) {
		if (DEFAULT_INSTANCE.equals(owner)) {
			return "the default instance (fw.plus.ws.websocket.path / fw.plus.ws.websocket.paths)";
		}
		return "server key '" + owner + "'";
	}

	/**
	 * 实例的全部端点：{@code path} 与 {@code paths} 逐个规范化后按配置顺序去重，空白项丢弃。
	 */
	private static List<String> resolveInstanceEndpoints(WsInstance server) {
		LinkedHashSet<String> unique = new LinkedHashSet<>();
		String primary = normalizeEndpoint(server.getPath());
		if (primary != null) {
			unique.add(primary);
		}
		if (server.getPaths() != null) {
			for (String path : server.getPaths()) {
				String normalized = normalizeEndpoint(path);
				if (normalized != null) {
					unique.add(normalized);
				}
			}
		}
		return new ArrayList<>(unique);
	}

	/**
	 * 规范化端点：补前导斜杠、去尾斜杠（根路径除外），与握手匹配的规范化口径一致。
	 *
	 * @param path 原始端点路径
	 * @return 规范化后的端点，空白时返回 {@code null} 表示丢弃
	 */
	private static String normalizeEndpoint(String path) {
		String normalized = WsEndpointPathResolver.normalize(path);
		if (normalized == null) {
			return null;
		}
		while (normalized.length() > 1 && normalized.endsWith("/")) {
			normalized = normalized.substring(0, normalized.length() - 1);
		}
		return normalized;
	}
}
