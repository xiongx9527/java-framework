package com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.common;

import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.config.WsConfig;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.config.WsConfig.WsInstance;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 按端点请求路径解析 WebSocket 实例，实现「一个实例 = 一个 key + 一组端点 + 一个 appId」。
 *
 * <p>匹配语义与 {@link WsEndpointPathResolver} 一致地保持纯静态、无 Spring 依赖，webmvc 与
 * webflux 两条栈共用同一套规则：</p>
 * <ul>
 *   <li>完整路径（含基础路径，如 {@code /magic-api/web/ws/api2}）与实例任一端点（主路径或追加路径）精确相等时命中；</li>
 *   <li>否则按后缀边界匹配：请求路径以实例任一端点结尾时命中；</li>
 *   <li>未命中任何实例（或实例未填 appId）时返回 {@code null}，由调用方回落到默认实例的
 *       {@code fw.plus.ws.appId}。</li>
 * </ul>
 *
 * <p>精确匹配优先于后缀匹配，因此实例写完整路径时不会被相对路径实例抢先。</p>
 *
 * @author zzy
 */
public final class WsInstanceResolver {

	private WsInstanceResolver() {
	}

	/**
	 * 解析请求路径命中的实例。
	 *
	 * @param config       WS 地基配置，可为 {@code null}
	 * @param requestPath  握手请求路径（{@code URI#getPath()}），可为 {@code null}
	 * @return 命中的实例；未命中时返回 {@code null}
	 */
	public static WsInstance resolve(WsConfig config, String requestPath) {
		if (config == null || config.getServers() == null || StringUtils.isBlank(requestPath)) {
			return null;
		}
		String path = normalizeRequestPath(requestPath);
		if (path == null) {
			return null;
		}
		List<WsInstance> servers = config.getServers();
		for (WsInstance server : servers) {
			if (matchesExactly(server, path)) {
				return server;
			}
		}
		for (WsInstance server : servers) {
			if (matchesBySuffix(server, path)) {
				return server;
			}
		}
		return null;
	}

	/**
	 * 精确匹配：请求路径与实例任一端点（主路径或追加路径）相等时命中。
	 */
	private static boolean matchesExactly(WsInstance instance, String path) {
		for (String endpoint : instancePaths(instance)) {
			if (endpoint.equals(path)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 后缀边界匹配：请求路径以实例任一端点结尾且长度严格更长时命中。
	 */
	private static boolean matchesBySuffix(WsInstance instance, String path) {
		for (String endpoint : instancePaths(instance)) {
			if (isSuffixMatch(path, endpoint)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 实例的全部端点路径：主路径与追加路径逐个规范化，空白项已剔除。
	 */
	private static List<String> instancePaths(WsInstance instance) {
		List<String> paths = new ArrayList<>();
		String primary = normalizeInstancePath(instance.getPath());
		if (primary != null) {
			paths.add(primary);
		}
		if (instance.getPaths() != null) {
			for (String additional : instance.getPaths()) {
				String normalized = normalizeInstancePath(additional);
				if (normalized != null) {
					paths.add(normalized);
				}
			}
		}
		return paths;
	}

	/**
	 * 解析请求路径命中的实例 appId。
	 *
	 * @param config       WS 地基配置，可为 {@code null}
	 * @param requestPath  握手请求路径，可为 {@code null}
	 * @return 命中且填写了 appId 的实例 appId；否则返回 {@code null} 表示回落到默认 appId
	 */
	public static String resolveAppId(WsConfig config, String requestPath) {
		WsInstance instance = resolve(config, requestPath);
		if (instance == null || StringUtils.isBlank(instance.getAppId())) {
			return null;
		}
		return instance.getAppId();
	}

	/**
	 * 后缀边界匹配：请求路径以实例端点结尾，且长度严格更长。
	 *
	 * <p>实例端点规范化后已带前导斜杠，{@code /my-api} 的尾段是 {@code -api}，天然不会命中 {@code /api}。</p>
	 */
	private static boolean isSuffixMatch(String path, String instancePath) {
		return path.length() > instancePath.length() && path.endsWith(instancePath);
	}

	/**
	 * 规范化请求路径：去掉 query、补前导斜杠、去尾斜杠（根路径除外），让
	 * {@code /ws/api} 与 {@code /ws/api/} 命中同一个实例。
	 */
	private static String normalizeRequestPath(String requestPath) {
		String path = requestPath.trim();
		int queryIndex = path.indexOf('?');
		if (queryIndex >= 0) {
			path = path.substring(0, queryIndex);
		}
		return normalize(path);
	}

	/**
	 * 规范化实例端点路径：补前导斜杠、去尾斜杠（根路径除外）；空白返回 {@code null} 表示该实例不参与匹配。
	 */
	private static String normalizeInstancePath(String instancePath) {
		String trimmed = instancePath == null ? null : instancePath.trim();
		if (StringUtils.isBlank(trimmed)) {
			return null;
		}
		return normalize(trimmed);
	}

	private static String normalize(String path) {
		String normalized = path.startsWith("/") ? path : "/" + path;
		while (normalized.length() > 1 && normalized.endsWith("/")) {
			normalized = normalized.substring(0, normalized.length() - 1);
		}
		return normalized;
	}
}
