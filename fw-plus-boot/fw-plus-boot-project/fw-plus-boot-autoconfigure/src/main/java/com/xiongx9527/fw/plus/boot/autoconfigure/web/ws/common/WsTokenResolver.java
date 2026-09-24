package com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.common;

import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.config.WsConfig;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.config.WsConfig.TokenRule;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * 按端点请求路径解析 token 规则，实现「一个实例对应一条路径 + 一套 token」。
 *
 * <p>匹配语义与 {@link WsEndpointPathResolver} 一致地保持纯静态、无 Spring 依赖，webmvc 与
 * webflux 两条栈以及 magic 侧签发路径共用同一套规则：</p>
 * <ul>
 *   <li>完整路径（含基础路径，如 {@code /magic-api/web/ws/api2}）与规则路径精确相等时命中；</li>
 *   <li>否则按后缀边界匹配：请求路径以 {@code '/' + 规则路径} 结尾时命中，</li>
 *   <li>未命中任何规则（或规则密钥留空）时由调用方回落到全局 {@code fw.plus.ws.token}。</li>
 * </ul>
 *
 * <p>精确匹配优先于后缀匹配，因此规则写完整路径时不会被相对路径规则抢先。</p>
 *
 * @author zzy
 */
public final class WsTokenResolver {

	private WsTokenResolver() {
	}

	/**
	 * 解析请求路径命中的 token 规则。
	 *
	 * @param config       WS 地基配置，可为 {@code null}
	 * @param requestPath  握手请求路径（{@code URI#getPath()}），可为 {@code null}
	 * @return 命中的规则；未命中时返回 {@code null}
	 */
	public static TokenRule resolve(WsConfig config, String requestPath) {
		if (config == null || config.getTokens() == null || StringUtils.isBlank(requestPath)) {
			return null;
		}
		String path = normalizeRequestPath(requestPath);
		if (path == null) {
			return null;
		}
		List<TokenRule> rules = config.getTokens();
		for (TokenRule rule : rules) {
			String rulePath = normalizeRulePath(rule.getPath());
			if (rulePath != null && rulePath.equals(path)) {
				return rule;
			}
		}
		for (TokenRule rule : rules) {
			String rulePath = normalizeRulePath(rule.getPath());
			if (rulePath != null && isSuffixMatch(path, rulePath)) {
				return rule;
			}
		}
		return null;
	}

	/**
	 * 解析请求路径绑定的 token 密钥。
	 *
	 * @param config       WS 地基配置，可为 {@code null}
	 * @param requestPath  握手请求路径，可为 {@code null}
	 * @return 命中且密钥非空的规则密钥；否则返回 {@code null} 表示回落到全局密钥
	 */
	public static String resolveSecret(WsConfig config, String requestPath) {
		TokenRule rule = resolve(config, requestPath);
		if (rule == null || StringUtils.isBlank(rule.getSecret())) {
			return null;
		}
		return rule.getSecret();
	}

	/**
	 * 解析请求路径绑定的 token 有效期。
	 *
	 * @param config       WS 地基配置，可为 {@code null}
	 * @param requestPath  握手请求路径，可为 {@code null}
	 * @return 命中且显式配置了的有效期；否则返回 {@code null} 表示继承全局配置
	 */
	public static Long resolveExpireHours(WsConfig config, String requestPath) {
		TokenRule rule = resolve(config, requestPath);
		return rule == null ? null : rule.getExpireHours();
	}

	/**
	 * 后缀边界匹配：请求路径以规则路径结尾，且长度严格更长。
	 *
	 * <p>规则路径规范化后已带前导斜杠，{@code /my-api} 的尾段是 {@code -api}，天然不会命中 {@code /api}。</p>
	 */
	private static boolean isSuffixMatch(String path, String rulePath) {
		return path.length() > rulePath.length() && path.endsWith(rulePath);
	}

	/**
	 * 规范化请求路径：去掉 query、补前导斜杠、去尾斜杠（根路径除外），让
	 * {@code /ws/api} 与 {@code /ws/api/} 命中同一条规则。
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
	 * 规范化规则路径：补前导斜杠、去尾斜杠（根路径除外）；空白返回 {@code null} 表示该规则不参与匹配。
	 */
	private static String normalizeRulePath(String rulePath) {
		String trimmed = rulePath == null ? null : rulePath.trim();
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
