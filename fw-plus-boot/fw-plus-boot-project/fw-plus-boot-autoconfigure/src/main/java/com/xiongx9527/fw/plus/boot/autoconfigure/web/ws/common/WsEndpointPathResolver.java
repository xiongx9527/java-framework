package com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.common;

import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.config.WsConfig;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.config.WsConfig.WsInstance;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * 端点路径解析：把「主路径 + 追加路径」整理成相对路径列表，再按基础路径拼成完整路径。
 *
 * <p>webmvc 与 webflux 两条栈共用同一套拼接规则，避免同一个配置在两端产生不同路径。
 * 解析结果与改造前单路径时的语义完全一致：主路径留空时不再补前导斜杠，直接按基础路径处理。</p>
 *
 * @author zzy
 */
public final class WsEndpointPathResolver {

	private WsEndpointPathResolver() {
	}

	/**
	 * 规范化单个路径：去掉首尾空白并补上缺失的前导斜杠；空白项返回 {@code null} 表示丢弃。
	 *
	 * @param path 原始路径
	 * @return 规范化后的路径，空白时返回 {@code null}
	 */
	public static String normalize(String path) {
		String trimmed = path == null ? null : path.trim();
		if (StringUtils.isBlank(trimmed)) {
			return null;
		}
		return trimmed.startsWith("/") ? trimmed : "/" + trimmed;
	}

	/**
	 * 解析端点相对路径列表。
	 *
	 * @param primaryPath     主路径（{@code fw.plus.ws.websocket.path}），允许为空串以保留既有语义
	 * @param additionalPaths 追加路径（{@code fw.plus.ws.websocket.paths}），可为空
	 * @return 按配置顺序去重后的相对路径列表，不含基础路径
	 */
	public static List<String> resolveRelativePaths(String primaryPath, List<String> additionalPaths) {
		LinkedHashSet<String> unique = new LinkedHashSet<>();
		String primary = primaryPath == null ? "" : primaryPath.trim();
		if (primary.isEmpty()) {
			unique.add("");
		} else {
			unique.add(primary.startsWith("/") ? primary : "/" + primary);
		}
		appendAdditionalPaths(additionalPaths, unique);
		return new ArrayList<>(unique);
	}

	/**
	 * 解析全部实例的端点相对路径：默认实例始终存在，命名实例在其之上做纯增量。
	 *
	 * <p>默认实例取 {@code websocket.path} 与 {@code websocket.paths}；命名实例取各自的
	 * {@code path} 与 {@code paths}，与默认实例重复的路径只注册一次。因此不配
	 * {@code fw.plus.ws.servers} 时结果与改造前完全一致。</p>
	 *
	 * @param config WS 地基配置，可为 {@code null}
	 * @return 按「默认实例、命名实例」顺序去重后的相对路径列表，不含基础路径
	 */
	public static List<String> resolveRelativePaths(WsConfig config) {
		if (config == null) {
			return resolveRelativePaths(null, null);
		}
		LinkedHashSet<String> unique = new LinkedHashSet<>();
		unique.addAll(resolveRelativePaths(config.getWebsocket().getPath(), config.getWebsocket().getPaths()));
		List<WsInstance> servers = config.getServers();
		if (servers != null) {
			for (WsInstance server : servers) {
				if (server == null) {
					continue;
				}
				String primary = normalize(server.getPath());
				if (primary != null) {
					unique.add(primary);
				}
				appendAdditionalPaths(server.getPaths(), unique);
			}
		}
		return new ArrayList<>(unique);
	}

	/**
	 * 把追加路径逐个规范化后并入集合，空白项丢弃。
	 */
	private static void appendAdditionalPaths(List<String> additionalPaths, LinkedHashSet<String> unique) {
		if (additionalPaths == null) {
			return;
		}
		for (String additional : additionalPaths) {
			String normalized = normalize(additional);
			if (normalized != null) {
				unique.add(normalized);
			}
		}
	}

	/**
	 * 拼接基础路径与相对路径，处理连接处斜杠重复或缺失。
	 *
	 * @param basePath      基础路径，可为空
	 * @param relativePath  相对路径
	 * @return 完整端点路径
	 */
	public static String joinBasePath(String basePath, String relativePath) {
		String base = basePath == null ? "" : basePath;
		if (base.isEmpty()) {
			return relativePath;
		}
		boolean baseEndsWithSlash = base.endsWith("/");
		boolean pathStartsWithSlash = relativePath.startsWith("/");
		if (!baseEndsWithSlash && !pathStartsWithSlash) {
			return base + "/" + relativePath;
		}
		if (baseEndsWithSlash && pathStartsWithSlash) {
			return base.substring(0, base.length() - 1) + relativePath;
		}
		return base + relativePath;
	}

	/**
	 * 把同一套相对路径展开到多个基础路径上，得到最终注册的完整路径集合。
	 *
	 * @param relativePaths 相对路径列表，通常由 {@link #resolveRelativePaths(String, List)} 产出
	 * @param basePaths     候选基础路径，为空时只按相对路径注册
	 * @return 按「基础路径 × 相对路径」顺序去重后的完整路径列表
	 */
	public static List<String> resolveFullPaths(List<String> relativePaths, Collection<String> basePaths) {
		LinkedHashSet<String> fullPaths = new LinkedHashSet<>();
		if (basePaths == null || basePaths.isEmpty()) {
			fullPaths.addAll(relativePaths);
			return new ArrayList<>(fullPaths);
		}
		for (String basePath : basePaths) {
			for (String relativePath : relativePaths) {
				fullPaths.add(joinBasePath(normalizeBasePath(basePath), relativePath));
			}
		}
		return new ArrayList<>(fullPaths);
	}

	/**
	 * 规范化基础路径：空白值按空串处理，表示不使用基础路径。
	 *
	 * @param basePath 原始基础路径
	 * @return 规范化后的基础路径，不为 {@code null}
	 */
	public static String normalizeBasePath(String basePath) {
		return basePath == null ? "" : basePath.trim();
	}
}
