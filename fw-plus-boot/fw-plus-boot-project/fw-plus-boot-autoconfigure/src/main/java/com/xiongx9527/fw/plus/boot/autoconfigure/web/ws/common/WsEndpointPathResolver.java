package com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.common;

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
		if (additionalPaths != null) {
			for (String additional : additionalPaths) {
				String normalized = normalize(additional);
				if (normalized != null) {
					unique.add(normalized);
				}
			}
		}
		return new ArrayList<>(unique);
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
